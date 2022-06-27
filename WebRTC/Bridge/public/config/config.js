window.webrtc = {
    iceServers: [{
        urls: [ "stun:eu-turn5.xirsys.com" ]
     }, {
        username: "J3y90t6JVJ4HNEOJcV6NqaprxU_vaXpFP-buT48SDdaAR5RKLTumz0eY0oTWMCfWAAAAAGK4nMd0ZXN0ZXdlYnJ0YzEyMw==",
        credential: "b1ee12d2-f578-11ec-a528-0242ac140004",
        urls: [
            "turn:eu-turn5.xirsys.com:80?transport=udp",
            "turn:eu-turn5.xirsys.com:3478?transport=udp",
            "turn:eu-turn5.xirsys.com:80?transport=tcp",
            "turn:eu-turn5.xirsys.com:3478?transport=tcp",
            "turns:eu-turn5.xirsys.com:443?transport=tcp",
            "turns:eu-turn5.xirsys.com:5349?transport=tcp"
        ]
     }],     
    encodedInsertableStreams: true
};
window.signalling_server = 'wss://localhost:8000';
//window.signalling_server = 'wss://5.196.26.66:8000';
window.tor_conn_addr = 'ws://localhost:4444';