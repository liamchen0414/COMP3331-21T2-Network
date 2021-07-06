# TCP

## Overview
* point to point
* reliable, in-order byte stream: no "message boundaries", all in fixed size data packet
* pipelined: TCP congestion and flow control set window size
* send and receive buffers: TCP makes sure that buffer is not overflowed
* full duplex data:
* connection-oriented
* flow controlled

## TCP segment structure
32 bits
* source port #, dest port #
* sequence number
* ack number
* header length, receive window
* checksum, urg data pointer
* options(variable length)
* application data(variable length)

    U: URG: urgent data, not used anymore\
    A: ACK # valid, 1 is valid, 0 is ignored\
    P: PSH: push data now, not used anymore\
    R: RST: see lecture 2\
    S: SYN: see lecture 2\
    F: FIN: see lecture 2\

## TCP Stream of Bytes Service
TCP numbers every byte in TCP, there are two ways segments are sent. The first one is when segment is full, the second one is when the application pushes one segment to be sent, for example, telnets sends one byte data.

## What exactly is the maximum segment size
MTU is determined by the link layer. IP Datagram + IP header(20 bytes minimum). IP Datagram = TCP Data + TCP header(20 bytes minimum). 

**MSS = MTU - TCP HEADER - IP HEADER**

## Sequence number
ISN(initial sequence number)
Sequence number = 1st byte in segment = ISN + k

## ACK sequence number
ACK Sequence Number = next expected byte = seqno + length(data)

## Example
h is seq_0, ack = seqno + length(data) = 0 + 5 = 5, so the ACK sent back by Bob is 5. The seq sends by Bob is the first byte in segment which is 10. for the second message

# What does TCP do?
Most of our previous tricks, but a few differences
* Checksum
* Sequence numbers are byte offsets
* Receiver sends cumulative acknowledgements (like GBN)
* Receivers can buffer out-of-sequence packets (like SR)
* Sender maintains a single retransmission timer (like GBN) and
retransmits on timeout (how much?)


## TCP round trip time, timeout time
Q: how to set TCP timeout value?
Q: how to estimate RTT?

We take sample RTT and average several recent measurements.
EstimatedRTT = (1-a) * EstimatedRTT + a * SampleRTT
typical value a = 0.125