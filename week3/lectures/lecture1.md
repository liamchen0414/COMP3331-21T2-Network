# Week 3 Lecture 1

## Electronic Mail: mail servers
* mailbox: contains incoming messages for user
* message queue of outgoing mail messages
* SMTP protocol between mail servers to send emails message
* port 25

**Three major components**
* user agents: mail reader, e.g. outlook, thunderbird
* mail servers
* simple mail transfer protocol: SMTP

user A sends message to user B: \
user agent A -> mail server -(TCP)-> mail server <- user agent B invoked to read message

### Similarity and Difference
* persistent connections
* SMTP requires message to be in ASCII
* SMTP uses crlf to determine end of message

HTTP uses pull, SMTP uses push \
HTTP each object is encapsulated, SMTP multiple objects are sent in multipart msg.

Question: why do we need sender's mail server?
To ensure that the mail can always be delivered even when the receiver server is temporary unavailable.

Question: why do we need receiver's mail server?
Because the user agent is not always online, the user may want to retrieve a file in later time. If a user's machine is not online, we still want to deliver the mail.

## P2P applications
time to distribute F to N clients using client-server approach
Dc-s >= max{upload time, download time} = max{NF/U_server, F/d_min} where N is the number of clients, F is the file size, U_server is the minimum speed of the server, d_min is the min client download rate.

time to distribute F to N clients using p2p approach
Dp2p >= max{server upload time for one copy, download time} = max{F/U_server, F/d_min, NF/(U_server+U_i)} where i is from 1 to N

bitTorrent: how does it work
* file divided into 256 KB chunks
* peers in torrent send/receive file chunks
* tracker: tracks peers participating in torrent
* torrent: group of peers exchanging chunks of a file

When a new peer arrive, the tracker gives her the .torrent file, which has a small subset of peers(their IP addresses and a list of file chunks and their cryptographic hashes. While downloading, peer uploads chunks to other peers, peer may change peers with whom it exchanges chunks.

1. Requesting chunks: at any given time, different peers have different subsets of file chunks. We want to request the rarest first. Because we want to balance the distribution of the chunks among the neighborhood. 
2. Sending chunks *tit for tat*: We try to match what we are receiving. We send chunks to those currently sending us at highest rate. 
3. How is new peer joining? every 30 seconds, every 30 seconds, randomly select another peer, starts sending chunks "optimistically unchoke"



## CDNs



## socket programming with UDP and TCP