## TorKameleon

TorKameleon is a solution designed to protect Tor users with increased censorship resistance against powerful traffic correlation attacks carried out by global adversaries. The system is based on flexible K-anonymization input circuits that can support TLS tunneling and WebRTC-based covert channels before forwarding users' original input traffic to the Tor network. Our goal is to protect users from correlation attacks through machine learning between incoming user traffic and the observed traffic at different Tor network intermediate relays or outgoing traffic to destinations. TorKameleon is the first system to implement a Tor pluggable transport based on both parameterizable TLS tunneling or WebRTC media as covert channels. We have implemented the TorKameleon prototype and performed extensive validations to observe the correctness and experimental performance of the proposed solution in the Tor environment.

## Repo Folders
The repository currently consists of four different folders: 
* The Client folder, which contains the code and configuration files for the client simulator Java program;
* The Deployment folder, which contains the Docker and configuration files for deploying TorKameleon in different modes and for deploying all the necessary components to create the setup described below;
* The HttpServer folder, which contains the code and configuration files for the HTTP Java server;
* The PT folder, which contains the code and configuration files for the TorKameleon software (including the necessary WebRTC-based application code and configuration files).


## Prerequisites
Depending on how the TorKameleon is used, different prerequisites are required.
If the deployment files are used (strongly recommended): 
* Docker (any version should do it, but the version used was 20.10.16);
* Docker compose (any version should do it, but the version used was 2.8.0);
* Ubuntu (20.04 and above)

When running TorKameleon with the jars (not recommended), the system has been tested with the following software versions:
* Java 11
* Maven 3.8.1 
* NodeJS
* Stunnel 5.62 (higher versions should work)
* Tor 0.4.6.7 (higher versions should work)
* Ubuntu 20.04

## Setup


In our experiments, we used a setup similar to the one shown in the figure above. There are five main components in this setup: 
* The local machine, which is used as the client and runs the TorKameleon Proxy locally;
* The TorKameleon Proxy, which receives the encapsulated data from the local machine via a WebRTC-based covert channel or TLS tunnel;
* The TorKameleon Tor Bridge, which receives the encapsulated data from the TorKameleon Proxy over a WebRTC-based covert channel or TLS tunnel;
* The HTTP server, which is used as the final destination for the client (the HTTP server can also be deployed as a hidden service);
* The STUN / TURN server used as a STUN and or TURN server for the WebRTC connections.

The setup can also be used without the Tor network component (TorKameleon Tor Bridge, Tor Middle Relay, and Tor Exit Relay) by forwarding traffic between the local machine and the proxy and from the proxy to the HTTP server, or without the proxy component by having the local machine forward traffic directly to the TorKameleon Tor Bridge.


## Configuration
There are two main configurable components in the TorKameleon system: the TorKameleon core and the WebRTC-based application. It is also possible to configure the deployment files, but these are only configurations of the test deployment, which in turn are configurations of the two aforementioned components.

### WebRTC Config Files
The WebRTC-based configuration files can be accessed via the folders ```PT/WebRTC/Bridge/public/config/``` or ```PT/WebRTC/Client/public/config/```. There is also a Signaling server that can be configured at ```PT/WebRTC/Signalling/config/```.

The first configures the WebRTC-based web application in receiver mode (the web application that receives and accepts all incoming WebRTC connections to a TorKameleon Tor Bridge or TorKameleon Proxy). There are three configurable fields in the configuration file:

* ```window.webrtc```: used to specify the IP and credentials of the TURN and STUN server and whether to use a TURN server (by adding in the second line ```iceTransportPolicy: 'relay',```);
* ```window.signalling_server```: used to specify the IP and port of the signaling server. It must always be localhost, but the port may change (although it must be the same port configured in the Signaling server configuration file);
* ```window.tor_conn_addr```: used to specify the IP and port of the WebSocket connection to the TorKameleon core software (it works like an interprocess connection between the TorKameleon Java core and the web application). It must always be localhost, but the port can change (although it must be the same port configured in the TorKameleon core configuration file);

The second configures the WebRTC-based web application in initiator mode (the web application that starts the WebRTC connections received by a TorKameleon Tor bridge or proxy). The configuration file is the same as the first one, with three exceptions:

* ```window.signalling_server```: instead of being just a localhost address, in the web application in initiator mode it is an array of addresses, where the first position is always the address of the TorKameleon Tor bridge (if no TorKameleon Tor bridge is used, the first position of the array must be the string ```"null"```) and after that, the addresses represent the TorKameleon proxies that make up the pre-staged network. The addresses of the proxies must be in the same order as in the TorKameleon core network configuration file;
* ```window.local_node_addr```: is used to make a WebSocket connection to nodeJS's local server to get from it the position in the array of signaling server addresses to which it will try to connect. The address must always be localhost, but the port may change (although it must be the same as configured in the TorKameleon core configuration files);
* ```window.modulation```: the encapsulation method (```"add"``` or ```"replace"```) to be used in the WebRTC-based covert channel;

