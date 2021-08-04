# Written in Python 3.

import time
import socket
import sys
import os
import threading
from random import seed, random
from collections import deque

def get_time():
	return time.time()-start_time

# Uses PLD module to determine whether to send packet and writes to log 
def PLD_module(h, senderSocket, log, resnd=0):
	global nRetrans, nDropped, nSent
	# if greater than pdrop, send the packet
	if random() > pdrop:
		senderSocket.sendto(h.encode(), receiverAddress)
		write_to_log('snd', get_time(), h.split('|'), log)
		nSent += 1
		if resnd:
			nRetrans += 1
	else:
		write_to_log('drop', get_time(), h.split('|'), log)
		nDropped += 1

# Read a file in current directory, no error checking
def read_file(file):
	with open(file) as f:
		contents = f.read()
	file_size = os.path.getsize(file)
	return contents, file_size

# Resize files based on MSS, i is the number of lines in the new file
def resize_file(contents, MSS):
	contents = [contents]
	i = 0
	while MSS < len(contents[i]):
		contents = contents[0:i] + [contents[i][0:MSS]] + [contents[i][MSS:]]
		i += 1
	return contents, i

# creates a PTP segment
def create_segment(seq, ack, flag, payload):
	return (seq + '|' + ack + '|' + flag + '|' + payload)

# Read a PTP segment
def read_segment(segment):
	segment = segment.decode().split('|')
	seq_sender = str(int(segment[1]))
	ack_sender = str(int(segment[0]))
	F, S, A, D = segment[2][0], segment[2][1], segment[2][2], segment[2][3]
	return seq_sender, ack_sender, F, S, A, D, segment

def check_ack(segment):
	return int(segment.decode().split('|')[1])

# write to log file
def write_to_log(status, time, segment, log):
	type_list = ['F','S','A','D']
	flag_type = ''
	for i in range(len(segment[2])):
		if segment[2][i] == '1':
			flag_type += type_list[i]
	line = status + '\t' + format('%.3f' % (time*1000)) +  '\t' + flag_type + \
		 '\t' + segment[0] +  '\t' + str(len(segment[3])) +  '\t' + segment[1] + '\n'
	print(line) # debug print
	with open(log, 'a') as f:
		f.write(line)





# Main
##############################################################################
# Confirm there are enough supplied arguments.
if len(sys.argv) != 9:
	print('Wrong argument number')
	sys.exit()

# Commandline arguments
receiver_host_ip = sys.argv[1]
receiver_port = int(sys.argv[2])
file = sys.argv[3]
MWS = int(sys.argv[4])
MSS = int(sys.argv[5])
timeout = int(sys.argv[6])/1000
pdrop = float(sys.argv[7])
seed(int(sys.argv[8]))

# Assign variables for PTP
seq = '0'
ack = '0'
duplicates = 0
curr_dup = 0

# Assign variables for summary 
nDropped = 0
nRetrans = 0
nSent = 0
log = 'Sender_log.txt'
with open(log, 'w') as f:
	f.write('')

# Initiate a socket, record program starting time
senderSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
receiverAddress = (receiver_host_ip, receiver_port)
start_time = time.time()

# read a file
contents, file_size = read_file(file)
liensToSend, line_number = resize_file(contents, MSS)

# Threeway handshaking
# Send initial SYN segment to receiver.
sSegment = create_segment(seq, ack, '0100', '')
senderSocket.sendto(sSegment.encode(), receiverAddress)
write_to_log('snd', get_time(), sSegment.split('|'), log)
# Receive SYNACK from receiver.
rSegment, receiverAddress = senderSocket.recvfrom(2048)
seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
write_to_log('rcv', get_time(), rSegment, log)
# Send final ACK segment
ack = str(int(ack)+1)
sSegment = create_segment(seq, ack, '0010','')
senderSocket.sendto(sSegment.encode(), receiverAddress)
write_to_log('snd', get_time(), sSegment.split('|'),log)


# sending file
window_size = MWS/MSS
Buffer = deque()
send_times = deque()
senderSocket.settimeout(0.00001)
curr_seq, init_seq, j = seq, seq, 0
while int(seq)-int(init_seq) < file_size:
	# Send packets if there is available window space.
	if (window_size > 0 and j < len(liensToSend)):
		sSegment = create_segment(curr_seq, ack, '0001', liensToSend[j])
		curr_seq = str(int(curr_seq) + len(liensToSend[j]))
		Buffer.append(sSegment)
		send_times.append(time.time())
		PLD_module(sSegment, senderSocket, log)
		j += 1
	else:
		# Resend packet if a timeout or triple duplicate ack occurs.
		if time.time() >= send_times[0] + timeout or curr_dup >= 3:
			curr_dup = 0
			PLD_module(Buffer[0], senderSocket, log, 1)
			send_times[0] = time.time()
		# Listen for ACK response from receiver
		try:
			rSegment, receiverAddress = senderSocket.recvfrom(2048)
			curr_ack = check_ack(rSegment)
			# Checks if duplicate ACK is received.
			if curr_ack <= int(seq):
				rSegment = rSegment.decode().split('|')
				write_to_log('rcv', get_time(), rSegment, log)
				duplicates += 1
				curr_dup += 1
			# Reads ACK removes acknowledged segments from buffer and updates window.
			else:
				curr_dup = 0
				seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
				write_to_log('rcv', get_time(), rSegment, log)
				while int(Buffer[0].split('|')[0]) < int(seq) and len(Buffer) > 1:
					Buffer.popleft()
					send_times.popleft()
				if int(Buffer[0].split('|')[0]) < int(seq):
						Buffer.popleft()
						send_times.popleft()
		except socket.timeout:
			continue
senderSocket.settimeout(None)

# Sends initial FIN segment.
sSegment = create_segment(seq, ack, '1000')
senderSocket.sendto(sSegment.encode(), receiverAddress)
write_to_log('snd', get_time(), sSegment.split('|'), log)
# Waits to receive ACK.
rSegment, receiverAddress = senderSocket.recvfrom(2048)
seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
write_to_log('rcv', get_time(), rSegment, log)
# Waits to recieve FIN.
rSegment, receiverAddress = senderSocket.recvfrom(2048)
seq, ack, F, S, A, D, rSegment = read_segment(rSegment)
write_to_log('rcv', get_time(), rSegment, log)
# Returns final ACK segment and closes.
ack = str(int(ack)+1)
sSegment = create_segment(seq, ack, '0010')
senderSocket.sendto(sSegment.encode(), receiverAddress)
write_to_log('snd', get_time(), sSegment.split('|'), log)
senderSocket.close()



with open(log, 'a') as f:
	f.write('\nAmount of Data Transferred: ' + str(file_size))
	f.write('\nNumber of Data Segments Sent: ' + str(nSent))
	f.write('\nNumber of Packets Dropped: ' + str(nDropped))
	f.write('\nNumber of Retransmitted Segments: ' + str(nRetrans))
	f.write('\nNumber of Duplicate Acknowledgements received: ' + str(duplicates))



