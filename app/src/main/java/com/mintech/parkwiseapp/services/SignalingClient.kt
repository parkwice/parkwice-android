package com.mintech.parkwiseapp.services

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.mintech.parkwiseapp.core.ApiConstants
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.webrtc.*

class SignalingClient(private val context: Context) {

    companion object {
        @Volatile private var instance: SignalingClient? = null
        fun getInstance(context: Context): SignalingClient =
                instance ?: synchronized(this) {
                    instance ?: SignalingClient(context).also { instance = it }
                }
    }

    val isCallActive = MutableStateFlow(false)
    val rtcState = MutableStateFlow("Connecting...")

    private var targetUserId: String? = null
    private var socket: Socket? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null

    private var pendingAcceptCallerId: String? = null
    
    // 🚨 NEW: For Delivery ACK
    private var pendingDeliveryAckCallerId: String? = null
    private var offlineTimeoutRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val remoteCandidatesQueue = mutableListOf<IceCandidate>()
    private var hasRemoteDescription = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        initWebRTC()
        initSocket()
    }

    private fun initWebRTC() {
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    private fun initSocket() {
        socket = IO.socket(ApiConstants.API_URL)

        socket?.on(Socket.EVENT_CONNECT) {
            AppLogger.logEvent("socket_connected")
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val myId = prefs.getString("user_id", "") ?: ""
            if (myId.isNotEmpty()) {
                socket?.emit("register", myId)
                
                // 🚨 NEW: If we woke up from a push, tell the server we got it!
                pendingDeliveryAckCallerId?.let { callerId ->
                    socket?.emit("call-delivered", JSONObject().put("callerId", callerId))
                    pendingDeliveryAckCallerId = null
                }
                
                pendingAcceptCallerId?.let { callerId ->
                    mainHandler.postDelayed({
                        val payload = JSONObject().apply {
                            put("targetUserId", callerId)
                            put("responderId", myId)
                        }
                        socket?.emit("accept-call", payload)
                        pendingAcceptCallerId = null 
                    }, 400) 
                }
            }
        }

        // 🚨 NEW: Caller receives confirmation that receiver's phone is actually ringing
        socket?.on("call-ringing") {
            mainHandler.post {
                offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) } // They are online! Stop the drop timer.
                rtcState.value = "Ringing..."
            }
        }

        socket?.on("call-accepted") { args ->
            val data = args[0] as JSONObject
            val responderId = data.getString("responderId")
            mainHandler.post {
                offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                rtcState.value = "Connecting..." // Changed to match iOS
            }
            startWebRTCCall(responderId)
        }

        socket?.on("offer") { args ->
            val data = args[0] as JSONObject
            val callerId = data.getString("callerId")
            val sdp = data.getJSONObject("sdp").getString("sdp")
            handleOffer(callerId, sdp)
        }

        socket?.on("answer") { args ->
            val data = args[0] as JSONObject
            val sdp = data.getJSONObject("sdp").getString("sdp")
            peerConnection?.setRemoteDescription(
                    object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            hasRemoteDescription = true
                            drainRemoteCandidates()
                        }
                    },
                    SessionDescription(SessionDescription.Type.ANSWER, sdp)
            )
        }

        socket?.on("ice-candidate") { args ->
            val data = args[0] as JSONObject
            val cand = data.getJSONObject("candidate")
            val iceCandidate = IceCandidate(cand.getString("sdpMid"), cand.getInt("sdpMLineIndex"), cand.getString("candidate"))
            
            if (hasRemoteDescription) {
                peerConnection?.addIceCandidate(iceCandidate)
            } else {
                remoteCandidatesQueue.add(iceCandidate)
            }
        }

        socket?.on("call-ended") { cleanup() }
    }
    
    // 🚨 NEW: Called by FCM Service when a Push wakes the app up
    fun notifyCallDelivered(callerId: String) {
        if (socket?.connected() == true) {
            socket?.emit("call-delivered", JSONObject().put("callerId", callerId))
        } else {
            pendingDeliveryAckCallerId = callerId
            socket?.connect()
        }
    }

    fun initiateCall(targetId: String) {
        AppLogger.logEvent("webrtc_initiate_call")
        this.targetUserId = targetId
        isCallActive.value = true
        rtcState.value = "Calling..."
        socket?.connect()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        // 🚨 NEW: 15 Second Offline Timeout
        offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        offlineTimeoutRunnable = Runnable {
            if (rtcState.value == "Calling...") {
                rtcState.value = "User Unreachable"
                mainHandler.postDelayed({
                    endCall()
                }, 2000)
            }
        }
        mainHandler.postDelayed(offlineTimeoutRunnable!!, 15000)
    }

    fun acceptCallBackground(callerId: String) {
        AppLogger.logEvent("webrtc_accept_call_background")
        this.targetUserId = callerId
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val myId = prefs.getString("user_id", "") ?: ""

        isCallActive.value = true
        rtcState.value = "Connecting..."
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false

        if (socket?.connected() == true) {
            val payload = JSONObject().apply {
                put("targetUserId", callerId)
                put("responderId", myId)
            }
            socket?.emit("accept-call", payload)
        } else {
            pendingAcceptCallerId = callerId
            socket?.connect()
        }
    }

    private fun setupPeerConnection(targetId: String) {
        if (peerConnection != null) return 

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:76.13.5.244:3478").setUsername("myuser").setPassword("mypassword").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    val candJson = JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }
                    socket?.emit("ice-candidate", JSONObject().put("targetUserId", targetId).put("candidate", candJson))
                }
                
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    mainHandler.post {
                        if (newState == PeerConnection.IceConnectionState.CONNECTED || newState == PeerConnection.IceConnectionState.COMPLETED) {
                            AppLogger.logEvent("webrtc_ice_connected")
                            offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                            rtcState.value = "Connected"
                        } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                            AppLogger.logEvent("webrtc_ice_failed")
                            rtcState.value = "Connection Failed"
                        }
                    }
                }
                
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
                override fun onDataChannel(p0: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            }
        )

        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio0", audioSource)
        peerConnection?.addTrack(localAudioTrack, listOf("stream0"))
    }

    private fun startWebRTCCall(targetId: String) {
        setupPeerConnection(targetId)

        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val myId = prefs.getString("user_id", "") ?: ""

        peerConnection?.createOffer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) return
                    peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                    val sdpJson = JSONObject().apply {
                        put("type", "offer")
                        put("sdp", sdp.description)
                    }
                    socket?.emit("offer", JSONObject().put("targetUserId", targetId).put("callerId", myId).put("sdp", sdpJson))
                }
            },
            MediaConstraints()
        )
    }

    private fun handleOffer(callerId: String, sdpString: String) {
        this.targetUserId = callerId
        setupPeerConnection(callerId)

        peerConnection?.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    hasRemoteDescription = true
                    drainRemoteCandidates()

                    peerConnection?.createAnswer(
                        object : SimpleSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription?) {
                                if (sdp == null) return
                                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                                val ansJson = JSONObject().apply {
                                    put("type", "answer")
                                    put("sdp", sdp.description)
                                }
                                socket?.emit("answer", JSONObject().put("targetUserId", callerId).put("sdp", ansJson))
                            }
                        },
                        MediaConstraints()
                    )
                }
            },
            SessionDescription(SessionDescription.Type.OFFER, sdpString)
        )
    }

    private fun drainRemoteCandidates() {
        remoteCandidatesQueue.forEach { candidate ->
            peerConnection?.addIceCandidate(candidate)
        }
        remoteCandidatesQueue.clear()
    }

    fun toggleMute(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun toggleSpeaker(isSpeaker: Boolean) {
        audioManager.isSpeakerphoneOn = isSpeaker
    }

    fun endCall(callerId: String? = null, onCallEnded: (() -> Unit)? = null) {
        AppLogger.logEvent("webrtc_end_call_emitted")
        offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        
        if (this.targetUserId == null && callerId != null) {
            this.targetUserId = callerId
        }
        
        val target = targetUserId ?: callerId
        
        if (target != null) {
            val payload = JSONObject().apply { put("targetUserId", target) }
            
            if (socket?.connected() == true) {
                socket?.emit("end-call", payload)
                mainHandler.postDelayed({
                    cleanup()
                    onCallEnded?.invoke()
                }, 500)
            } else {
                var handled = false
                socket?.connect()
                socket?.once(Socket.EVENT_CONNECT) {
                    if (!handled) {
                        handled = true
                        mainHandler.postDelayed({
                            socket?.emit("end-call", payload)
                            mainHandler.postDelayed({
                                cleanup()
                                onCallEnded?.invoke()
                            }, 500)
                        }, 500) 
                    }
                }
                
                mainHandler.postDelayed({
                    if (!handled) {
                        handled = true
                        cleanup()
                        onCallEnded?.invoke()
                    }
                }, 8000)
            }
        } else {
            cleanup()
            onCallEnded?.invoke()
        }
    }

    fun cleanup() {
        offlineTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        peerConnection?.close()
        peerConnection = null
        socket?.disconnect()
        audioManager.mode = AudioManager.MODE_NORMAL
        
        isCallActive.value = false
        targetUserId = null
        hasRemoteDescription = false
        remoteCandidatesQueue.clear()
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}