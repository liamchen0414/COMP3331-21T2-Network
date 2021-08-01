#!/usr/bin/python3
import socket
import sys
import time
from collections import deque

# creates a Segment
def createSegment(seq, ack, flag, payload=''):
	return (seq + '|' + ack + '|' + flag + '|' + payload)

# Read and Prepare a Segment
def prepareSegment(receivedSegment, receiver_seq):
	# receiver seq is sender ack, receiver ack is sender seq
	receiver_seq = str(int(receivedSegment[1]))
	receiver_ack = str(int(receivedSegment[0]) + max(len(receivedSegment[3]), 1)) # SYN or FIN has length of 1
	F, S, A, D = receivedSegment[2][0], receivedSegment[2][1], receivedSegment[2][2], receivedSegment[2][3]
	return receiver_seq, receiver_ack, F, S, A, D

# Write segment to file. format (seq + '|' + ack + '|' + flag + '|' + payload)
def write_file(segment):
	payload = segment[3]
	with open(file, 'a') as f:
		f.write(payload)

# Write to log file
def write_to_log(status, time, segment, log):
	flags = ['F','S','A','D']
	flag = ''
	# convert 0 and 1 bits to letters
	for i in range(0, 4):
		if segment[2][i] == '1':
			flag += flags[i]
	line = status + '\t' + format('%.3f' % (time*1000)) + '\t' + flag + \
		'\t' + segment[0] + '\t' + str(len(segment[3])) + '\t' + segment[1] + '\n'
	with open(log, 'a') as f:
		f.write(line)

# Main program
if(len(sys.argv) != 3):
	print("Usage: sender.py <receiver_port> <FileReceived.txt>")
	sys.exit()

# from command line argument
receiver_port = int(sys.argv[1])
file = sys.argv[2]

# Assign other variables.
seq = '0'

log = 'Receiver_log.txt'
listening = True
time_init = False
file_created = False
data_received = 0
seg_received = 0
duplicate_seg = 0
with open(log, 'w') as f:
	f.write('')

# create a receiver socket and set it to listen for a client.
st_time = time.time()
rSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
rSocket.bind(('', receiver_port))
print("Server is up: waiting for handshake")
Buffer = deque()

print("Threeway Handshaking")
while True:
	sSegment, sAddress = rSocket.recvfrom(2048)
	sSegment = sSegment.decode().split('|')
	print("debug " + sSegment[0] + ' ' + sSegment[1] + ' ' + sSegment[2])
	if (int(sSegment[2][1])): # if SYN received
		write_to_log('rcv', time.time()-st_time, sSegment, log)
		ack = sSegment[0]
		rSegment = createSegment(seq, ack, '0110', "")
		rSocket.sendto(rSegment.encode(), sAddress)
		write_to_log('snd', time.time()-st_time, rSegment.split('|'), log)
	elif (int(sSegment[2][2])):
		write_to_log('rcv', time.time()-st_time, sSegment, log)
		if not file_created:
			with open(file, 'w') as f:
				f.write('')
			file_created = True
		break

while listening:
	sSegment, sAddress = rSocket.recvfrom(2048)

	if not time_init:
		time_init = True
	sSegment = sSegment.decode().split('|')
	write_to_log('rcv', time.time()-st_time, sSegment, log)

	if sSegment[0] != ack and ack != False:
		# Buffer the data segment if it is out of order.
		if int(sSegment[0]) > int(ack):
			rSocket.sendto(rSegment.encode(), sAddress)
			write_to_log('snd', time.time()-st_time, rSegment.split('|'), log)
			Buffer.append(sSegment)
			seg_received += 1
			data_received += len(sSegment[3])
		# Ignore duplicate segment that was already received.
		else:
			duplicate_seg += 1
			seg_received += 1
	# Creates output file once final ACK of handshake is received.
	else:
		seq, ack, F, S, A, D = prepareSegment(sSegment, seq)

		if int(D):
			print("File transfer...")
			seg_received += 1
			data_received += len(sSegment[3])
			write_file(sSegment)
			if Buffer:
				while Buffer[0][0] == ack and len(Buffer) > 1:
					nextSegment = Buffer.popleft()
					write_file(nextSegment)
					seq, ack, F, S, A, D = prepareSegment(nextSegment, seq)
				if Buffer[0][0] == ack:
					nextSegment = Buffer.popleft()
					write_file(nextSegment)
					seq, ack, F, S, A, D = prepareSegment(nextSegment, seq)
				rSegment = createSegment(seq, ack, '0010')
				rSocket.sendto(rSegment.encode(), sAddress)
				write_to_log('snd', time.time()-st_time, rSegment.split('|'), log)
			else:
				rSegment = createSegment(seq, ack, '0010')
				rSocket.sendto(rSegment.encode(), sAddress)
				write_to_log('snd', time.time()-st_time, rSegment.split('|'), log)
		elif int(A):
			with open(log, 'w') as f:
				f.write('')
		elif int(F):
			rSegment = createSegment(seq, ack, '1010')
			rSocket.sendto(rSegment.encode(), sAddress)
			write_to_log('snd', time.time()-st_time, rSegment.split('|'), log)
			while listening:
				sSegment, sAddress = rSocket.recvfrom(2048)
				sSegment = sSegment.decode().split('|')
				write_to_log('rcv', time.time()-st_time, sSegment, log)
				rSocket.close()
				listening = False
			print("File transfer completed")
with open(log, 'a') as f:
	f.write('\nAmount of Data received: ' + str(data_received))
	f.write('\nNumber of Data segments Received: ' + str(seg_received))
	f.write('\nNumber of duplicate segments received: ' + str(duplicate_seg))
					



