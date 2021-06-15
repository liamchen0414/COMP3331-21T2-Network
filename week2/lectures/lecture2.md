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



I just love **bold text**.
Italicized text is the *cat's meow*.
This text is ***really important***.

> Dorothy followed her through many of the beautiful rooms in her castle.
>
> The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.

> Dorothy followed her through many of the beautiful rooms in her castle.
>
>> The Witch bade her clean the pots and kettles and sweep the floor and keep the fire fed with wood.

> #### The quarterly results look great!
>
> - Revenue was off the chart.
> - Profits were higher than ever.
>
>  *Everything* is going according to **plan**.

1. First item
2. Second item
3. Third item
4. Fourth item

- First item
- Second item
- Third item
- Fourth item
