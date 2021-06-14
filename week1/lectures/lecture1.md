1 Introduction
1.1 What is the Internet?

View 1: An interconnection of different computer networks
View 2: An infrastructure that provides services to networked applications

1.2 network edge
network edge:
	hosts: clients and servers
	servers often in data centers

edge router: A router that connects the edge components to the 'core' routers

1.3 network core
circuit switching: FDM vs. TDM
issue with circuit switching: inefficeint, fixed data rate, connection maintenance

packet switching: 
data is sent as chunks of formatted bits(Packets)
packets consiste of a header and payload(address,age(TTL),checkSum to protect header)
Switches forward packets based on their headers
each packet travels independently

1.4


A handshake can only occur on a link which supports communication both way (not always at once) 'half-duplex' link at least,, 
but some forms of communication use only one way communication (like UDP, which is a broadcast (think megaphone to a crowd))