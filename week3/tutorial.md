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
