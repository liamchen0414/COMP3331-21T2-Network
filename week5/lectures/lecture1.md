# TCP, reliable data transfer

**Questions from previous lecture**
* how do the sender and receiver keep track of outstanding pipelined segments
* how many segments should be pipelined
* how do we choose sequence number
* what does connection establishment and teardown look like
* how should we choose timeout values

RFCs: 793,1122,1323,2018,2581

## Overview
* point to point, one sender, one receiver
* reliable, in-order byte stream: no "message boundaries", all in fixed size data packet
* pipelined: TCP congestion and flow control set window size
* send and receive buffers: TCP makes sure that buffer is not overflowed
* full duplex data: maximum segment size
* connection-oriented: handshaking inits sender, receiver state before data exchange
* flow controlled: sender will not overwhelm receiver, tell the sender buffer status

## TCP segment structure
32 bits
* source port #, dest port # (like UDP)
* sequence number
* acknowledge number
* header length, not used, UAPRSF, receive window(# bytes receiver is willing to accept)
* checksum(as in UDP), urg data pointer
* options(variable length)
* application data, payload(variable length)

    U: URG: urgent data, not used anymore\
    A: ACK # valid, 1 is valid, 0 is ignored\
    P: PSH: push data now, not used anymore\
    R: RST: see lecture 2(setup, teardown)\
    S: SYN: see lecture 2(setup, teardown)\
    F: FIN: see lecture 2(setup, teardown)\

TCP header is 20 bytes without option, UDP is 8 bytes

## rdt for TCP

* Checksums(for error detection)
* Timers(for loss detection)
* Acknowledgments(cumulative vs selective)
* Sequence numbers(duplicates, windows)
* Sliding Windows(GBD,SR)

### TCP Stream of Bytes Service
TCP numbers every byte in TCP, there are two ways segments are sent. 
* The first one is when segment is full.
* The second one is when the application pushes one segment to be sent, for example, telnets sends one byte data.

## What exactly is the maximum segment size
MTU(maximum transmission unit) is determined by the link layer. 
IP header(20 bytes minimum)
TCP header(20 bytes minimum). 
**MSS = MTU - TCP HEADER - IP HEADER**
MSS = MTU(1500 bytes) - 20 - 20 = 1460 for Ethernet

### Sequence number
ISN(initial sequence number)
Sequence number = 1st byte in segment = ISN + k, byte stream number of the first byte in the segment
For example, segment A is from byte 0 to byte 80, segment B is from byte 81 to 160, then the sequence number for A is 0, sequence number for B is 81.

**why random ISN**
1. avoids ambiguity with back to back connections between same end-point, if the connection is closed for some reason, then the application may open a new connection with the same end-point, then the older packet may end up with same number and be accepted by the new connection by the application.
2. potential security issue if the ISN is known

## ACK sequence number
ACK Sequence Number = **next expected byte** = seqno + length(data), ACK is always 1 greater than what's already received  

# What does TCP do?
Most of our previous tricks, but a few differences
* Checksum
* Sequence numbers are byte offsets
* Receiver sends **cumulative acknowledgements** (like GBN), everything up to ACK N has been correctly received
* Receivers can buffer out-of-sequence packets (like SR)
* Sender maintains a single retransmission timer (like GBN) and retransmits on timeout (how much?)

## TCP round trip time, timeout time
Q: how to set TCP timeout value?
Q: how to estimate RTT?

**we don't include retransmission in RTT computation**
Estimate the current RTT, and set the timeout. We take sample RTT and average several recent measurements.
EstimatedRTT = (1-a) * EstimatedRTT + a * SampleRTT, typical value a = 0.125

Timeout interval: safety margin
DevRTT = (1-b)* DevRTT + b * (SampleRTT - EstimatedRTT), typical value b = 0.25

Final TimeoutInterval = EstimatedRTT + 4*DevRTT

## sender
* data received from application
    1. create segment with seq #
    2. seq # is byte-stream number of first data byte in segment
    3. start timer if not already running
* timeout
    1. retransmit segment that caused timeout
    2. restart timer
* ack received
    1. if ack acknowledges previously unacked segments

## TCP fast retransmit
After receive 3 duplicate ACK, the sender retransmit.


**TCP retransmission scenarios**
1. lost ACK: ACK is lost, timeout, sender sends the packet again
2. premature timeout
3. cumulative ACK: back to back, didn't receive the first ACK but the second ACK, no need to retransmitt. **delayed ACK** wait up to 500ms for next segment, if no next segment, send ACK.don't worry it in exam
4. cumulative ACK: back to back, first packet loss, ACK is not updated. Sender retransmitt, because the second packet is in the buffer out of order by the receiver, ACK is second seq + data received.

