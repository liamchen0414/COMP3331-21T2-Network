#!/usr/bin/python3

import socket
import sys
import os
import random
import time
from collections import deque
import threading

t_lock=threading.Condition()
# creates a segment
def create_segment(seq, ack, flag, payload=''):
	return (seq + '|' + ack + '|' + flag + '|' + payload)

# reads a segment
def read_segment(segment):
	seq = str(int(segment[1]))
	ack = str(int(segment[0]))
	flags = segment[2]
	return seq, ack, flags

# reads a file
def read_file(file):
	with open(file) as f:
		linesToSend = f.read()
	bytes_in_file = os.path.getsize(file)
	return linesToSend, bytes_in_file

# divides a file based on MSS
def resize_line(file, MSS):
	file = [file]
	i = 0
	while len(file[i]) > MSS:
		file = file[0:i] + [file[i][0:MSS]] + [file[i][MSS:]]
		i += 1
	return file, i

# checks ack acknowledged by the receiver
def check_ack_receiver(segment):
	return int(segment.decode().split('|')[1])

# gets the current time difference
def get_time():
	return time.time()-start_time

# writes a line to log
def write_log(status, time, segment):
	flag_type = ''
	flag_list = ['F','S','A','D']
	for i in range(0,4):
		if segment[2][i] == '1':
			flag_type += flag_list[i]
	line = status + '\t' + format('%.3f' % (time*1000)) + '\t' + flag_type + \
		'\t' + segment[0] + '\t' + str(len(segment[3])) + '\t' + segment[1] + '\n'
	with open('Sender_log.txt', 'a') as f:
		f.write(line)


# PL module to decide sending or dropping
def PL_module(segment, senderSocket, is_retrans):
	global nRetrans_sent, nSeg_drop, nSegmentSent
	if is_retrans:
		nRetrans_sent += 1
	if random.random() > pdrop: # random is greater than pdrop, send the packet
		status = 'snd'
		senderSocket.sendto(segment.encode(), receiverAddress)
		nSegmentSent += 1
	else:
		status = 'drop'
		nSeg_drop += 1 # if segment is dropped, increment the segment drop counter
	# write to log
	write_log(status, get_time(), segment.split('|'))

# Main program
# Confirm there are enough supplied arguments.
if len(sys.argv) != 9:
	print("Usage: Sender.py <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>")
	sys.exit()

# Assigns arguments
receiver_host_ip = sys.argv[1]
receiver_port = int(sys.argv[2])
file = sys.argv[3]
MWS = int(sys.argv[4])
MSS = int(sys.argv[5])
timeout = int(sys.argv[6])/1000 # in milliseconds
pdrop = float(sys.argv[7])
random.seed(int(sys.argv[8]))

# Assign other variables.
seq = '0'
ack = '0'
log = 'Sender_log.txt'
# Assign variables for summary
nSeg_drop = 0
nRetrans_sent = 0
nSegmentSent = 0

# Assign variables for triple duplicates
nDuplicates = 0
triple_dup_counter = 0
linesToSend, bytes_in_file = read_file(file)
linesToSend, nLines = resize_line(linesToSend, MSS)

# initiate a socket.
senderSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
receiverAddress = (receiver_host_ip, receiver_port)
start_time = time.time()
with open(log, 'w') as f:
	f.write('')

print('Initiating 3-way handshaking')
# 1. three way handshake
# send SYN 'S'
sendSegment = create_segment(seq, ack, '0100')
senderSocket.sendto(sendSegment.encode(), receiverAddress)
write_log('snd', get_time(), sendSegment.split('|'))
# receive 'SA'
receiverSegment, receiverAddress = senderSocket.recvfrom(2048)
receiverSegment = receiverSegment.decode().split('|')
seq, ack, flags = read_segment(receiverSegment)
write_log('rcv', get_time(), receiverSegment)
# Send ACK 'A'
if flags[1] and flags[2]:
	ack = str(int(ack)+1)
	sendSegment = create_segment(seq, ack, '0010')
	senderSocket.sendto(sendSegment.encode(), receiverAddress)
	write_log('snd', get_time(), sendSegment.split('|'))

