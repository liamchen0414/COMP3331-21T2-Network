internet protocol stack
1. application layer: supporting network applications: FTP, SMTP, HTTP...
2. transport layer: process-process data transfer: TCP, UDP
3. network layer: routing of datagrams from source to destination: IP, routing protocols
4. link layer: data transfer between neighboring network elements: Ethernet, 802.111 (WiFi), PPP
5. physical layer: bits“on the wire”

benefits of layering
introducing an intermediate layer provides a common abstraction for various network technologies
Application (ssh, http, skype) => transport & network => transmission media(ethernet, fiber optic, wireless)

disadvantages of layering
layer may duplicate lower-level functionality, e.g. Error recovery to retransmit lost data
information hiding may hurt performance: e.g. Packet loss due to corruption vs congestion
headers start to get large: e.g. Typically, TPC+IP+Ethernet headers add up to 54 bytes
layer violations when gains too great to resist: e.g. NAT
layer violations when network doesn't trust ends: e.g. Firewalls

What are two benefits of using a layered network model? (Choose two)
it makes it wasy to introduce new protocols
it prevents technology in one layer from affecting other layers

Application layer


# you need to choose the architecture of your application
## client-server
	server: 
		exports well defined requests/response interface
		long-lived process that waits for requests
		upon receiving request, carries it out
	client:
		short-lived process that makes requests
		user-side of application
		initiate the communication
client vs server
	server:
		always-on host
		permanent IP address
		static port conventions
		data centres for scaling
		may communicate with other servers to respond
	client
		may be intermittently connected
		may have dynamic IP address
		do not communicate directly with each other
## peer to peer
	no always on server
	arbitrary end systems directly communicate
	symmetric responsibility
	often used for:
		file sharing
		games
		blockchain and cryptocurrencies
		video distribution, video chat

### advantage of P2P
	Speed: parallelism, less contention
	Reliability: redundancy, fault tolerance
	Geographic distribution

### disadvantage of P2P
	Fundamental problems of decentralized control
		State uncertainty: no shared memory or clock
		Action uncertainty: mutually conflicting decisions

# what transport service does an app need? (TCP or UDP)
data integrity: some apps require 100% reliable data transfer(TCP), other apps can tolerate some loss(UDP)
timing: internet telephony, interactive games require low delay to be effective
throughput: some apps require minimum amount of throughput to be effective
security: encryption, data integrity etc

## TCP
reliable tranport
flow control
congestion control
doesn't provide : timing, minimum throughput, guarantee, security
connection-oriented: setup required between client and server processes

## UDP
unreliable data transfer
doesn't provide: reliability, flow control, congestion control, timing, 
throughput guarantee, security, orconnection setup

### why do we need UDP?
in some applications, 