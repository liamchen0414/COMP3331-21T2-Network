# Week 2 Lecture 2

## Application layer

### transport layer service model

### What transport layer provides us


### How to structure the application

client-server paradigm vs. p2p paradigm

1. client-server paradigm
server:
    - export well-defined requests
    - long-lived process that waits for requests
    - upon receiving request, carries it out

characteristics

    1. server is always on host
    2. it has permanent ip address
    3. static port convention
    4. data centres for scaling
    5. may communicate with other servers to respond

clients: 
    - short-lived process that makes requests
    - user side of application
    - initiates the communication

characteristics

    1. may be intermittently connected
    2. may have dynamic ip address
    3. do not communicate directly with each other

2. p2p paradigm

- no always-on server
- arbitrary end systems(peers) directly communicate
- symmetric responsibility
- **self-scalability**

    ** usage: file sharing, games, blockchain, video distribution, other distributed systems.

pros and cons

peer request service from other peers, provide service in return to other peers
speed
reliability
geographic distribution

#### TCP or UDP
factors to consider
1. data integrity: Reliable Data Transfer, we want to make sure everything is delivered reliably. This may be acceptable for loss-tolerant applications, most notably multimedia applications such as conversational audio/video that can tolerate some amount of data loss.

2. throughput: Applications that have throughput requirements are said to be bandwidth-sensitive applications, some applications require minimum throughput e.g. multimedia application, gaming

3. timing sensitive: e.g. multimedia application, gaming
4. security: not covered, neither TCP or UDP provides this

TCP service
reliable data transfer
flow control
congestion control
does not provide
connection-oriented

UDP service:
unreliable data transfer
does not provide: reliability, flow control, congestion control, timing, throughput guarantee, security, or connection setup

Question: why bother, why there is a UDP?
Some applications can take advantage of this lightweight transport protocol service. DNS for example.
### popular application-level protocols

- HTTP
- SMTP/POP3/IMAP
- DNS

### creating network applications
The fore of the network application development is writing programs that run on different end systems and communicate with each other over the network

write programs that
- run on various end systems
- communicate over network
- we don't need to write code that runs on network core devices such as routers or link-layer switches

#### socket API
process sends and receives messages to and from its socket
socket
- sending process shoves message out through the door
- sending process relies on transport infrastructure on other side of door to deliver message to socket at receiving process

socket is between application and transport layer. The process in application layer is controlled by app developer. The other four layers are controlled by the OS.

#### addressing processes
The process is the identifier. Host device has unique 32-bit ip address (ipv4). And because ip address of host on which process runs is not suffice for identifying the process, we need to include both ip address and port numbers associated with process on host.

for example, the HTTP server runs on port 80 and mail server runs on port 25

### HTTP
Hypertext Transfer Protocol (HTTP) is the web’s application layer. It is based on a client/server architecture, uses TCP and is **stateless**.
HTTP is all text, which makes the protocol simple to read, although is not the most efficient.

**HTTP Request**

	GET /index.html HTTP/1.1\r\n
	Host: www-net.cs.umass.edu\r\n
	User-Agent: Firefox/3.6.10\r\n
	Accept: text/html,application/xhtml+xml\r\n 
	Accept-Language: en-us,en;q=0.5\r\n 
	Accept-Encoding: gzip,deflate\r\n 
	Accept-Charset: ISO-8859-1,utf-8;q=0.7\r\n 
	Keep-Alive: 115\r\n
	Connection: keep-alive\r\n
	\r\n

**HTTP Response**

    HTTP/1.1 200 OK\r\n
    Date: Sun, 26 Sep 2010 20:09:20 GMT\r\n 
    Server: Apache/2.0.52 (CentOS)\r\n 
    Last-Modified: Tue, 30 Oct 2007 17:00:02 GMT\r\n
    ETag: "17dc6-a5c-bf716880"\r\n 
    Accept-Ranges: bytes\r\n
    Content-Length: 2652\r\n
    Keep-Alive: timeout=10, max=100\r\n 
    Connection: Keep-Alive\r\n
    Content-Type: text/html; charset=ISO-8859-1\r\n
    \r\n

#### HTTP response status codes

    200 OK
request succeeded, requested object later in this msg

    301 Moved Permanently
requested object moved, new location specified later in this msg (Location:)

    400 Bad Request
request msg not understood by server

    404 Not Found
requested document not found on this server


#### Verbs
HTTP consists of multiple verbs indicating to the server what type of request was sent. These include 
HTTP/1.0 GET, POST, HEAD, 
HTTP/1.1 PUT, DELETE, TRACE, OPTIONS, CONNECT and PATCH

### Cookie
many Web sites use cookies
four components:
1) cookie header line of HTTP response message
2) cookie header line in next HTTP request message
3) cookie file kept on user’s host, managed by user’s browser
4) back-end database at Web site

## Performance of HTTP

Page Load Time
- from click until user sees page
- key measure of web performance

How to improve PLT
1) Reduce content size
2) change HTTP to make better use of available bandwidth (persistent and pipelining)
3) Change HTTP to avoid repeated transfer of the same content (Caching and web proxies)
4) Move content closer to the client (CDNs)

Depends on many factors
- page content/structure
- protocols involved
- network bandwidth and RTT

### non-persistent HTTP vs. persistent HTTP

| non-persistent HTTP                         	| persistent HTTP                                            	|
|---------------------------------------------	|------------------------------------------------------------	|
| requires 2 RTT per object                   	| server leaves connection open after sending response    	|
| OS overhead for each TCP connection         	| subsequent HTTP request/response sent over the same TCP open connection 	|
| browser often initiate parallel connections 	| one RTT for all referenced objects                         	|

RTT (definition): time for a small packet to travel from client to server and back

* HTTP response time:
 1. one RTT to initiate TCP connection
 2. one RTT for HTTP request and first few bytes of HTTP response to return
 3. file transmission time

non-persistent HTTP response time = 2RTT + file transmission time

persistent HTTP:
    * no pipelining: clients issue new requests only when previous responses have been received, one RTT for each referenced object
    * pipelining: clients send requests as soon as it encounters a referenced object, as little as one RTT for all the referenced object, NOTE: ***you always need 1 RTT to fetch the index page***.

### improving HTTP performance
1. concurrent requests and responses
    * use multiple connections in parallel
    * does not necessarily maintain order of response
    drawbacks: can overload the server. Server has to keep state and manage state.
2. persistent HTTP
3. Caching: not browser, but web cache. This is also called proxy server. The Web cache has its own disk storage and keeps copies of recently requested objects in this storage. Typically a Web cache is purchased and installed by an ISP. For example, a university might install a cache on its campus network and configure all of the campus browsers to point to the cache.
4. CDN, caching and replication as a service (discussed in DNS)

### Caching calculation (exam question)
hit rate: repeated contents
for example, cache hit rate is 0.4
access link utilization = 60% of requests use access link
this is better than just increase the bottleneck access link, cache is much cheaper than increasing the access link.

### Issues with HTTP
* security
* head of line blocking: slow objects delay later requests
* browsers often open multiple TCP connections for parallel transfers
* HTTP headers are big
* Objects have dependencies, different priorities

