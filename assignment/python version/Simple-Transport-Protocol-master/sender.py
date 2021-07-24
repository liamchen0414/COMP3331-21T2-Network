#!/usr/bin/env python3

import time
import socket
import sys
import os
from collections import deque
from random import seed, random

# three-way-handshake
def three_way_handshake(sender_socket, receiver_address):
	seq = '0'
	ack = '0'
	# Send SYN to server
	PTP_segment = create_header(seq, ack, '0100')
	sender_socket.sendto(PTP_segment.encode(), receiver_address)
	write_sender_log(PTP_segment.split('|'), 'snd', time.time() - start_time, sender_log)
	# Receive SYNACK response from receiver.
	rSegment, receiver_address = sender_socket.recvfrom(2048)
	seq, ack, F, S, A, D, rSegment = read_header(rSegment)
	write_sender_log(rSegment, 'rcv', time.time() - start_time, sender_log)
	# Send final ACK segment if SYNACK was received.
	if int(S) and int(A):
		ack = str(int(ack)+1)
		PTP_segment = create_header(seq, ack, '0010')
		sender_socket.sendto(PTP_segment.encode(), receiver_address)
		write_sender_log(PTP_segment.split('|'), 'snd', time.time() - start_time, sender_log)

def send_file():

	duplicates = 0
	curr_dup = 0

	Buffer = deque()
	send_times = deque()
	sender_socket.settimeout(0.00001)
	curr_seq, init_seq, j = seq, seq, 0
	while int(seq)-int(init_seq) < file_bytes:
		# Send packets if there is available window space.
		if (int(curr_seq) - int(seq)) + MSS <= MWS and j < len(file_contents):
			PTP_segment = create_header(curr_seq, ack, '0001', file_contents[j])
			curr_seq = str(int(curr_seq) + len(file_contents[j]))
			Buffer.append(PTP_segment)
			send_times.append(time.time())
			PL_module(PTP_segment, sender_socket, sender_log)
			j += 1
		else:
			# Resend packet if a timeout or triple duplicate ack occurs.
			if time.time() >= send_times[0] + timeout or curr_dup >= 3:
				curr_dup = 0
				PL_module(Buffer[0], sender_socket, sender_log, 1)
				send_times[0] = time.time()
			# Listen for ACK response from receiver
			try:
				rSegment, receiver_address = sender_socket.recvfrom(2048)
				curr_ack = check_ack(rSegment)
				# Checks if duplicate ACK is received.
				if curr_ack <= int(seq):
					rSegment = rSegment.decode().split('|')
					write_sender_log(rSegment, 'rcv', time.time() - start_time, sender_log)
					duplicates += 1
					curr_dup += 1
				# Reads ACK removes acknowledged segments from buffer and updates window.
				else:
					curr_dup = 0
					seq, ack, F, S, A, D, rSegment = read_header(rSegment)
					write_sender_log(rSegment, 'rcv', time.time() - start_time, sender_log)
					while int(Buffer[0].split('|')[0]) < int(seq) and len(Buffer) > 1:
						Buffer.popleft()
						send_times.popleft()
					if int(Buffer[0].split('|')[0]) < int(seq):
							Buffer.popleft()
							send_times.popleft()
			except socket.timeout:
				continue
	sender_socket.settimeout(None)

def connection_teardown():
	# Sends initial FIN segment.
	PTP_segment = create_header(seq, ack, '1000')
	sender_socket.sendto(PTP_segment.encode(), receiver_address)
	write_sender_log(PTP_segment.split('|'), 'snd', time.time() - start_time, sender_log)
	# Waits to receive ACK.
	rSegment, receiver_address = sender_socket.recvfrom(2048)
	seq, ack, F, S, A, D, rSegment = read_header(rSegment)
	write_sender_log(rSegment, 'rcv', time.time() - start_time, sender_log)
	# Waits to recieve FIN.
	rSegment, receiver_address = sender_socket.recvfrom(2048)
	seq, ack, F, S, A, D, rSegment = read_header(rSegment)
	write_sender_log(rSegment, 'rcv', time.time() - start_time, sender_log)
	# Returns final ACK segment and closes.
	ack = str(int(ack)+1)
	PTP_segment = create_header(seq, ack, '0010')
	sender_socket.sendto(PTP_segment.encode(), receiver_address)
	write_sender_log(PTP_segment.split('|'), 'snd', time.time() - start_time, sender_log)
	sender_socket.close()


