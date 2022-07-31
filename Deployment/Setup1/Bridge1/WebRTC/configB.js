window.webrtc = {
   iceTransportPolicy: 'relay',
   iceServers: [{
      urls: ["stun:54.38.65.236"]
   }, {
      username: "test",
      credential: "test123",
      urls: [
         "turn:54.38.65.236:3478"
      ]
   }],

   encodedInsertableStreams: true
};

//window.signalling_server = 'wss://localhost:8000';
window.signalling_server = 'wss://localhost:10000';
window.tor_conn_addr = 'ws://localhost:4444';