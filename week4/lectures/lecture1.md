# Transport Layer

## 3.1 transport-layer services
Transport layer provides logical communication between app processes running on different hosts. It runs in end systems. On the sender side, transport layer breaks app msgs into segments and passes them to network layer. On the receiver side, transport layer reassessembles segments into messages, passes to app layer.

**why do we need transport layer**
Transport layer manages msgs are sent between the correct processes.

**trarnsport layer protocoles**
UDP and TCP(connectionless transport vs connection-oriented reliable transport)

## 3.2 multiplexing and demultiplexing
multiplexing at sender: handle data from multiple sockets, *add transport header*(later used for demultiplexing)
demultiplexing: use header info to deliver received segments to correct socket

With UDP, the socket open for the connection is same as the socket for data transfer. With TCP, a welcome socket is open, when client connects to the server, a connection socket is used to transfer data. A TCP port can have multiple sockets for connections

## 3.3 UDP

### Segment header
source port, dest port, length, checksum,payload

**why UDP**
1. no connection establishment(which can add delay)
2. simple: no connection state at sender, receiver
3. small header
4. no congestion control: can blast away as fast as desired, you have some control over the speed

**checksum**
GOAL: detect "errors" in transmitted segment such as router memory errors, driver bugs, electromagnetic interference. It is the 16 bit one's complement of the one's complement sum of a pseudo header of information from IP header.

**Reliable data transfer**
a packet is corrupted
a packet is lost(router, switches use certain buffers that are overloaded, packets loss)
a packet is delayed(queued in buffer)
packets are reordered(internet doesn't guarantee every packet takes the same path)
packet is duplicated(retransmission)

## 3.4 principles of reliable data transfer(rdt)
TCP: keep the information at the end point
UDP: doesn't keep any information

we discuss this based on the channel below and starting relax our assumptions.

### rdt1.0: reliable transfer over a reliable channel, transport layer does nothing
* no bit errors
* no loss of packets

### rdt2.0: channel with bit errors
* underlying channel may flip bits in packet, check sum to detect bit errors
* to recover, we have to use ACKs and NAKs
* ACK: receiver explicitly tells sender that pkt received ok
* NAK: receiver explicitly tells sender that pkt had errors