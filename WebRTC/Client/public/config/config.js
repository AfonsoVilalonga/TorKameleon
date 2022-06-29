window.webrtc = {
    iceServers: [{
        urls: [ "stun:eu-turn4.xirsys.com" ]
     }, {
        username: "Es2MF1dNpixy1MuvrIgjIT-5R33ZlXc8sj1AoJQJka8Kk9_guwMDUmFHDlswCxfMAAAAAGK8vFF0ZXN0ZXdlYnJ0Y29pMTIzNDU=",
        credential: "d8c91006-f7ed-11ec-a2a0-0242ac140004",
        urls: [
            "turn:eu-turn4.xirsys.com:80?transport=udp",
            "turn:eu-turn4.xirsys.com:3478?transport=udp",
            "turn:eu-turn4.xirsys.com:80?transport=tcp",
            "turn:eu-turn4.xirsys.com:3478?transport=tcp",
            "turns:eu-turn4.xirsys.com:443?transport=tcp",
            "turns:eu-turn4.xirsys.com:5349?transport=tcp"
        ]
     }],
    encodedInsertableStreams: true
};

//window.signalling_server = 'wss://5.196.26.66:8000';
window.signalling_server = 'wss://localhost:8000';
window.tor_conn_addr = 'ws://localhost:4444';