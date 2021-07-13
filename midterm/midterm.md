Information of the authoritative name servers of a hostname is stored in the TLD servers so that when a local DNS server queries for the mapping for the hostname, they can be directed to the authoritative name servers which would contain the actual answer (i.e. the mapping requested). Thus, the NS records for the Google name servers (primary and secondary) would be stored in the .com TLD server. In addition, the corresponding IP addresses of these name servers (i.e. the A records) would also be stored in the .com TLD server. 

The MX record for the gmail mail server and the corresponding A record that holds the IP address for this server would be stored at the authoritative name servers (i.e. ns1.google.com and ns2.google.com). 

transport layer may be able to provide reliability by using its own mechanism, despite working over an unreliable network layer(TRUE)
IP is an unreliable and connection less protocol.