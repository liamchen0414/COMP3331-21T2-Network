#!/usr/bin/python3

import socket
import sys
import time
from collections import deque

# Creates a header based on arguments.
def create_segment(seq, ack, flag, payload=''):
	return (seq + '|' + ack + '|' + flag + '|' + payload)

# Reads a segment
def read_segment(segment, seq):
	ack = str(int(segment[0]) + max(len(segment[3]), 1))
	if int(segment[1]):
		seq = str(int(segment[1]))
	flags = segment[2]
	return seq, ack, flags

# Appends packet payload to file
def write_to_file(segment):
	with open(file, 'a') as f:
		f.write(segment[3])

def get_time():
	return time.time()-start_time

# Formats and writes a line to log file.
def write_log_line(status, time, segment, log):
	flag_type = ''
	flag_list = ['F','S','A','D']
	for i in range(0,4):
		if segment[2][i] == '1':
			flag_type += flag_list[i]
	line = status + '\t' + format('%.3f' % (time*1000)) + '\t' + flag_type + \
		'\t' + segment[0] + '\t' + str(len(segment[3])) + '\t' + segment[1] + '\n'
	with open('Receiver_log.txt', 'a') as f:
		f.write(line)
# Get the time difference


# Main program starts here

# Checks argument
if len(sys.argv) != 3:
	print("Usage: python Receiver.py <port> <receivedfile.txt>")
	sys.exit()

# Assigns arguments
receiver_port = int(sys.argv[1])
file = sys.argv[2]

# Assigns other variables.
seq = '0'
ack = '0'
log = 'Receiver_log.txt'
listening = True

# Assigns variables for summary
data_received = 0
nData_seg = 0
nDup_Seg = 0

# Creates a receiver socket and set it to listen for a client.
receiverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
receiverSocket.bind(('', receiver_port))

# Opens log file and write
print('Receiver is ready')
with open(log, 'w') as f:
	f.write('')
start_time = time.time()

print('Initiating threeway handshake')
# 3way handshake
senderSegment, senderAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
seq, ack, flags = read_segment(senderSegment, seq)
write_log_line('rcv', get_time(), senderSegment, log) # write syn to log
# reply with synack
replySegment = create_segment(seq, ack, '0110')
receiverSocket.sendto(replySegment.encode(), senderAddress)
write_log_line('snd', get_time(), replySegment.split('|'), log)
# ack
senderSegment, senderAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
write_log_line('rcv', get_time(), senderSegment, log)

# receving file started
with open(file, 'w') as f:
	f.write('')
packet_buffer = deque()
while listening:
	print('File transfering......')
	# keep receiving data, write status to 
	senderSegment, senderAddress = receiverSocket.recvfrom(2048)
	senderSegment = senderSegment.decode().split('|')
	write_log_line('rcv', get_time(), senderSegment, log)
	print(data_received)
	if senderSegment[0] != ack:
		print('Out of order packet detected')
		if int(ack) < int(senderSegment[0]):
			# write it to log and packet_buffer and update data length received
			receiverSocket.sendto(replySegment.encode(), senderAddress)
			write_log_line('snd', get_time(), replySegment.split('|'), log)
			data_received += len(senderSegment[3])
			packet_buffer.append(senderSegment)
		else:
			nDup_Seg += 1 # Duplicate segment
		nData_seg += 1
	else:
		seq, ack, flags = read_segment(senderSegment, seq)
		if int(flags[3]):
			# if it has flag "D"
			# two cases
			# retransmission
			# in order packet
			nData_seg += 1
			write_to_file(senderSegment)
			data_received = data_received + len(senderSegment[3]) # add payload length to data received
			# if retransmission is trigered
			if packet_buffer:
				# while there is something in the packet_buffer, we pop it and write it to the file
				# we do it until there is no packet in the packet_buffer
				while packet_buffer[0][0] == ack and len(packet_buffer) > 1:
					next_header = packet_buffer.popleft()
					write_to_file(next_header)
					seq, ack, flags = read_segment(next_header, seq)
				replySegment = create_segment(seq, ack, '0010')
				receiverSocket.sendto(replySegment.encode(), senderAddress)
				write_log_line('snd', get_time(), replySegment.split('|'), log)
		elif int(flags[0]):
			# fin is already recorded
			listening = False
			print('File transfer completed')

# 4-way close connection
# FA
replySegment = create_segment(seq, ack, '1010')
receiverSocket.sendto(replySegment.encode(), senderAddress)
write_log_line('snd', get_time(), replySegment.split('|'), log)

# FINAL ACK
senderSegment, senderAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
write_log_line('rcv', get_time(), senderSegment, log)
# connection closed
print('Connection closed')
receiverSocket.close()

with open(log, 'a') as f:
	f.write('\nAmount of Data received: ' + str(data_received))
	f.write('\nNumber of Data segments Received: ' + str(nData_seg))
	f.write('\nNumber of duplicate segments received: ' + str(nDup_Seg))
					
