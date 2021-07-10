# Transport Layer

## 3.4 principles of reliable data transfer
TCP: keep the information at the end point
UDP: doesn't keep any information

**Reliable data transfer**

### rdt2.0: channel with bit errors
Assumption: underlying channel may flip bits in packet, checksum to detect bit errors. 

**The question: how do we recover from errors:**
1. ACK: receiver explicitly tells sender that pkt received ok
2. NACK: receiver explicitly tells sender that pkt had errors
3. sender retransmits pkt on receipt of negative acknowledgement

note: dotted line: erroneous transmission

Issue with rdt2.0: we only assume pkts in forward direction is corrupted. But in reality the pkt coming back can be corrupted as well.

### rdt2.1: erroneous protocol (erroneous)
measure: seq # added to pkt + checksum like UDP(Receiver checks if data is corrupted/duplicated, sender checks if NACK, ACK is corrupted), seq # can also be used to check duplicates. 

/*stop and wait protocol*/
sender:
* seq # added to pkt
* two seq.#'s (0,1) will suffice. why? **The sender is only sending one pkt at given time, from the receiver's perspective, he or she only needs to check if the new pkt is a duplicate of the previous pkt**
* must check if received ACK/NAK corrupted
* twice as much state

Receiver:
* must check if received packet is duplicate
* receiver can not know if its last ACK/NAK received OK at sender


### rdt2.2: a nak-free protocol (erroneous)

We don't need two types control packets, we don't need NAK. This is what TCP does. We use seq# of pkt to indicate NAK

sender sends pkt0, receiver sends ack0. data1 is erroneous, receiver sends ack0 again, and sender knows pkt1 is not sent correctly. Note: be careful with the first packet data(0) being erroneous, we need some special handling/exception for this.

### rdt3.0: implementation of timer (packet loss)
Check the graph example

new assumption: underlying channel can also lose packets, we still have our tools, checksum, seq#, ACKs, retransmissions, etc

**Solution**
sender waits reasonable amount of time for ACK,
* retransmits if no ACK received in this time
* if pkt just delayed(not lost, therefore duplicates):
    1 retransmission will be duplicate, but seq number already handles this
    2 receiver must specify seq number of pkt being ACKed
* require countdown timer
* no retransmission on duplicate ACKs

**scenarios**
* data is lost
* ack is lost
* timeout premature

Utilization_sender = (L/R)/(RTT+L/R)

### rdt3.0: pipelining: increased utilization
#### go back N
1. sender can have up to n unack packets in pipeline
2. sender has single timer for oldest unack packet, when timer expires, retransmit all unacked packets
3. there is no buffer available at receiver, out of order packets are discarded
4. receiver only sends cumulative ack, doesn't ack new packet if there is a gap


#### selective repeat
1. sender can have up to n unack packets in pipeline
2. sender can have up to N unacked packets in pipeline
sender maintains timer for each unack packet, when timer expires, retransmit only that packet
3. receiver has buffer, can accept out of order packet
4. receiver sends individual ack

sender
* data from above, if next available seq number in window, send pkt
* timeout(n), resend pkt n, restart timer
* ACK(n) in [sendbase, sendbase+N];

receiver
* send ACK(n)
* out of order: buffer
* in order: deliver, advance window to next not-yet-received pkt

**ISSUE WITH SELECTIVE REPEAT**
Q: window size <= 1/2 of sequence number space

3.5


3.6


3.7