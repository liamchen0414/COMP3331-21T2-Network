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
if len(sys.argv) != 3:
	print("not enough arguments.")
	sys.exit()

# Assign variables from the supplied arguments.
receiver_port = int(sys.argv[1])
file = sys.argv[2]

# Assign other variables.
seq = '0'
ack = '0'
log = 'Receiver_log.txt'
listening = True
file_created = False
data_received = 0
nData_seg = 0
nDup_Seg = 0
with open(log, 'w') as f:
	f.write('')

# create a receiver socket and set it to listen for a client.
receiverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
receiverSocket.bind(('', receiver_port))
start_time = time.time()

# 3way handshake
senderSegment, sAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
seq, ack, F, S, A, D = read_segment(senderSegment, seq)
write_log_line(senderSegment, 'rcv', time.time()-start_time, log) # write syn to log
# reply with synack
replySegment = create_segment(seq, ack, '0110')
receiverSocket.sendto(replySegment.encode(), sAddress)
write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)
# ack
senderSegment, sAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
write_log_line(senderSegment, 'rcv', time.time()-start_time, log)
with open(file, 'w') as f:
	f.write('')
Buffer = deque()
while listening:
	senderSegment, sAddress = receiverSocket.recvfrom(2048)
	senderSegment = senderSegment.decode().split('|')
	write_log_line(senderSegment, 'rcv', time.time()-start_time, log)

	if senderSegment[0] != ack:
		# Buffer the data segment if it is out of order.
		if int(senderSegment[0]) > int(ack):
			receiverSocket.sendto(replySegment.encode(), sAddress)
			write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)
			Buffer.append(senderSegment)
			data_received += len(senderSegment[3])
		else:
			# duplicate segment counting
			nDup_Seg += 1
		nData_seg += 1
	else:
		seq, ack, F, S, A, D = read_segment(senderSegment, seq)
		if int(D):
			nData_seg += 1
			data_received += len(senderSegment[3])
			write_to_file(senderSegment)
			if Buffer:
				while Buffer[0][0] == ack and len(Buffer) > 1:
					next_header = Buffer.popleft()
					write_to_file(next_header)
					seq, ack, F, S, A, D = read_segment(next_header, seq)
				if Buffer[0][0] == ack:
					next_header = Buffer.popleft()
					write_to_file(next_header)
					seq, ack, F, S, A, D = read_segment(next_header, seq)
				replySegment = create_segment(seq, ack, '0010')
				receiverSocket.sendto(replySegment.encode(), sAddress)
				write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)
			else:
				replySegment = create_segment(seq, ack, '0010')
				receiverSocket.sendto(replySegment.encode(), sAddress)
				write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)
		elif int(F):
			# fin is already recorded
			listening = False

# 4 way close connection
# F
replySegment = create_segment(seq, ack, '0010')
receiverSocket.sendto(replySegment.encode(), sAddress)
write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)
# A
replySegment = create_segment(seq, ack, '1000')
receiverSocket.sendto(replySegment.encode(), sAddress)
write_log_line(replySegment.split('|'), 'snd', time.time()-start_time, log)

senderSegment, sAddress = receiverSocket.recvfrom(2048)
senderSegment = senderSegment.decode().split('|')
write_log_line(senderSegment, 'rcv', time.time()-start_time, log)
# connection closed
print('debug: connection closed')
receiverSocket.close()

with open(log, 'a') as f:
	f.write('\nAmount of Data received: ' + str(data_received))
	f.write('\nNumber of Data segments Received: ' + str(nData_seg))
	f.write('\nNumber of duplicate segments received: ' + str(nDup_Seg))
					



