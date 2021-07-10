# Week 3 Lecture 2

When fetching a website, your browser is almost always fetching the
index page first unless explicitly indicated otherwise

## DNS:domain name system
DNS query the system: what's the ip address, what's the host name, it is essentially a mapping service. Host name servers communicate to resolve names. Core internet function implemented as application layer protocol

## DNS services:
* hostname to IP address translation
* indirection
* host aliasing
* mail server aliasing
* load distribution

**why not centralize DNS**
* single point of failuer
* traffic volume
* distant centralised database
* maintenance
* doesn't scale

## Hierarchy

* top of hierarchy: root servers, .
* next level: top level domain server: TLD, .com .edu etc
* bootom level: Authoritative DNS servers, provides mapping for name hosts

The root server will never have the mapping information.
TLD doesn't have the mapping information either

**local name server**
* does not strictly belong to hierarchy, it is like cache and checked if it is stored in the local name server.
* each ISP has one
* hosts configured with local DNS server address or learn server via a host configuration protocol (DHCP)
* when host makes DNS query, it is sent to its local DNS server

### iterated query

host -> local DNS server
local DNS server <-> root DNS server
local DNS server <-> TLD DNS server
local DNS server <-> authoritative DNS server
local DNS server -> host


### recursive query
recursive query: put burden of name resolution on contacted name server
host -> local DNS server -> root DNS server -> TLD DNS server-> authoritative DNS server-> TLD DNS server-> root DNS server-> local DNS server-> host


## DNS records
inserting: type A and type MX into authoritative server

## why is DNS using UDP
because it is simple and lightweight. If using TCP, the local server needs to establish connection every time.