#!/usr/bin/python2

from sender import MWS
import threading
import time
import random
def keep_sending():
    MWS = 500
    MSS = 100
    sender_seq = 0
    sendBase = sender_seq
    sentPacket = 0
    rdrop = random()
    pdrop = 0.5
    while(sentPacket <= MWS/MSS):
        if(rdrop > pdrop):
            # send this packet
        else:
            # drop this packet
        sentPacket += 1
        # if this is the last packet, break the while loop
        if()


linestoSend = 100
from threading import Thread
sending = Thread(target=keep_sending, args=(linestoSend,))
sending.start()