The Signaling server can also be configured, namely:
* ```port```: The port assigned to the signaling server; 
* ```pem```: Location of the pem file for the secure WebSocket connections to the signaling server;
* ```key```: Location of the key file for the secure WebSocket connections to the signaling server;


### TorKameleon Core Config Files
There are two configurable files for the TorKameleon core: the ```network``` configuration file and the ```config.properties``` configuration file. They are both located in the ```PT/Config/``` folder.

#### config.properties
The config.properties is a file used to configure all possible configurations in the TorKameleon core system. It has the following properties:
* ```tor_buffer_size```: the size of the Tor cells (should be left at the default value, it is the same size used by the Tor network);
* ```number_of_nodes```: the number of TorKameleon proxies in the pre-staged network;
* ```bypass_timer```: the time in seconds required to switch the next proxy to which the data is forwarded;
* ```remote_host```, ```remote_port```: the IP and port of the target destination (the IP and port of the HTTP server);
* ```local_port_unsecure```, ```local_port_secure```, and ```streaming_port_proxy```: the open ports for incoming TCP and UDP, TLS and DTLS, and WebRTC protocol handshake connections respectively (should be left as default);
* ```tor_port```, ```tor_ip```, and ```control_tor_port```: open port and IP of the Tor client for connections between the Tor client and TorKameleon and the open control port of the Tor client (configured in the torrc file, all these configurations should be left as default);
* ```stunnel_port```: open port on the receiving TorKameleon proxy for the stunnel SSL tunnel (should be left as default);
* ```connect_streaming```: open port on the receiving TorKameleon proxy for incoming connections of the WebRTC protocol handshake (should be left as default);
* ```modulation```: type of modulation used for the TorKameleon proxy and TorKameleon pluggable transport (```streaming``` or ```stunnel```);
* ```pt_client_port```: open port for the SOCKS5 proxy for the TorKameleon pluggable transport (should be left as default);
* ```pt_server_port```: open port for the reverse proxy for the TorKameleon pluggable transport (should be left as default);
* ```or_port```: open ORport of the Tor client configured in the torrc file (should be left as default); 
* ```keystore```, ```password```, and ```key```: location of the keystore, password of the Keystore and location of the key file for the secure communication used for the protocols and SSL tunnels; 
* ```client_streaming_port```: open port for the local nodeJS server serving the web application in initiator mode (should be left as default); 
* ```bridge_streaming_port```: open port for the local nodeJS server serving the web application in receiver mode (should be left as default);
* ```webdriver_location```: the location of the web driver for the Chrome driver used in the Selenium framework;
* ```proxy_buffer_size```: the size of the encapsulated data between TorKameleon proxies;
* ```pt_buffer_size```: the size of the encapsulated data between the client-side TorKameleon pluggable transport and the server-side TorKameleon pluggable transport;
* ```websocket_port```: open port for the WebSocket server used to communicate with the web application (should be left as default);
* ```webrtc_location```: the location of the WebRTC files;
* ```watch_video```: used to launch the Chrome browser with or without a graphical user interface;

For examples of how to configure various components (proxies, bridges, clients, and so on), see the Deployment folder.

#### network

The addresses of the proxies are configured in the network file. The proxies should be added in the following format: ```IP-Encapsulation_method-Order_number```, where IP is the IP of the proxy, Encapsulation_method is the type of encapsulation to be used with this specific proxy (```s```, i.e. streaming, or ```t```, i.e. TLS), and Order_number is the number of the proxy in the network file (the first is 1, the second is 2, etc.). The window.signalling_server property of the WebRTC web application in initiator mode follows the same order in the array as in the network file. 


## Deployment
The deployment folder contains two folders, the ```/Deployment/Coturn/``` folder and the ```/Deployment/Setup/``` folder.

### Coturn Folder

The Coturn folder contains the configuration file for configuring the Coturn TURN and STUN server. To deploy the Coturn server on a machine, the following command should be used, using the configuration file in the Coturn folder: 

```docker run -d --network=host -v $(pwd)/my.conf:/etc/coturn/turnserver.conf coturn/coturn```

The coturn configuration file should be located in the current directory.

### Setup Folder

## Usage
The PT, the client, and the HttpServer are Maven projects that can be compiled and run with the Jar, but to use TorKameleon, the deployment files should be used and the various components of TorKameleon should be deployed with the Docker containers.

### Client

### HTTP Server 

### TorKameleon Tor Bridge

### TorKameleon Proxy
