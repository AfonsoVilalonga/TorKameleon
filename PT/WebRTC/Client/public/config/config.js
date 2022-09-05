window.webrtc = {
   //iceTransportPolicy: 'relay',
   iceServers: [{
      urls: ["stun:54.38.65.236"]
   }, {
      username: "test",
      credential: "test123",
      urls: [
         "turn:54.38.65.236:3478?transport=tcp"
      ]
   }],
     
    encodedInsertableStreams: true
};

window.modulation = 'add';
window.signalling_server = ['null', 'wss://37.187.198.176:10002'];
//window.signalling_server = ['null','wss://localhost:8000'];
window.tor_conn_addr = 'ws://localhost:4444';
window.local_node_addr = 'ws://localhost:8002';
