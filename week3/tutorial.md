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

1. why do we have more than one authoritative name server? This is for the load balance, if one name server is done, the website can't be resolved.

2. the additional records contain the ip address to reach any of the authoritative name server.


Question 3. What can you make of the rest of the response (i.e. the details available in the Authority and Additional sections)?

Question 4. What is the IP address of the local nameserver for your machine?

Question 5. What are the DNS nameservers for the “eecs.berkeley.edu.” domain (note: the domain name is eecs.berkeley.edu and not www.eecs.berkeley.edu . This is an example of what is referred to as the apex/naked domain)? Find out their IP addresses? What type of DNS query is sent to obtain this information?

Question 6. What is the DNS name associated with the IP address 111.68.101.54? What type of DNS query is sent to obtain this information?

Question 7. Run dig and query the CSE nameserver (129.94.242.33) for the mail servers for Yahoo! Mail (again the domain name is yahoo.com, not www.yahoo.com ). Did you get an authoritative answer? Why? (HINT: Just because a response contains information in the authoritative part of the DNS response message does not mean it came from an authoritative name server. You should examine the flags in the response to determine the answer)

Question 8. Repeat the above (i.e. Question 7) but use one of the nameservers obtained in Question 5. What is the result?
Question 9. Obtain the authoritative answer for the mail servers for Yahoo! Mail. What type of DNS query is sent to obtain this information?

Question 10. In this exercise, you simulate the iterative DNS query process to find the IP address of your machine (e.g. lyre00.cse.unsw.edu.au). If you are using VLAB Then find the IP address of one of the following: lyre00.cse.unsw.edu.au, lyre01.cse.unsw.edu.au, drum00.cse.unsw.edu.au or drum01.cse.unsw.edu.au. First, find the name server (query type NS) of the "." domain (root domain). Query this nameserver to find the authoritative name server for the "au." domain. Query this second server to find the authoritative nameserver for the "edu.au." domain. Now query this nameserver to find the authoritative nameserver for "unsw.edu.au". Next query the nameserver of unsw.edu.au to find the authoritative name server of cse.unsw.edu.au. Now query the nameserver of cse.unsw.edu.au to find the IP address of your host. How many DNS servers do you have to query to get the authoritative answer?

Question 11. Can one physical machine have several names and/or IP addresses associated with it?