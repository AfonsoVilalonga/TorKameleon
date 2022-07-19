window.webrtc = {
    iceServers: [{
        urls: [ "stun:fr-turn1.xirsys.com" ]
     }, {
        username: "01MTi058dhJGL5x3MLQHQPCTk1yPwYLhIyhQjPW9FUjZa0xxAvD8JeyvHUW3mgkYAAAAAGLVTNlib2dlbWk2ODM1",
        credential: "1a3dca9c-0692-11ed-8bcd-0242ac120004",
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

window.modulation = 'add';

window.signalling_server = ['wss://5.196.26.66:8000'];
//window.signalling_server = ['null','wss://localhost:8000'];
window.tor_conn_addr = 'ws://localhost:4444';
window.local_node_addr = 'ws://localhost:8002';
