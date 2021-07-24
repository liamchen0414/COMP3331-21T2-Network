# Simple-Transport-Protocol #

### Project Description ###
The simple transport protocol project aims to transmit data using UDP while implementing the reliable transport features of TCP. The project consists of sender and reciever components that allow for reliable unidirectional data transfer. The reliable transport features include sequence numbers, acknowledgments, three-way handshake, four-segment connection termination, buffering for out of order packets, timeout and fast-retransmit.

----
### Demo ###
To demo the project simply type:

`$ sh run.txt`

After the program has been run, the created files _Sender_log.txt_, _Receiver_log.txt_ and _output.txt_ can be removed by typing:

`$ sh clean.txt`

The arguments of each program can be modified within the run.txt file. A description of the arguments is provided below

`$ python3 receiver.py receiver_port output_file` \
`$ python3 sender.py receiver_host_ip receiver_port input_file MWS MSS timeout pdrop seed`

Notes:
MWS -> maximum window size (bytes) \
MSS -> maximimum segment size (bytes) \
timeout -> the value of timeout after which a lost packet will be retransmitted (milliseconds) \
pdrop -> probability that a given packet will be dropped (0 ≤ pdrop ≤ 1)

----
### Output ###
As well as the output file produced by the receiver which is identical to the input file at the sender. The sender and receiver also produce their own _Sender_log.txt_ and _Receiver_log.txt_ files respectively. The contents of these files provide information about each segment that it sends, drops and receives. The format of these files is as follows:

snd/rcv/drop   | time          | type of packet| seq-number    | number-of-bytes | ack-number
-------------  | ------------- | ------------- | ------------- | -------------   | -------------
snd            | 0.10          | S             | 0             | 0               | 0
rcv            | 0.55          | SA            | 0             | 0               | 1
snd            | 1.41          | A             | 1             | 0               | 1
snd            | 2.25          | D             | 1             | 100             | 1
snd            | 3.32          | D             | 101           | 100             | 1
drop           | 4.74          | D             | 201           | 100             | 1
rcv            | 6.01          | A             | 1             | 0               | 101
drop           | 7.53          | D             | 301           | 100             | 1
rcv            | 8.01          | A             | 1             | 0               | 201
snd            | 9.78          | D             | 401           | 100             | 1
rcv            | 10.71         | A             | 1             | 0               | 201
drop           | 1004.81       | D             | 201           | 100             | 1
snd            | 2005.33       | D             | 201           | 100             | 1
rcv            | 2006.61       | A             | 1             | 0               | 301
drop           | 2006.83       | D             | 501           | 100             | 1
drop           | 2052.48       | D             | 301           | 100             | 1
snd            | 3055.74       | D             | 301           | 100             | 1
rcv            | 3104.72       | A             | 1             | 0               | 501
 ............. | ............. | ............. | ............. | ............... | .............
 
The data in the table above was produced in _Sender_log.txt_ after running the program with MSS = 100, MWS = 300, timeout = 1000 and pdrop = 0.5. Similar output is produced at the receiver end in _Receiver_log.txt_. Statistical information can also be found at the bottom of these two files such as amount of data transferred, number of data segments sent, number of packets dropped, number of retransmitted segments and number of duplicate acknowledgements received.
