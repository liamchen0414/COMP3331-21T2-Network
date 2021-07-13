# Transport Layer TCP

## 3.5 connection-oriented transport: TCP


### flow control
ensure the sender doesn't overrun the receiver.
1. the receiver uses receiver window in the TCP header to tell sender the current size of the free buffer window size.
2. what if the rwnd is filled up? sender would stop sending data. sender keeps sending TCP segments with one data byte to the receiver (minimum data). These segments are dropped but ACK by the receiver with a zero-window size. Eventually when the buffer is freed up, data transfer can begin again.

### connection management(connection establish and teardown: handshaking)
Socket clientSocket = new Socket("hostname","port number");
Socket connectionSocket = welcomeSocket.accept();
TCP 3-way handshake

**What if the SYN packet gets lost?**
sender sets a timer and waits for the SYN-ACK
...what's the value of our timer? most implementation use between 1 to 3 seconds

**teardown process**
exchange of control packets: FIN bit
1. Normal termination, sends 1 FIN bit

### RESET
A sends a RESET (RST) to B E.g., because application process on A crashed

1. B does not ack the RST
2. Thus, RST is not delivered reliably
3. And: any data in flight is lost
4. But: if B sends anything more, will elicit another RST

**segment lifetime**
ttl

## principles of congestion control
congestion:
1. Increase delay
2. Increase loss rate
3. Increase retransmissions, many unnecessary

### Cost of congestion
* knee: point after which throughput increases slowly, delay increases fast
* cliff: throughputs starts to drop to zero(congestion collapse), delay approaches infinity

## TCP congestion control
how sender detects congestion: infer loss
1. duplicated ACKs, fast retransmission
2. timeout: not enough dup ACKs.

how sender detects traffic goes back to normal
1. Upon receipt of ACK (of new data): increase rate
2. Upon detection of loss: decrease rate

### end to end congestion control
TCP sending rate: congestion window/RTT, we can reduce the window, hence reduce the number of packets sent in the window (RWND)

sender-side window = minimum{CWND,RWND}
window = multiple MSS.

1. First phase: TCP slow start(bandwidth discovery): when connection begins, incrase rate exponentially until first loss event
2. Adjusting to varying bandwidth: Congestion Avoidance (CA). TCP uses: “Additive Increase Multiplicative Decrease” (AIMD)

additive increase: increase cwnd by 1 MSS every RTT until loss detected
cwnd = cwnd +1, Simple implementation: for each ACK, cwnd = cwnd + 1/cwnd
multiplicative decrease: cut cwnd in half after loss

## Implementation
**ACK**
if: cwnd < slow start threshold, cwnd = cwnd +1(slow start)
else: cwnd = cwnd + 1/cwnd, Congestion Avoidance (CA)
**dupACK**
dupACKcounter++
if dupACKcounter = 3
slow start threshold <- cwnd/2
cwnd = cwnd/2
**Timeout**
slow start threshold <- cwnd/2
cwnd = 1 MSS