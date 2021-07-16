# UPT-simple-transport-protocol-to-simulate-TCP

Project Description

We have implemented a reliable transport protocol over the UDP protocol. We refer to the reliable transport protocol as Simple Transport Protocol (STP). STP will include most (but not all) of the features that are described in TCP. Examples of these features include timeout, ACK, sequence number etc. Note that these features are commonly found in many transport protocols.  

we have implemented Simple Transport Protocol (STP), a piece of software that consists of a sender and receiver component that allows reliable unidirectional data transfer. STP includes some of the features of the TCP protocols.

STP has two separate programs: Sender and Receiver. we only have to implement unidirectional transfer of data from the Sender to the Receiver. Data segments will flow from Sender to Receiver while ACK segments will flow from Receiver to Sender. We are not using TCP sockets directly.

The STP protocol is implemented pretty similar to what TCP looks like with the following features and implementation:

The sender initiates the TCP connection by three-way handshake with SYN = 1, the receiver, after receiving the TCP, sends reply to the sender with SYN = 1 and ACK = 1. The sender then sends an packet with ACK = 1 and starts sending data and receiver after receiving this packet will start receiving data correspondingly. There are two timers on both sides in case these packets get lost, which will cause retransmission of the lost packets. The sender time will be set be the parameter and the timer for receiver is set to 500 milli seconds empirically. The client_isn and server_isn are randomly chosen with the maximum length of 10 and its modification is following the TCP.

During the data transmission process, the sequence number of sender will start from 0 with the timer set by the user. The sequence number s are bye offsets starting from 0 and increases by MMS except for the last packet which might be less than MSS.

Sender maintains a single timer for the first unacknowledged packet in the current window. If this packet is acknowledged, the window represented be ‘SendBase’ moves to the next unacknowledged packet and if time-out is triggered, such packet will be retransmitted.

When sender receives any ack that is no more than the current window base(the first unacknowledged packet with the least sequence number), sender dose nothing and keep timing as all these acks were received before.

To simplify the packet, all the acknowledgement field of sender’s packet will be set to 0 since they are useless in the reliable STP.
Receiver sends cumulative acknowledgements back to sender with acknowledge number of the next expected byte.

When receiver receives the seq which is equal to the next wanted packet, the receiver will write the data in the file and move the current receiving buffer by the size of one packet to the right to make a new space for the buffer.

Receiver also keeps the out-of-sequence packets in a buffer, when the sequence number of the current received packet is larger than the current ack. If the gap is perfectly filled, the receiver will directly move to the next expected but not received byte and send an ack back to sender, otherwise, receiver will keep buffering data.

When the received packets have the sequence number no more than the current ack, receiver will simply drop the packet as the content of the packets has been received already and send the current ack again to sender to remind sender to send the correct and wanted packet.

All the ack packets sent by receiver have 0 in the sequence number of the packet as only ack field is useful in acknowledgement on sender’s side. Also, SYN, FIN and ACK flags are set to 0.

There is a fast-retransmission function  on sender side. When three replicated acks (making the total number of four for the same ack) are received consecutively, the sender will raise time-out exception immediately, which causes the retransmission of the unacknowledged packet with the least acknowledged packet. The fast retransmission will be executed for only one time.

PLD module is implemented with the drop rate set by the user. PLD will exempt packets used for connection and disconnection of STP but will drop data packet of first transmission and retransmission, which means all data packets will go through PLD module.

The window size is defined by MWS/MSS which represents stop-and-wait protocol when it is equal to 1.

Both sender and receiver record the packets of data in log files with the same format in the specification of this assignment.

When all data is transferred, sender will send a packet with FIN = 1 to receiver to initialises the disconnection and enter FIN_WAIT_1 phase. Receiver will send two packets with ACK = 1 (leading sender to enter FIN_WAIT_2) and FIN =1(leading sender to enter Time-wait) consecutively before closure.  Sender will send back an ACK segment after that and enter Time-wait period before close the connection fully.
In the closing state, all seq and ack numbers are set to 0.

MWS should always be larger than MSS otherwise the code will have ArrayIndexOutOfBoundsException.