# Uses PLD module to determine whether to send segment
# required arguments, socket, sender_log file, 
def PL_module(header, sender_socket, sender_seed, pdrop, retrans_flag=False):
	seg_drop = 0
	retrans_counter = 0
	sent_total = 0
	seed(sender_seed)
	p_to_drop = random()
	# if the probability to drop a packet is higher than pdrop, we send it
	if p_to_drop > pdrop:
		write_sender_log(header.split('|'), 'snd', time.time() - start_time, sender_log)
		sender_socket.sendto(header.encode(), receiver_address)
		sent_total += 1
		# if this packet is a retransmission, we increment the retransmission counter
		if retrans_flag:
			retrans_counter += 1
	# else, we drop it and increment the counter for segment dropped
	else:
		write_sender_log(header.split('|'), 'drop', time.time() - start_time, sender_log)
		seg_drop += 1

# regroup the lines in file based on MSS
def divide_file(file, MSS):
	file = [file]
	i = 0
	while len(file[i]) > MSS:
		file = file[0:i] + [file[i][0:MSS]] + [file[i][MSS:]]
		i += 1
	return file

# Opens and reads file, assume file is in the current location
def read_file(file):
	with open(file) as f:
		file_contents = f.read()
	file_bytes = os.path.getsize(file)
	return file_contents, file_bytes

# create a header
def create_header(seq, ack, flag, data=''):
	return (seq + '|' + ack + '|' + flag + '|' + data)

# Read a header
# <type of packet> could be S (SYN), A (ACK), F (FIN) and D (Data)
def read_header(header):
	header = header.decode().split('|')
	seq = str(int(header[0]))
	ack = str(int(header[1]))
	F, S, A, D = header[2][0], header[2][1], header[2][2], header[2][3]
	return seq, ack, F, S, A, D, header

def check_ack(header):
	return int(header.decode().split('|')[1])

# Formats and writes a line to sender_log file.
# TO UPDATE
def write_sender_log(header, status, time, sender_log):
	type_list = ['F','S','A','D']
	flag_type = ''
	for i in range(len(header[2])):
		if header[2][i] == '1':
			flag_type += type_list[i]
	line = '{0: <5}'.format(status)
	line += '{0: <9}'.format('%.3f' % (time*1000))
	line += '{0: <4}'.format(flag_type)
	line += '{0: <11}'.format(header[0])
	line += '{0: <7}'.format(str(len(header[3])))
	line += header[1] + '\n'
	with open(sender_log, 'a') as f:
		f.write(line)

def main():
	global start_time, sender_log
	# define argument variables
	if len(sys.argv) < 9:
		sys.exit()

	receiver_host_ip = sys.argv[1]
	receiver_port = int(sys.argv[2])
	file = sys.argv[3]
	MWS = int(sys.argv[4])
	MSS = int(sys.argv[5])
	timeout = int(sys.argv[6])/1000
	pdrop = float(sys.argv[7]) # variables used exclusively for PL_module
	sender_seed = (int(sys.argv[8])) # variables used exclusively for PL_module

	# Initiate a socket and define receiver's address
	sender_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	receiver_address = (receiver_host_ip, receiver_port)

	# Connection setup
	sender_log = 'Sender_log.txt'
	start_time = time.time()
	three_way_handshake(sender_socket, receiver_address)

	# data transmission (repeat until end of file)
	# read file
	with open(sender_log, 'w') as f:
		f.write('')
	file_contents, file_bytes = read_file(file)
	file_contents = divide_file(file_contents, MSS)
	# create PTP segment
	

	with open(sender_log, 'a') as f:
	f.write('\nAmount of Data Transferred: ' + str(file_bytes))
	f.write('\nNumber of Data Segments Sent: ' + str(sent_total))
	f.write('\nNumber of Packets Dropped: ' + str(seg_drop))
	f.write('\nNumber of Retransmitted Segments: ' + str(retrans_counter))
	f.write('\nNumber of Duplicate Acknowledgements received: ' + str(duplicates))