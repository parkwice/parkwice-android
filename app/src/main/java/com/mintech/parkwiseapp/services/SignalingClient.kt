package com.mintech.parkwiseapp.services

import android.content.Context
import android.media.AudioManager
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
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val myId = prefs.getString("user_id", "") ?: ""
            if (myId.isNotEmpty()) {
                socket?.emit("register", myId)
                
                pendingAcceptCallerId?.let { callerId ->
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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

        socket?.on("call-accepted") { args ->
            val data = args[0] as JSONObject
            val responderId = data.getString("responderId")
            rtcState.value = "Receiver joined. Negotiating..."
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

    fun initiateCall(targetId: String) {
        this.targetUserId = targetId
        isCallActive.value = true
        rtcState.value = "Ringing..."
        socket?.connect()
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }

    fun acceptCallBackground(callerId: String) {
        this.targetUserId = callerId
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val myId = prefs.getString("user_id", "") ?: ""

        isCallActive.value = true
        rtcState.value = "Connecting Securely..."
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
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (newState == PeerConnection.IceConnectionState.CONNECTED || newState == PeerConnection.IceConnectionState.COMPLETED) {
                            rtcState.value = "Connected"
                        } else if (newState == PeerConnection.IceConnectionState.FAILED) {
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
        if (this.targetUserId == null && callerId != null) {
            this.targetUserId = callerId
        }
        
        val target = targetUserId ?: callerId
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
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
                
                // 🚨 INCREASED TIMEOUT: Gives the app 8 seconds to connect to the network from the background
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