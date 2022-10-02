## TorKameleon

TorKameleon is a solution designed to protect Tor users with increased censorship resistance against powerful traffic correlation attacks carried out by global adversaries. The system is based on flexible K-anonymization input circuits that can support TLS tunneling and WebRTC-based covert channels before forwarding users' original input traffic to the Tor network. Our goal is to protect users from correlation attacks through machine learning between incoming user traffic and the observed traffic at different Tor network intermediate relays or outgoing traffic to destinations. TorKameleon is the first system to implement a Tor pluggable transport based on both parameterizable TLS tunneling or WebRTC media as covert channels. We have implemented the TorKameleon prototype and performed extensive validations to observe the correctness and experimental performance of the proposed solution in the Tor environment.

## Setup
![Alt text](https://github.com/AfonsoVi/TorKameleon/blob/master/Setup.png)

In our experiments, we used a setup similar to the one shown in the figure above. There are five main components in this setup: 
* The local machine, which is used as the client and runs the TorKameleon Proxy locally;
* The TorKameleon Proxy, which receives the encapsulated data from the local machine via a WebRTC-based covert channel or TLS tunnel;
* The TorKameleon Tor Bridge, which receives the encapsulated data from the TorKameleon Proxy over a WebRTC-based covert channel or TLS tunnel;
* The HTTP server, which is used as the final destination for the client;
* The STUN / TURN server used as a STUN and or TURN server for the WebRTC connections.


## Configuration

### WebRTC Config Files


### TorKameleon Core Config Files


### Deployment


## Usage



