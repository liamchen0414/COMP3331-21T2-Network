# Written in Python 3.

import time
import socket
import sys
import os
from random import randint, seed, random
from collections import deque

# Uses PLD module to determine whether to send packet and writes to log 
def PLD_module(h, sSocket, log, resnd=0):
	global resent, seg_drop, seg_sent
	p = random()
	if p > pdrop:
		write_log_line(h.split('|'), 'snd', time.time()-st_time, log)
		sSocket.sendto(h.encode('ascii'), rAddress)
		seg_sent += 1
		if resnd:
			resent += 1
	else:
		write_log_line(h.split('|'), 'drop', time.time()-st_time, log)
		seg_drop += 1

# Divides the file into segments based on maximum segment size
def divide_file(file, MSS):
	file = [file]
	i = 0
	while len(file[i]) > MSS:
		file = file[0:i] + [file[i][0:MSS]] + [file[i][MSS:]]
		i += 1
	return file

# Opens and reads file if present in current directory
def read_file(file):
	if file in os.listdir('.'):
		with open(file) as f:
			contents = f.read()
		file_size = os.path.getsize(file)
		return contents, file_size, True
	# If file not present empty arguments are returned.
	return 0, 0, False

# creates a header based on arguments.
def create_segment(seq, ack, flag, data=''):
	return (seq + '|' + ack + '|' + flag + '|' + data)

# Reads header, updates seq/ack and assigns flags.
def read_segment(h):
	h = h.decode('ascii').split('|')
	seq = str(int(h[1]))
	ack = str(int(h[0]))
	F, S, A, D = h[2][0], h[2][1], h[2][2], h[2][3]
	return seq, ack, F, S, A, D, h

def check_ack(h):
	return int(h.decode('ascii').split('|')[1])

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


# Main program starts here

# Confirm there are enough supplied arguments.
if len(sys.argv) < 9:
	sys.exit()

# Assign variables from supplied arguments.
receiver_host_ip = sys.argv[1]
receiver_port = int(sys.argv[2])
file = sys.argv[3]
MWS = int(sys.argv[4])
MSS = int(sys.argv[5])
timeout = int(sys.argv[6])/1000
pdrop = float(sys.argv[7])
seed(int(sys.argv[8]))

# Assign other variables.
seq = '0'
ack = '0'
seg_drop = 0
resent = 0
seg_sent = 0
duplicates = 0
curr_dup = 0
log = 'Sender_log.txt'
with open(log, 'w') as f:
	f.write('')

# Reads, divides and checks the presence of the chosen file.
contents, file_size, file_present = read_file(file)
if not file_present:
	sys.exit()
contents = divide_file(contents, MSS)

# Initiate a socket.
sSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
rAddress = (receiver_host_ip, receiver_port)

# Stages [initial handshake, data, terminating handshake]
stage = [False, False, False]


# Main loop continues until all stages are complete.
st_time = time.time()

while False in stage:
	
	# Stage 1 initial handshake.
	if not stage[0]:
		# Send initial SYN segment to receiver.
		sSegment = create_segment(seq, ack, '0100')
		sSocket.sendto(sSegment.encode('ascii'), rAddress)
		write_log_line(sSegment.split('|'), 'snd', time.time()-st_time, log)
		# Receive SYNACK response from receiver.
		rSegment, rAddress = sSocket.recvfrom(2048)
		seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
		write_log_line(rSegment, 'rcv', time.time()-st_time, log)
		# Send final ACK segment if SYNACK was received.
		if int(S) and int(A):
			ack = str(int(ack)+1)
			sSegment = create_segment(seq, ack, '0010')
			sSocket.sendto(sSegment.encode('ascii'), rAddress)
			write_log_line(sSegment.split('|'), 'snd', time.time()-st_time, log)
			stage[0] = True

	# Stage 2 sending data.
	elif not stage[1]:
		Buffer = deque()
		send_times = deque()
		sSocket.settimeout(0.00001)
		curr_seq, init_seq, j = seq, seq, 0
		while int(seq)-int(init_seq) < file_size:
			# Send packets if there is available window space.
			if (int(curr_seq) - int(seq)) + MSS <= MWS and j < len(contents):
				sSegment = create_segment(curr_seq, ack, '0001', contents[j])
				curr_seq = str(int(curr_seq) + len(contents[j]))
				Buffer.append(sSegment)
				send_times.append(time.time())
				PLD_module(sSegment, sSocket, log)
				j += 1
			else:
				# Resend packet if a timeout or triple duplicate ack occurs.
				if time.time() >= send_times[0] + timeout or curr_dup >= 3:
					curr_dup = 0
					PLD_module(Buffer[0], sSocket, log, 1)
					send_times[0] = time.time()
				# Listen for ACK response from receiver
				try:
					rSegment, rAddress = sSocket.recvfrom(2048)
					curr_ack = check_ack(rSegment)
					# Checks if duplicate ACK is received.
					if curr_ack <= int(seq):
						rSegment = rSegment.decode('ascii').split('|')
						write_log_line(rSegment, 'rcv', time.time()-st_time, log)
						duplicates += 1
						curr_dup += 1
					# Reads ACK removes acknowledged segments from buffer and updates window.
					else:
						curr_dup = 0
						seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
						write_log_line(rSegment, 'rcv', time.time()-st_time, log)
						while int(Buffer[0].split('|')[0]) < int(seq) and len(Buffer) > 1:
							Buffer.popleft()
							send_times.popleft()
						if int(Buffer[0].split('|')[0]) < int(seq):
								Buffer.popleft()
								send_times.popleft()
				except socket.timeout:
					continue
		sSocket.settimeout(None)
		stage[1] = True

	# Stage 3 final handshake.
	elif not stage[2]:
		# Sends initial FIN segment.
		sSegment = create_segment(seq, ack, '1000')
		sSocket.sendto(sSegment.encode('ascii'), rAddress)
		write_log_line(sSegment.split('|'), 'snd', time.time()-st_time, log)
		# Waits to receive ACK.
		rSegment, rAddress = sSocket.recvfrom(2048)
		seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
		write_log_line(rSegment, 'rcv', time.time()-st_time, log)
		# Waits to recieve FIN.
		rSegment, rAddress = sSocket.recvfrom(2048)
		seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
		write_log_line(rSegment, 'rcv', time.time()-st_time, log)
		# Returns final ACK segment and closes.
		ack = str(int(ack)+1)
		sSegment = create_segment(seq, ack, '0010')
		sSocket.sendto(sSegment.encode('ascii'), rAddress)
		write_log_line(sSegment.split('|'), 'snd', time.time()-st_time, log)
		sSocket.close()
		stage[2] = True

with open(log, 'a') as f:
	f.write('\nAmount of Data Transferred: ' + str(file_size))
	f.write('\nNumber of Data Segments Sent: ' + str(seg_sent))
	f.write('\nNumber of Packets Dropped: ' + str(seg_drop))
	f.write('\nNumber of Retransmitted Segments: ' + str(resent))
	f.write('\nNumber of Duplicate Acknowledgements received: ' + str(duplicates))



