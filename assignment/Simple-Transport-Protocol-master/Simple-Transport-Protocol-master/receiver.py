# Written in Python 3

import socket
import sys
from random import randint
import time
from collections import deque

# creates a header based on arguments.
def create_segment(seq, ack, flag, data=''):
	return (seq + '|' + ack + '|' + flag + '|' + data)

# Reads header, updates seq/ack and assigns flags.
def read_segment(header, seq):
	ack = str(int(header[0]) + max(len(header[3]), 1))
	if int(header[1]):
		seq = str(int(header[1]))
	F, S, A, D = header[2][0], header[2][1], header[2][2], header[2][3]
	return seq, ack, F, S, A, D

# Formats and writes a line to log file.
def write_log_line(header, category, time, log):
	type_list = ['F','S','A','D']
	flag_type = ''
	for i in range(len(header[2])):
		if header[2][i] == '1':
			flag_type += type_list[i]
	line = '{0: <5}'.format(category)
	line += '{0: <9}'.format('%.2f' % (time*1000))
	line += '{0: <4}'.format(flag_type)
	line += '{0: <11}'.format(header[0])
	line += '{0: <7}'.format(str(len(header[3])))
	line += header[1] + '\n'
	with open(log, 'a') as f:
		f.write(line)

# Write segment data to file.
def write_to_file(header):
	with open(file, 'a') as f:
		f.write(header[3])


# Main program starts here

# Confirm there are enough supplied arguments.
if len(sys.argv) < 3:
	print("not enough arguments.")
	sys.exit()

# Assign variables from the supplied arguments.
receiver_port = int(sys.argv[1])
file = sys.argv[2]

# Assign other variables.
seq = '0'
ack = False
log = 'Receiver_log.txt'
listening = True
file_created, time_init = False, False
rec_data = 0
data_seg = 0
dup_seg = 0
with open(log, 'w') as f:
	f.write('')

# create a receiver socket and set it to listen for a client.
rSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
rSocket.bind(('', receiver_port))

Buffer = deque()

while listening:
	sSegment, sAddress = rSocket.recvfrom(2048)

	if not time_init:
		st_time = time.time()
		time_init = True
	sSegment = sSegment.decode('ascii').split('|')
	write_log_line(sSegment, 'rcv', time.time()-st_time, log)

	if sSegment[0] != ack and ack != False:
		# Buffer the data segment if it is out of order.
		if int(sSegment[0]) > int(ack):
			rSocket.sendto(rSegment.encode('ascii'), sAddress)
			write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
			Buffer.append(sSegment)
			data_seg += 1
			rec_data += len(sSegment[3])
		# Ignore duplicate segment that was already received.
		else:
			dup_seg += 1
			data_seg += 1
	# Creates output file once final ACK of handshake is received.
	elif int(sSegment[2][2]):
		if not file_created:
			with open(file, 'w') as f:
				f.write('')
			file_created = True
	else:
		seq, ack, F, S, A, D = read_segment(sSegment, seq)
		if int(S):
			rSegment = create_segment(seq, ack, '0110')
			rSocket.sendto(rSegment.encode('ascii'), sAddress)
			write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
		elif int(D):
			data_seg += 1
			rec_data += len(sSegment[3])
			write_to_file(sSegment)
			if Buffer:
				while Buffer[0][0] == ack and len(Buffer) > 1:
					next_header = Buffer.popleft()
					write_to_file(next_header)
					seq, ack, F, S, A, D = read_segment(next_header, seq)
				if Buffer[0][0] == ack:
					next_header = Buffer.popleft()
					write_to_file(next_header)
					seq, ack, F, S, A, D = read_segment(next_header, seq)
				rSegment = create_segment(seq, ack, '0010')
				rSocket.sendto(rSegment.encode('ascii'), sAddress)
				write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
			else:
				rSegment = create_segment(seq, ack, '0010')
				rSocket.sendto(rSegment.encode('ascii'), sAddress)
				write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
		elif int(A):
			with open(log_file, 'w') as f:
				f.write('')
		elif int(F):
			rSegment = create_segment(seq, ack, '0010')
			rSocket.sendto(rSegment.encode('ascii'), sAddress)
			write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
			rSegment = create_segment(seq, ack, '1000')
			rSocket.sendto(rSegment.encode('ascii'), sAddress)
			write_log_line(rSegment.split('|'), 'snd', time.time()-st_time, log)
			while listening:
				sSegment, sAddress = rSocket.recvfrom(2048)
				sSegment = sSegment.decode('ascii').split('|')
				write_log_line(sSegment, 'rcv', time.time()-st_time, log)
				rSocket.close()
				listening = False

with open(log, 'a') as f:
	f.write('\nAmount of Data received: ' + str(rec_data))
	f.write('\nNumber of Data segments Received: ' + str(data_seg))
	f.write('\nNumber of duplicate segments received: ' + str(dup_seg))
					



