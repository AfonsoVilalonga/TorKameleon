window.webrtc = {
    iceServers: [{
        urls: [ "stun:eu-turn6.xirsys.com" ]
     }, {
        username: "mfdlqlPnR3r7lW1zEsq2Rm9_9Ebk7cU9m2cb-IJfrSHdXFZuULZ7neCfS_5QmTPwAAAAAGK-B9d3YnJ0YzEyMzQ1NnRlc3Rl",
        credential: "73488402-f8b3-11ec-9a53-0242ac140004",
        urls: [
            "turn:eu-turn6.xirsys.com:80?transport=udp",
            "turn:eu-turn6.xirsys.com:3478?transport=udp",
            "turn:eu-turn6.xirsys.com:80?transport=tcp",
            "turn:eu-turn6.xirsys.com:3478?transport=tcp",
            "turns:eu-turn6.xirsys.com:443?transport=tcp",
            "turns:eu-turn6.xirsys.com:5349?transport=tcp"
        ]
     }],
    encodedInsertableStreams: true
};
window.signalling_server = 'wss://localhost:8000';
//window.signalling_server = 'wss://5.196.26.66:8000';
window.tor_conn_addr = 'ws://localhost:4444';