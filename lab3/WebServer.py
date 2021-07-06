#!/usr/bin/evb python3

import sys
from socket import *
import io

serverName = 'localhost'
serverPort = int(sys.argv[1])
serverSocket = socket(AF_INET, SOCK_STREAM)
#Prepare a sever socket
#Fill in start
serverSocket.bind((serverName,serverPort))
serverSocket.listen(1)
print ('the web server is up on port:',serverPort)

while True:
    print ('Ready...')
    connectionSocket, addr = serverSocket.accept()

    try:
        message = connectionSocket.recv(1024)
        filename = message.split()[1]
        with io.open(filename[1:],'rb') as f:
            outputdata = f.read()

        #Send one HTTP header line into socket
        connectionSocket.send('\nHTTP/1.1 200 OK\n\n'.encode())
        
        for i in range(0, len(outputdata)):
            connectionSocket.send(outputdata[i])
        connectionSocket.send("\r\n")
        connectionSocket.close()
    except IOError:
        connectionSocket.send('\nHTTP/1.1 404 Not Found\r\n')
        connectionSocket.close()
        pass
