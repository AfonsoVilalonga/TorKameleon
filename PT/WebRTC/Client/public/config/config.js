window.webrtc = {
    iceServers: [{
        urls: [ "stun:eu-turn2.xirsys.com" ]
     }, {
        username: "tykNiA_lpo762O254PychQp_Fjntt9xR-C8rjrgRAyt9qZ0bwQDjOhVzeAWz5jUZAAAAAGLWlcp5b25vaDE2NDkz",
        credential: "2add367a-0756-11ed-b39b-0242ac140004",
        urls: [
            "turn:eu-turn2.xirsys.com:80?transport=udp",
            "turn:eu-turn2.xirsys.com:3478?transport=udp",
            "turn:eu-turn2.xirsys.com:80?transport=tcp",
            "turn:eu-turn2.xirsys.com:3478?transport=tcp",
            "turns:eu-turn2.xirsys.com:443?transport=tcp",
            "turns:eu-turn2.xirsys.com:5349?transport=tcp"
        ]
     }],
     
     
     
    encodedInsertableStreams: true
};

window.modulation = 'add';

window.signalling_server = ['wss://5.196.26.66:10000'];
//window.signalling_server = ['null','wss://localhost:8000'];
window.tor_conn_addr = 'ws://localhost:4444';
window.local_node_addr = 'ws://localhost:8002';
