package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.remote.HomEaseSupabaseClient
import com.example.data.repository.HomeaseRepository
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "AgoraVoiceManager"

sealed class CallState {
    data object Idle : CallState()
    data class Connecting(
        val jobId: String,
        val channelName: String,
        val targetName: String,
        val targetRole: String,
        val targetPhone: String
    ) : CallState()

    data class Connected(
        val jobId: String,
        val channelName: String,
        val targetName: String,
        val targetRole: String,
        val targetPhone: String,
        val durationSeconds: Int = 0,
        val isMuted: Boolean = false,
        val isSpeaker: Boolean = true,
        val remoteUserConnected: Boolean = false
    ) : CallState()

    data class Ended(
        val jobId: String,
        val durationSeconds: Int,
        val reason: String = "Call ended"
    ) : CallState()
}

class AgoraVoiceManager private constructor(private val appContext: Context) {

    companion object {
        const val AGORA_APP_ID = "3014ea1db0a84f5ca24477ad98c0f495"

        @Volatile
        private var INSTANCE: AgoraVoiceManager? = null

        fun getInstance(context: Context): AgoraVoiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgoraVoiceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var rtcEngine: RtcEngine? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var currentCallLogId: String? = null
    private var callStartEpochMs: Long = 0L

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined Agora channel: $channel with uid: $uid")
            scope.launch {
                val current = _callState.value
                if (current is CallState.Connecting) {
                    _callState.value = CallState.Connected(
                        jobId = current.jobId,
                        channelName = current.channelName,
                        targetName = current.targetName,
                        targetRole = current.targetRole,
                        targetPhone = current.targetPhone,
                        durationSeconds = 0,
                        isMuted = false,
                        isSpeaker = true,
                        remoteUserConnected = false
                    )
                    startDurationTimer()
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            scope.launch {
                val current = _callState.value
                if (current is CallState.Connected) {
                    _callState.value = current.copy(remoteUserConnected = true)
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "Remote user offline: $uid, reason: $reason")
            scope.launch {
                val current = _callState.value
                if (current is CallState.Connected) {
                    _callState.value = current.copy(remoteUserConnected = false)
                }
            }
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora RTC error: $err")
        }
    }

    private fun ensureEngineInitialized() {
        if (rtcEngine == null) {
            try {
                val config = RtcEngineConfig().apply {
                    mContext = appContext
                    mAppId = AGORA_APP_ID
                    mEventHandler = rtcEventHandler
                    mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                }
                rtcEngine = RtcEngine.create(config).apply {
                    enableAudio()
                    setEnableSpeakerphone(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Agora RtcEngine", e)
            }
        }
    }

    fun startCall(
        jobId: String,
        targetName: String,
        targetRole: String,
        targetPhone: String,
        currentUserId: String,
        repository: HomeaseRepository? = null,
        supabaseClient: HomEaseSupabaseClient? = null
    ) {
        val cleanJobId = jobId.replace("-", "").take(16)
        val channelName = "job_$cleanJobId"

        _callState.value = CallState.Connecting(
            jobId = jobId,
            channelName = channelName,
            targetName = targetName,
            targetRole = targetRole,
            targetPhone = targetPhone
        )

        callStartEpochMs = System.currentTimeMillis()

        // Log call start to Supabase & Room
        scope.launch(Dispatchers.IO) {
            try {
                val callId = supabaseClient?.logCallStart(jobId, currentUserId)?.getOrNull()
                    ?: java.util.UUID.randomUUID().toString()
                currentCallLogId = callId

                repository?.insertCallLog(
                    com.example.data.db.CallLogEntity(
                        id = callId,
                        jobId = jobId,
                        callerId = currentUserId,
                        startedAtEpochMs = callStartEpochMs
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not log call start: ${e.message}")
            }
        }

        try {
            ensureEngineInitialized()
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                publishMicrophoneTrack = true
            }

            // In Agora test mode with App ID only, token is null or empty string
            rtcEngine?.joinChannel(null, channelName, 0, options)
            rtcEngine?.setEnableSpeakerphone(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join Agora channel", e)
            _callState.value = CallState.Ended(jobId, 0, "Failed to connect audio: ${e.message}")
        }
    }

    private fun startDurationTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (isActive) {
                delay(1000L)
                seconds++
                val current = _callState.value
                if (current is CallState.Connected) {
                    _callState.value = current.copy(durationSeconds = seconds)
                }
            }
        }
    }

    fun toggleMute() {
        val current = _callState.value
        if (current is CallState.Connected) {
            val newMuted = !current.isMuted
            rtcEngine?.muteLocalAudioStream(newMuted)
            _callState.value = current.copy(isMuted = newMuted)
        }
    }

    fun toggleSpeaker() {
        val current = _callState.value
        if (current is CallState.Connected) {
            val newSpeaker = !current.isSpeaker
            rtcEngine?.setEnableSpeakerphone(newSpeaker)
            _callState.value = current.copy(isSpeaker = newSpeaker)
        }
    }

    fun endCall(
        repository: HomeaseRepository? = null,
        supabaseClient: HomEaseSupabaseClient? = null
    ) {
        timerJob?.cancel()
        val current = _callState.value
        val duration = if (current is CallState.Connected) current.durationSeconds else 0
        val jobId = when (current) {
            is CallState.Connected -> current.jobId
            is CallState.Connecting -> current.jobId
            is CallState.Ended -> current.jobId
            CallState.Idle -> ""
        }

        try {
            rtcEngine?.leaveChannel()
        } catch (e: Exception) {
            Log.w(TAG, "Error leaving channel: ${e.message}")
        }

        _callState.value = CallState.Ended(jobId, duration)

        val logId = currentCallLogId
        if (logId != null && jobId.isNotBlank()) {
            val endedAt = System.currentTimeMillis()
            scope.launch(Dispatchers.IO) {
                try {
                    supabaseClient?.logCallEnd(logId, duration)
                    repository?.updateCallLog(logId, endedAt, duration)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not log call end: ${e.message}")
                }
            }
        }
        currentCallLogId = null
    }

    fun resetToIdle() {
        _callState.value = CallState.Idle
    }
}
