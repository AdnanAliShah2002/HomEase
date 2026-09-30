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
        const val AGORA_APP_CERTIFICATE = "bbd579b06ed04b1a9e07fd2cdd694996"

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
    private var connectionTimeoutJob: Job? = null
    private var ringingTimeoutJob: Job? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var currentCallLogId: String? = null
    private var callStartEpochMs: Long = 0L
    @Volatile
    private var isIncomingSession: Boolean = false
    @Volatile
    private var remoteUserAccepted: Boolean = false

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined Agora channel: $channel with uid: $uid, incoming=$isIncomingSession, remoteAccepted=$remoteUserAccepted")
            scope.launch {
                connectionTimeoutJob?.cancel()
                val current = _callState.value
                if (current is CallState.Connecting) {
                    val isConnected = isIncomingSession || remoteUserAccepted
                    _callState.value = CallState.Connected(
                        jobId = current.jobId,
                        channelName = current.channelName,
                        targetName = current.targetName,
                        targetRole = current.targetRole,
                        targetPhone = current.targetPhone,
                        durationSeconds = 0,
                        isMuted = false,
                        isSpeaker = true,
                        remoteUserConnected = isConnected
                    )
                    startDurationTimer()
                    if (!isConnected) {
                        startRingingTimeout()
                    }
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            scope.launch {
                connectionTimeoutJob?.cancel()
                ringingTimeoutJob?.cancel()
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

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Agora connectionStateChanged: state=$state, reason=$reason")
            if (state == Constants.CONNECTION_STATE_FAILED) {
                val errorMsg = when (reason) {
                    Constants.CONNECTION_CHANGED_INVALID_TOKEN, 8 ->
                        "Agora Error 110: Invalid Token. App Certificate is enabled on Agora project. Please switch to 'Testing Mode: APP ID' in Agora Console."
                    Constants.CONNECTION_CHANGED_TOKEN_EXPIRED, 9 ->
                        "Agora Error 109: Token expired."
                    Constants.CONNECTION_CHANGED_REJECTED_BY_SERVER, 10 ->
                        "Agora Error: Connection rejected by Agora server."
                    else ->
                        "Agora connection failed (reason $reason)"
                }
                scope.launch {
                    handleConnectionFailed(errorMsg)
                }
            }
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora RTC error: $err")
            if (err == 110 || err == 109 || err == Constants.ERR_INVALID_TOKEN || err == Constants.ERR_TOKEN_EXPIRED) {
                val errorMsg = "Agora Error $err: App Certificate active in Agora Console. Please switch project to 'Testing mode: APP ID'."
                scope.launch {
                    handleConnectionFailed(errorMsg)
                }
            } else if (err == 17 || err == Constants.ERR_JOIN_CHANNEL_REJECTED) {
                scope.launch {
                    handleConnectionFailed("Agora Error 17: Join channel was rejected by server.")
                }
            }
        }
    }

    private fun handleConnectionFailed(reason: String) {
        val current = _callState.value
        val jobId = when (current) {
            is CallState.Connecting -> current.jobId
            is CallState.Connected -> current.jobId
            is CallState.Ended -> current.jobId
            CallState.Idle -> ""
        }
        connectionTimeoutJob?.cancel()
        ringingTimeoutJob?.cancel()
        timerJob?.cancel()
        try {
            rtcEngine?.leaveChannel()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to leave channel: ${e.message}")
        }
        if (current !is CallState.Ended) {
            _callState.value = CallState.Ended(jobId, 0, reason)
        }
    }

    private fun startConnectionTimeout() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(25000L)
            val current = _callState.value
            if (current is CallState.Connecting) {
                Log.w(TAG, "Call connection timed out after 25s")
                handleConnectionFailed("Call timed out. No response from remote party or network error.")
            }
        }
    }

    private fun startRingingTimeout() {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = scope.launch {
            delay(45000L)
            val current = _callState.value
            if (current is CallState.Connected && !current.remoteUserConnected) {
                Log.w(TAG, "Ringing timed out after 45s without callee connecting")
                handleConnectionFailed("No answer. The call was not picked up.")
            }
        }
    }

    fun markRemoteUserConnected() {
        remoteUserAccepted = true
        scope.launch {
            connectionTimeoutJob?.cancel()
            ringingTimeoutJob?.cancel()
            val current = _callState.value
            if (current is CallState.Connected) {
                _callState.value = current.copy(remoteUserConnected = true)
            }
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
        isIncomingSession = false
        remoteUserAccepted = false
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

            val token = if (AGORA_APP_CERTIFICATE.isNotBlank()) {
                AgoraTokenBuilder.buildToken(
                    appId = AGORA_APP_ID,
                    appCertificate = AGORA_APP_CERTIFICATE,
                    channelName = channelName,
                    uid = 0
                )
            } else {
                null
            }
            Log.d(TAG, "Joining Agora channel $channelName with token: ${if (token != null) "TOKEN_GENERATED" else "NULL"}")
            val joinResult = rtcEngine?.joinChannel(token, channelName, 0, options) ?: -1
            if (joinResult < 0) {
                Log.e(TAG, "joinChannel failed with code: $joinResult")
                handleConnectionFailed("Failed to start voice call (Agora code $joinResult)")
                return
            }
            rtcEngine?.setEnableSpeakerphone(true)
            startConnectionTimeout()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join Agora channel", e)
            handleConnectionFailed("Failed to connect audio: ${e.message}")
        }
    }

    fun joinIncomingCall(
        jobId: String,
        channelName: String,
        targetName: String,
        targetRole: String,
        targetPhone: String,
        currentUserId: String,
        repository: HomeaseRepository? = null,
        supabaseClient: HomEaseSupabaseClient? = null
    ) {
        isIncomingSession = true
        remoteUserAccepted = true
        _callState.value = CallState.Connecting(
            jobId = jobId,
            channelName = channelName,
            targetName = targetName,
            targetRole = targetRole,
            targetPhone = targetPhone
        )

        callStartEpochMs = System.currentTimeMillis()

        try {
            ensureEngineInitialized()
            val options = ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                autoSubscribeAudio = true
                publishMicrophoneTrack = true
            }

            val token = if (AGORA_APP_CERTIFICATE.isNotBlank()) {
                AgoraTokenBuilder.buildToken(
                    appId = AGORA_APP_ID,
                    appCertificate = AGORA_APP_CERTIFICATE,
                    channelName = channelName,
                    uid = 0
                )
            } else {
                null
            }
            Log.d(TAG, "Joining Agora incoming channel $channelName with token: ${if (token != null) "TOKEN_GENERATED" else "NULL"}")
            val joinResult = rtcEngine?.joinChannel(token, channelName, 0, options) ?: -1
            if (joinResult < 0) {
                Log.e(TAG, "joinChannel for incoming call failed with code: $joinResult")
                handleConnectionFailed("Failed to answer voice call (Agora code $joinResult)")
                return
            }
            rtcEngine?.setEnableSpeakerphone(true)
            startConnectionTimeout()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join Agora channel for incoming call", e)
            handleConnectionFailed("Failed to connect audio: ${e.message}")
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
        supabaseClient: HomEaseSupabaseClient? = null,
        reason: String = "Call ended"
    ) {
        connectionTimeoutJob?.cancel()
        ringingTimeoutJob?.cancel()
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

        _callState.value = CallState.Ended(jobId, duration, reason)

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
        connectionTimeoutJob?.cancel()
        ringingTimeoutJob?.cancel()
        timerJob?.cancel()
        isIncomingSession = false
        remoteUserAccepted = false
        _callState.value = CallState.Idle
    }
}
