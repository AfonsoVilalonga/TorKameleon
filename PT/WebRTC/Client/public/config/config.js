window.webrtc = {
    iceServers: [{
        urls: [ "stun:fr-turn1.xirsys.com" ]
     }, {
        username: "EgKMuc0h07Gydlgl9NhHfGwaTVGygqmGPXxN0K2elnwF63zMb4TdjXvFdVahm-ejAAAAAGLRaaJsYWRlY282NjY1",
        credential: "e8620964-0440-11ed-bda4-0242ac120004",
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

window.modulation = 'replace';

window.signalling_server = ['wss://5.196.26.66:8000'];
//window.signalling_server = ['null','wss://localhost:8000'];
window.tor_conn_addr = 'ws://localhost:4444';
window.local_node_addr = 'ws://localhost:8002';
