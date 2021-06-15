# Week 2 Lecture 1

## Throughput (leftover from week 1)

### Definition of throughput
    1. Throughput is a performance measure in addition to delay and packet loss.
    2. We consider throughput in the end to end system, not including routers/links etc.
    3. Instantaneous throughput vs. average throughput.

### Bottleneck Link
    Can a link with a high transmission rate be the bottleneck link for a file transfer system? The answer is yes. A simplified example has been given in the textbook where we have 10 servers and 10 clients connected to the core of the computer network. Suppoer Rs = 2 Mbps, Rc = 1 Mbps, and R = 5 Mbps, the common link divides its transmission rate equally among the 10 downloads, then the bottleneck for each download is no longer in the access network but in the common link in the core. So the end-to-end throughput is the min{Rs, Rc, R/n}.

## Protocol layers (notion of service model)

### Three networking design steps
    1. break down the problem into tasks
    2. organise these tasks
    3. decide who does what

### Key tasks in computer networking (**layers**)

- prepare data(application)
- ensure that packets get to the destination process(transport)
- deliver packets across global network(network)
- deliver packets within local network to next hop(datalink)
- bits/packets on wire(physical)
    
    Applications -> reliable (or unreliable) transport -> best effort global packet delivery (no gurantee) ->best effort local packet delivery -> physical transfer of bits   

### Internet protocol stack
    1. Application: supporting network applications -> FTP, SMTP, HTTP, Skype,...
    2. Transport: process-process data transfer -> TCP, UDP
    3. Network: routing of datagrams from src to dest -> IP
    4. Link: data transfer between neighboring network elements -> ethernet, 802.III(wifi), PPP
    5. Physical: wire

    - each layer depends on layer below 
    - supports layer above
    - independent of others

#### benefits and drawbacks of layering
benefits
- layering provides a structured way to discuss system components
- it prevetns technology in one layer from affecting other layers
-  Modularity makes it easier to update system components, for example, the transport and network layers provide an intermediate part between application and transmission media, so whenever we develop an app, we just use api and make existing applications compable, easier to introduce new protocols

drawbacks
-  one layer may duplicate lower-layer functionality, for example, we can recover packet loss from link layer and transport layer.
- headers start to get large, each layer has header, TCP+IP+Ethernet headers = 54 bytes
- layer violations when the gains too great to resist, NAT
- layer violations when network doesn't trust ends, Firewalls

### Distributing layers
- hosts runs all layers
- routers only run network layer, link layer and physical layer
- switches only run link layer and physical layer


