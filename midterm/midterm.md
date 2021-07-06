# Key topics: Introduction

## Packet Switching/Circuit Switching

### Circuit Switching: used in legacy telephone networks
end-end resources allocated to, reserved for "call" between source and destination

issue with circuit switching
* inefficient: computer communications tends to be very bursty, dedicated circuit cannot be used or shared in periods of silence, and cannot adopt to web dynamics.
* fixed data rate
* connection state maintenance: requires per communication state to be maintained that is a considerable overhead, easy router along the way must keep all the information, not scalable.

### Packet Switching: used in the internet
Data is sent as chunks of formatted bits, Packets consist of a “header” and “payload”, Switches “stre and forward” packets based on their headers. Each packet travels independently

**Statistical Multiplexing:**
No link resources are reserved in advance. Instead, packet switching leverages statistical multiplexing

1. No overloading: Statistical multiplexing relies on the assumption that not all flows burst at the same time
2. With overloading: Queue overload into Buffer. 
3. What about persistent overload? Will eventually drop packets, provision the network better


## Delay, Loss, Throughput
**dnodal = dproc + dqueue + dtrans + dprop**
* nodal processing delay: relatively negligible
* queuing delay: Packet arrival rate to link (temporarily) exceeds output link capacity
* transmission delay: L/R, L: packet length (bits), R: link bandwidth (bps)
* propagation delay: d/s, d: length of physical link, s: propagation speed in medium (~2x10^8 m/sec)

**packet loss: Buffer gets full**

**throughput**
Small Rs to Rc, throughput is Rs
Large Rs to Rc, throughput is Rc
Internet scenario: pre connection end to end throughput min(Rs,RC,R/n)

## Protocol 

What's a protocol: protocols define format, order of msgs sent and received among network entities, and actions taken on msg transmission receipt

## Protocol stack and Layer
* application: supporting network applications
    § FTP, SMTP, HTTP, Skype, ..
* transport: process-process data transfer
    § TCP, UDP
* network: routing of datagrams from source to destination
    § IP, routing protocols
* link: data transfer between neighboring network elements
    § Ethernet, 802.111 (WiFi), PPP
* physical: bits “on the wire”

1. each layer depends on the layer below
2. intermediate layer provides support to the layer above
3. layers are independent

layering vs no layering

No layering: each new application has to be reimplemented for every network technology. Introducing an intermediate layer provides a common
abstraction for various network technologie

Issue with layering:
* Layer N may duplicate lower-level functionality, Information hiding may hurt performance
* Headers start to get large: typically, TCP + IP + Ethernet headers add up to 54 bytes
* Layer violations when the gains too great to resist: NAT
* Layer violations when network doesn’t trust ends:

Hosts: all layers, application runs on end hosts.
Routers: bottom three network layers.
Switches: bottom two layers.

# Key topics: Applications
what we do in the labs, write program that run on different end systems, hosts and clients.

## Principles
transport-layer service models

## Structure:

### client server paradigm

server: well defined, long-lived process that waits for requests, permanent IP address

client: short lived, do not communicate directly with each other, may have dynamic IP address

### peer to peer paradigm

* no always on server, no permanent ip
* arbitrary end systems(peers)
* symmetric responsibility

## TCP vs UDP

Reliable transport: TCP
Flow control: TCP
Congestion control: TCP
Timing minimum throughput guarantee:
Connection oriented: steup required between clients and server processes.

typical application using UDP: Streaming multimedia, internet telephone

## Web/HTTP
HTTP is stateless, process of data transmission
* client initiates TCP connection
* server accepts TPC connection from client
* HTTP messge exchanged between browser and web server
* TCP connection closed

Question, why do we need to include the host in the request? because a server can have multiple sites, for example we need the host to get the correct index page.

## Cookie


## E-mail (SMTP only)

## persistent vs non persistent: http 1.1 vs 1.0

In non-persistent HTTP, every object is downloaded over a fresh TCP connection. Since parallel connections are not supported, this would mean the ten objects are fetched serially. The time required to fetch one object = N * (time to setup TCP connection + RTT for sending GET request and receiving response + time to tear down TCP connection).

In persistent HTTP, if not using pipeline, all objects can be fetched over one single TCP connection but serially (one after the other). Thus the total time = time to setup TCP connection + N x (RTT for sending GET request and receiving the object) + time to tear down TCP connection.

If using pipeline, since pipelining is used once the index page is fetched and the client knows of the 9 embedded objects, these 9 objects can be requested back-to-back (simultaneously) and the corresponding objects would also be received back-to-back. Thus the total time = time to setup TCP connection + RTT for sending GET request for the index page and receiving that page + RTT for sending 9 GET requests for embedded objects and receiving them + time to tear down TCP connection.



DNS


P2P, BitTorrent, DHT


Video Streaming and CDN





# Key topics: Transport Layer

Principles



Sockets (Multiplexing/Demultiplexing)



## DNS records RR format: (name, value, type, ttl)

type A:         hostname, ip address, A, days
type NS:        domain, hostname of authoritative name server, NS, days
type MX:        name, value is name of mailserver associated with name
type CNAME:     alias name, canonical name

**NS records are stored in the TLD, their corresponding IP address(A records) are also stored in the TLD.**



UDP


Reliable Data Transfer concepts: RDT (1.0 to 3.0),



GBN, SR


# TCP: Everything that we covered


Excluded
• QUIC
• Complex checksum computations
• Congestion Control: Principles and TCP specifics (we
leave this for this final exam)
