//TOR PT Connection
var tor_conn = new WebSocket(tor_conn_addr);

tor_conn.onerror = function (err) {
    tor_conn.close();
};

tor_conn.onopen = function () {};

tor_conn.onclose = function () {};

tor_conn.onmessage = function (message) {
    addEnconding(message);
};


//SIGNALLING
let stream;

var localVideo = document.querySelector('#leftVideo');
const remoteVideo = document.createElement('video');

var socket = io(signalling_server);
var room_n = -1;
var isChannelReady = false;
var isStarted = false;


socket.on('bridge_join', function (room) {
    if (room_n == -1) {
        room_n = room;
        socket.emit("join", room_n);
    }
});

socket.on('ready', function (room) {
    isChannelReady = true;
    leftVideo.play();
});

socket.on('message', function (message, room) {
    if (message.type === 'offer') {
        if (!isStarted) {
            maybeStart();
        }
        pc.setRemoteDescription(new RTCSessionDescription(message));

        doAnswer();
    } else if (message.type === 'candidate' && isStarted) {
        var candidate = new RTCIceCandidate({
            sdpMLineIndex: message.label,
            candidate: message.candidate
        });
        pc.addIceCandidate(candidate);
    } else if (message === 'end' && isStarted) {
        handleRemoteHangup();
    }
});

function sendMessage(message, room) {
    socket.emit('message', message, room);
};



//WEBRTC
window.addEventListener("beforeunload", function (event){
    hangup();
    return null;
});

var transform = null;

switch(modulation) {
    case 'replace':
        transform = new Transform(encondeReplacing, decodeReplacing);
      break;
    default:
        transform = new Transform(encondeReplacing, decodeReplacing);
}

let enconding = [];

localVideo.oncanplay = getStream;
if (leftVideo.readyState >= 3) {
    getStream();
}

function getStream() {
    if (leftVideo.captureStream) {
        stream = leftVideo.captureStream();
    } else if (leftVideo.mozCaptureStream) {
        stream = leftVideo.mozCaptureStream();
    } else {
        console.log('captureStream() not supported');
    }
}


function maybeStart() {
    if (!isStarted && typeof stream !== 'undefined' && isChannelReady) {
        createPeerConnection();
        stream.getTracks().forEach((track) => {
            pc.addTrack(track, stream)
        });
        pc.getSenders().forEach(setupSenderTransform);
        isStarted = true;
    }
}

function forceCodec(preferredVideoCodecMimeType) {
    const {
        codecs
    } = RTCRtpSender.getCapabilities('video');
    const selectedCodecIndex = codecs.findIndex(c => c.mimeType === preferredVideoCodecMimeType);
    const selectedCodec = codecs[selectedCodecIndex];
    const transceiver = pc.getTransceivers().find(t => t.sender && t.sender.track === stream.getVideoTracks()[0]);
    transceiver.setCodecPreferences([selectedCodec]);
}

function createPeerConnection() {
    try {
        pc = new RTCPeerConnection(webrtc);

        pc.onicecandidate = handleIceCandidate;
        
        pc.ontrack = e => {
            setupReceiverTransform(e.receiver);
            handleRemoteStreamAdded(e.streams[0]);
        };

        pc.onremovestream = handleRemoteStreamRemoved;
    } catch (e) {
        alert('Cannot create RTCPeerConnection object.');
        return;
    }
}

function handleIceCandidate(event) {
    if (event.candidate) {
        sendMessage({
            type: 'candidate',
            label: event.candidate.sdpMLineIndex,
            id: event.candidate.sdpMid,
            candidate: event.candidate.candidate
        }, room_n);
    } else {
        console.log('End of candidates.');
    }
}

function doAnswer() {
    pc.createAnswer().then(
        setLocalAndSendMessage,
        onCreateSessionDescriptionError
    );
}

function setLocalAndSendMessage(sessionDescription) {
    pc.setLocalDescription(sessionDescription);
    sendMessage(sessionDescription, room_n);
}

function onCreateSessionDescriptionError(error) {
    trace('Failed to create session description: ' + error.toString());
}

function handleRemoteStreamAdded(event) {
    if (remoteVideo.srcObject !== event) {
        const box = document.getElementById('div2');
        remoteVideo.srcObject = event;
        remoteVideo.autoplay = true;
        remoteVideo.controls = true;
        box.appendChild(remoteVideo);
    }
}

function handleRemoteStreamRemoved(event) {
    console.log('Remote stream removed. Event: ', event);
}

function hangup() {
    stop();
    sendMessage('end', room_n);
}

function handleRemoteHangup() {
    stop();
    isInitiator = false;

    if(tor_conn.readyState == 1){
        tor_conn.close();
    }
}

function stop() {
    isStarted = false;
    pc.close();
    pc = null;
}

function setupSenderTransform(sender) {
    const senderStreams = sender.createEncodedStreams();
    const {
        readable,
        writable
    } = senderStreams;

    const transformStream = new TransformStream({
        transform: encodeFunction,
    });
    readable.pipeThrough(transformStream).pipeTo(writable);
}

function setupReceiverTransform(receiver) {
    const receiverStreams = receiver.createEncodedStreams();
    const {
        readable,
        writable
    } = receiverStreams;

    const transformStream = new TransformStream({
        transform: decodeFunction,
    });
    readable.pipeThrough(transformStream).pipeTo(writable);
}

function encodeFunction(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame && enconding.length > 0) {
        encodedFrame.data = transform.getModulator()(encodedFrame);
    }

    controller.enqueue(encodedFrame);
}

let prevFrameType;
let prevFrameTimestamp;
let prevFrameSynchronizationSource;



function decodeFunction(encodedFrame, controller) {
    if (encodedFrame instanceof RTCEncodedVideoFrame) {
        const view = new DataView(encodedFrame.data);
        
        const hasencoded = view.getUint16(encodedFrame.data.byteLength-2);
        const len = view.getUint16(encodedFrame.data.byteLength-4);

        if (!(encodedFrame.type === prevFrameType &&
                encodedFrame.timestamp === prevFrameTimestamp &&
                encodedFrame.synchronizationSource === prevFrameSynchronizationSource) && hasencoded == 12345) {
            

            tor_conn.send(transform.getDemodulator()(encodedFrame, len));
            encodedFrame.data = encodedFrame.data.slice(0, encodedFrame.data.byteLength - 4 - len);
            
        }
        
        prevFrameType = encodedFrame.type;
        prevFrameTimestamp = encodedFrame.timestamp;
        prevFrameSynchronizationSource = encodedFrame.synchronizationSource;
    }

    controller.enqueue(encodedFrame);
}

function addEnconding(bytes) {
    var x = atob(bytes.data);
    var len = x.length;
    var bytes = new Uint8Array(len);
    for (var i = 0; i < len; i++) {
        bytes[i] = x.charCodeAt(i);
    }

    enconding.push(bytes);    
}

