# Domain name system(DNS)

## Graph
hierarchical naming system for devices connected to the internet
DNS is a directionary that maps human-readable domain names to computer-readable information(IPV4 IPV6)

1. The query is sent to the DNS resolver, which is provided by your ISP. This is the first gateway of DNS query
2. DNS resolver -> DNS root name server(globally distributed), DNS root name server will forward the name server for .com TLD -> DNS resolver
3. DNS resolver knows where to forward the query next -> Name server for .com TLD, Name server for .com TLD forward the IP address query for authorative name server -> DNS resolver
4. DNS resolver forward the query -> the Authorative name server, the Authorative name server has the IP address of the query. the Authorative name server -> sends mapping back to DNS resolver
5. DNS resolver -> end user with ip address for the website.

## DNS Records:
* A:(address record) -- maps hostnames to IPv4 address, IPv6(AAAA)
* CNAME: (canonical name record) -- maps alises to a real/cononical name, without CNAME, a website may have ftp server, mail server, we need to configure new IP addresses for all services. With CNAME, we can assign different services name to a common alias name. When we move our service to a new IP address, we only need to configure the IP address once.
* MX: (Mail exchange record) -- main server for the domain
* NS: (Name Server Record) -- name server responsible for the domain
* PTR: (Pointer Resource Record) -- used for reverse DNS lookups
* SOA: (Start of Authority Record) --  who is in charge of the administration for the domain 

    dig -x 138.44.5.0 vs. nslookup 138.44.5.0
    YourDomain.com IN SOA ns1.NameServer.com webmaster.YourDomain.com

http/tcp transport layer port: 80
dns/udp transport layer port: 53

# Application layer questions
1. Why is SMTP not used for transferring e-mail messages from the recipient’s mail server to the recipient’s personal computer?

Because SMTP is a push protocol, the task of transferring email from the mail server to the client is a pull operation

2. Why do you think DNS uses UDP, instead of TCP, for its query and response
messages?

Because UDP doesn't require the establish of connection every time there is a query and response. Using TCP for DNS may end up involving several TCP connections to be established since several name servers may have to be contacted to translate a name into an IP address. This imposes a high overhead in delay that is acceptable for larger transfers but not acceptable for very short messages such as DNS queries and responses. In addition, UDP affords a smaller packet size and also imposes a smaller load on name servers due to its simplicity in comparison to TCP. 

3. Suppose you are sending an email from your Hotmail account to your friend, who reads his/her e-mail from his/her mail server using IMAP. Briefly describe how your email travels from your host to your friend’s host. Also, what are the application-layer protocols involved?

First, my email is sent from the host to the Hotmail server using HTTP. Then Hotmail server sends the email to my friend's mail server using SMTP. Then my friend is going to transfer e-mail from his/her mail server to his/her host using IMAP.

4. How can iterated DNS queries improve the overall performance? 
Iterated request can improve overall performance by offloading the processing of requests from root and TLD servers to local servers. In recursive queries, root servers can be tied up ensuring the completion of numerous requests, which can result in a substantial decrease in performance. Iterated requests move that burden to local servers, and distributed the load more evenly throughout the Internet. With less work at the root servers, they can perform much faster. Some requests can be resolved at local DNS server.

5. Consider the circular DHT example that we discussed in the lecture. Explain how peer 6 would join the DHT assuming that peer 15 is the designated contact peer for the DHT

* Peer 6 will contact Peer 15 with a join request. 
* Peer 15, whose successor is peer 1, knows that Peer 6 should not be its successor. Peer 15 will forward the join request from Peer 6 to Peer 1.
* Peer 1, whose successor is peer 3, knows that Peer 6 should not be its successor. Peer 1 will forward the join request from Peer 6 to Peer 3. The actions of peers 3 and 4 are identical to those of peers 15 and 1.
* The join request will finally arrive at peer 5. Peer 5 knows that its current successor is peer 8, therefore peer 6 should become its new successor. Peer 5 will let peer 6 knows that its successor is peer 8. At the same time, peer 5 updates its successor to be peer 6.

6. Consider a new peer Alice that joins BitTorrent without possessing any chunks. Without any chunks, she cannot become a top-four uploader for any of the peers, since she has nothing to upload. How then will Alice get her first chunk? 

“optimistic unchoke"