//TOR PT Connection
var tor_conn = new WebSocket(window.tor_conn_addr);

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

var socket = null;
var room_n = -1;
var isChannelReady = false;
var isStarted = false;

var socket_to_node = io(window.local_node_addr);

socket_to_node.on('bridge', function (room) {
    room_n = room;
    socket_to_node.disconnect();

    socket = io(window.signalling_server[room_n]);
    
    socket.on('want_join', function (room) {
        room_n = room;
        socket.emit('create', room);
    });
    
    socket.on('ready', function (room) {
        isChannelReady = true;
        leftVideo.play();
        maybeStart();
    });
    
    socket.on('message', function (message, room) {
        if (message.type === 'answer' && isStarted) {
            pc.setRemoteDescription(new RTCSessionDescription(message));
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
    
    socket.emit('want_join_server');
});


function sendMessage(message, room) {
    socket.emit('message', message, room);
};


//WEBRTC
window.addEventListener("beforeunload", function (event){
    hangup();
    return null;
});

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
        stream.getTracks().forEach((track) => {pc.addTrack(track, stream)});
        pc.getSenders().forEach(setupSenderTransform);
        isStarted = true;

        forceCodec('video/VP8');
        doCall();
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
        pc = new RTCPeerConnection(window.webrtc);

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

function handleCreateOfferError(event) {
    console.log('createOffer() error: ', event);
}

function doCall() {
    pc.createOffer(setLocalAndSendMessage, handleCreateOfferError);
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
        const to_encode = enconding[0];
        enconding.splice(0,1);

        const newData = new ArrayBuffer(encodedFrame.data.byteLength + to_encode.length + 2 + 2);
        
        const newView = new DataView(newData);
       
        for(let i = 0; i < to_encode.length; i++){
            newView.setInt8(i + encodedFrame.data.byteLength, to_encode[i]);
        }

        newView.setUint16(encodedFrame.data.byteLength + to_encode.length, to_encode.length);
        newView.setUint16(encodedFrame.data.byteLength + to_encode.length + 2, 12345);
        
        encodedFrame.data = newData;
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
            
            var bytes = [] 

            for(let i = encodedFrame.data.byteLength - 4 - len; i < encodedFrame.data.byteLength - 4; i++){
                bytes.push(view.getUint8(i));
            }

            tor_conn.send(decode(bytes));
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


function decode(bytes){
    var result = btoa(bytes.map(function(v){return String.fromCharCode(v)}).join(''))
    return result;
}


