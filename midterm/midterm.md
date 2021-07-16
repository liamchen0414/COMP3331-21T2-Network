Quiz 5

Q1. TCP receiver may intentionally delay the acknowledgement of a correctly received packet.
This is **True**. This is what we called the delayed ACK. Wait up to 500ms for next segment. If no next segment, send ACK

Q2.A TCP receiver receives an in-order segment with expected sequence number, but it has one other segment with pending ACK. Which of the following is a possible action for this receiver if it is using the delayed ACK mechanism?
The TCP receiver will send the cumulative ACK for both segments

Q3. TCP is never allowed to retransmit unless there is a timeout.
This is **False**. TCP uses the notion of fast retransmission after sender receive triple duplicate ACK. We use 3 because reordering only by 1 packet would cause an unnecessary retransmission.


Q4. During slow start, congestion window increases:
Exponentially

Q5 . Maximum segment size (MSS) refers to the number of bytes in a TCP segment including its header.
This is **False**. MSS = MTU - IP HEADER - TCP HEADER

Q6. A TCP connection is using an MSS=1460 bytes. At the start of slow start, how many bytes the TCP sender can transmit without having to wait for ACK?
Slow start, start with 1 MSS = 1460 bytes

Q7. A TCP sender could still reduce its window size even if there was no triple duplicate ACK or timeout.
This is **True**. TCP not only has congestion control but also flow control. In the header, TCP response tells the sender the size of the receiver window, if the window = 0, sender would send TCP segments with one data byte to the receiver.


Quiz 4
Q1. A transport layer protocol implements timer to address the loss problem. The timer cannot expire if there is no loss.
This is **False**. When ACK is delayed, timer will expire.

Q2. A reliable transport protocol must implement both ACK and NAK if it wants to address bit errors as well as packet loss problems.
This is **False**. rdt 2.2 don't use NACK

Q3. Stop-and-Wait: sends one packet and wait for the receiver to response
A. receiver buffers packets
**B. has only 1 bit for the sequence number**
C. requires a large sequence number space
D. requires more than 1 bit for the sequence number

Q3. Stop-and-Wait cannot provide reliability. True or False ?
This is **False**.

Q5 . For short distances, Stop-and-Wait is always efficient, but it fails to support high throughput only when the distance between the client and server is large. True or False?
This is **False**.

throughput: rate (bits/time unit) at which bits transferred between sender/receiver