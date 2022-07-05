window.webrtc = {
    iceServers: [{
        urls: [ "stun:fr-turn1.xirsys.com" ]
     }, {
        username: "-71sy_KL_1XHohz9pAchFcAHCEAPQ3OX7iYGaJhjQhpxv1WrdQo6QSC20W3dnUBaAAAAAGLEW2lwb2dvcmEzMTQy",
        credential: "c9a51de2-fc78-11ec-b5f0-0242ac120004",
        urls: [
            "turn:fr-turn1.xirsys.com:80?transport=udp",
            "turn:fr-turn1.xirsys.com:3478?transport=udp",
            "turn:fr-turn1.xirsys.com:80?transport=tcp",
            "turn:fr-turn1.xirsys.com:3478?transport=tcp",
            "turns:fr-turn1.xirsys.com:443?transport=tcp",
            "turns:fr-turn1.xirsys.com:5349?transport=tcp"
        ]
     }],
    encodedInsertableStreams: true
};

//window.signalling_server = ['wss://5.196.26.66:8000'];
window.signalling_server = ['null','wss://5.196.26.66:8000'];
window.tor_conn_addr = 'ws://localhost:4444';
window.local_node_addr = 'ws://localhost:8001';