# 2. sending file
# sliding window
senderWindow = deque()
timeWindow = deque()
# set a timeout on blocking socket operations
senderSocket.settimeout(0.0000001)
seq_isn = seq
lastByteSent = seq
lastByteAcked = seq
line_index = 0
is_finished = False

while int(seq)-int(seq_isn) < bytes_in_file and not(is_finished):
	# send if sender window is not full and there is still lines to send
	# LastByteSent – LastByteAcked ≤ MWS and line_index <= nLines
	if (int(lastByteSent) - int(lastByteAcked)) <= MWS and line_index <= nLines:
		sendSegment = create_segment(lastByteSent, ack, '0001', linesToSend[line_index])
		# update LastByteSent to next sequence number
		lastByteSent = str(int(lastByteSent) + len(linesToSend[line_index]))
		senderWindow.append(sendSegment)
		timeWindow.append(time.time())
		# sending packet to PL module
		PL_module(sendSegment, senderSocket, 0)
		line_index += 1
	else:
		
		# resend packet if a timeout
		if (time.time() - timeWindow[0] >= timeout) or triple_dup_counter == 3:
			# a retransmission should also be fed to pl module, change is_retrans flag to 1
			if triple_dup_counter == 3:
				triple_dup_counter == 0

			PL_module(senderWindow[0], senderSocket, 1)
			# update the retrans packet sent time
			timeWindow[0] = time.time()
		try:
			receiverSegment, receiverAddress = senderSocket.recvfrom(2048)
			lastByteAcked = check_ack_receiver(receiverSegment)
			receiverSegment = receiverSegment.decode().split('|')
			write_log('rcv', get_time(), receiverSegment)
			# duplicate acks
			if int(lastByteAcked) < int(senderWindow[0].split('|')[0])  + len(linesToSend[line_index - 1]):
				nDuplicates += 1
				triple_dup_counter += 1
			# read ACK removes acknowledged segments from buffer and updates window.
			else:
				seq, ack, flags = read_segment(receiverSegment)
				# if there are still pakcets in the sender window
				while int(senderWindow[0].split('|')[0]) < int(seq) and len(senderWindow) > 0:
					senderWindow.popleft()
					timeWindow.popleft()
					if len(senderWindow) == 0:
						is_finished = True
						break
		except socket.timeout:
			continue
senderSocket.settimeout(None)

# 3. close connection
# Sends FIN
seq_aft = seq
sendSegment = create_segment(seq, ack, '1000')
senderSocket.sendto(sendSegment.encode(), receiverAddress)
write_log('snd', get_time(), sendSegment.split('|'))
# Receive FA.
receiverSegment, receiverAddress = senderSocket.recvfrom(2048)
receiverSegment = receiverSegment.decode().split('|')
seq, ack, flags = read_segment(receiverSegment)
write_log('rcv', get_time(), receiverSegment)
# Returns final ACK
ack = str(int(ack)+1)
sendSegment = create_segment(seq, ack, '0010')
senderSocket.sendto(sendSegment.encode(), receiverAddress)
write_log('snd', get_time(), sendSegment.split('|'))

print('Closing connection......')
senderSocket.close()
print('Connection is closed')

with open(log, 'a') as f:
	f.write('\nAmount of Data Transferred: ' + str(int(seq_aft) - int(seq_isn)))
	f.write('\nNumber of Data Segments Sent: ' + str(nSegmentSent))
	f.write('\nNumber of Packets Dropped: ' + str(nSeg_drop))
	f.write('\nNumber of Retransmitted Segments: ' + str(nRetrans_sent))
	f.write('\nNumber of Duplicate Acknowledgements received: ' + str(nDuplicates))

print('Program finished')