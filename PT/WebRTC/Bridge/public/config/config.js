window.webrtc = {
   iceServers: [{
      urls: [ "stun:fr-turn1.xirsys.com" ]
   }, {
      username: "OffHKG4nEm-Ml8IL_lGIC8l1OU71h3mw0ukxmBhGlkVSYCTVsph5cyPrW3X77f44AAAAAGLLZSxnYXNpeTE1OTI4",
      credential: "b8a6ac46-00aa-11ed-a552-0242ac120004",
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

//window.signalling_server = 'wss://localhost:8000';
window.signalling_server = 'wss://5.196.26.66:8000';
window.tor_conn_addr = 'ws://localhost:4444';