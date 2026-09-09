package com.example.data.remote

import android.util.Log
import com.example.data.model.JobMessage
import com.example.data.model.ProviderLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "SupabaseRealtime"

data class PostgresAction<T>(
    val record: T,
    val eventType: String = "UPDATE"
)

class PostgresChangeFilter {
    var table: String = "provider_locations"
    var filter: String = ""
}

class RealtimeChannel(
    val topic: String,
    private val client: OkHttpClient,
    private val supabaseUrl: String,
    private val anonKey: String,
    private val getLatestLocationFallback: suspend (String) -> ProviderLocation?,
    private val getLatestMessagesFallback: (suspend (String) -> List<JobMessage>)? = null
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private var pollingJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private var activeFilter = PostgresChangeFilter()
    private val _locationFlow = MutableSharedFlow<PostgresAction<ProviderLocation>>(replay = 1)
    private val _messageFlow = MutableSharedFlow<PostgresAction<JobMessage>>(replay = 1)
    private var lastEmittedLocation: ProviderLocation? = null
    private val emittedMessageIds = mutableSetOf<String>()

    @Suppress("UNCHECKED_CAST")
    fun <T> postgresChangeFlow(
        schema: String = "public",
        configure: PostgresChangeFilter.() -> Unit
    ): Flow<PostgresAction<T>> {
        activeFilter.configure()
        return if (activeFilter.table == "job_messages") {
            _messageFlow.asSharedFlow() as Flow<PostgresAction<T>>
        } else {
            _locationFlow.asSharedFlow() as Flow<PostgresAction<T>>
        }
    }

    fun subscribe() {
        val wsUrl = supabaseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Realtime WebSocket opened for topic: $topic")
                joinChannel(webSocket)
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime WebSocket failure: ${t.message}. Reconnecting or relying on fallback...")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime WebSocket closed: $reason ($code)")
            }
        })

        // Extract jobId from filter if present (e.g. "job_id=eq.123")
        val filterVal = activeFilter.filter
        val jobId = if (filterVal.contains("job_id=eq.")) {
            filterVal.substringAfter("job_id=eq.").trim()
        } else ""

        // Companion polling fallback for high reliability under all network/emulator environments
        if (jobId.isNotBlank()) {
            startPollingFallback(jobId)
        }
    }

    private fun joinChannel(ws: WebSocket) {
        try {
            val joinPayload = JSONObject().apply {
                put("topic", "realtime:public:${activeFilter.table}:${activeFilter.filter}")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    put("config", JSONObject().apply {
                        val changes = org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", activeFilter.table)
                                if (activeFilter.filter.isNotBlank()) {
                                    put("filter", activeFilter.filter)
                                }
                            })
                        }
                        put("postgres_changes", changes)
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinPayload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error joining Realtime channel", e)
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25000L)
                try {
                    val hb = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", refCounter.getAndIncrement().toString())
                    }
                    ws.send(hb.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat failed", e)
                }
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val msg = JSONObject(text)
            val event = msg.optString("event", "")
            if (event == "postgres_changes") {
                val payload = msg.optJSONObject("payload") ?: return
                val data = payload.optJSONObject("data") ?: return
                val recordObj = data.optJSONObject("record") ?: return
                val type = data.optString("type", "UPDATE")

                if (activeFilter.table == "job_messages") {
                    val message = JobMessage.fromJson(recordObj)
                    emitMessage(message, type)
                } else {
                    val location = ProviderLocation.fromJson(recordObj)
                    emitLocation(location, type)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing incoming realtime message: ${e.message}")
        }
    }

    private fun startPollingFallback(jobId: String) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            if (activeFilter.table == "job_messages") {
                fetchAndEmitMessages(jobId)
                while (isActive) {
                    delay(3000L) // Poll every 3 seconds for messages
                    fetchAndEmitMessages(jobId)
                }
            } else {
                fetchAndEmitLocation(jobId)
                while (isActive) {
                    delay(4000L) // Poll every 4 seconds for provider locations
                    fetchAndEmitLocation(jobId)
                }
            }
        }
    }

    private suspend fun fetchAndEmitLocation(jobId: String) {
        try {
            val location = getLatestLocationFallback(jobId)
            if (location != null) {
                if (lastEmittedLocation == null ||
                    lastEmittedLocation?.lat != location.lat ||
                    lastEmittedLocation?.lng != location.lng ||
                    lastEmittedLocation?.heading != location.heading
                ) {
                    emitLocation(location, "UPDATE")
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun fetchAndEmitMessages(jobId: String) {
        try {
            val messages = getLatestMessagesFallback?.invoke(jobId) ?: emptyList()
            for (msg in messages) {
                if (!emittedMessageIds.contains(msg.id) || msg.readAt != null) {
                    emitMessage(msg, "INSERT")
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun emitLocation(location: ProviderLocation, type: String) {
        lastEmittedLocation = location
        scope.launch {
            _locationFlow.emit(PostgresAction(record = location, eventType = type))
        }
    }

    private fun emitMessage(message: JobMessage, type: String) {
        emittedMessageIds.add(message.id)
        scope.launch {
            _messageFlow.emit(PostgresAction(record = message, eventType = type))
        }
    }

    fun unsubscribe() {
        heartbeatJob?.cancel()
        pollingJob?.cancel()
        try {
            val leavePayload = JSONObject().apply {
                put("topic", "realtime:public:${activeFilter.table}:${activeFilter.filter}")
                put("event", "phx_leave")
                put("payload", JSONObject())
                put("ref", refCounter.getAndIncrement().toString())
            }
            webSocket?.send(leavePayload.toString())
            webSocket?.close(1000, "Unsubscribed")
        } catch (_: Exception) {
        }
        webSocket = null
    }
}

class RealtimeClient(
    private val client: OkHttpClient,
    private val supabaseUrl: String,
    private val anonKey: String,
    private val getLatestLocationFallback: suspend (String) -> ProviderLocation?,
    private val getLatestMessagesFallback: (suspend (String) -> List<JobMessage>)? = null
) {
    fun channel(topic: String): RealtimeChannel {
        return RealtimeChannel(
            topic = topic,
            client = client,
            supabaseUrl = supabaseUrl,
            anonKey = anonKey,
            getLatestLocationFallback = getLatestLocationFallback,
            getLatestMessagesFallback = getLatestMessagesFallback
        )
    }
}

