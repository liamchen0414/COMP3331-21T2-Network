
import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
	static int server_isn = 0;

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
		    System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + "<receiver_port> <FileReceived.txt>");
            System.exit(1);
        }
	    int port = Integer.parseInt(args[0]);
	    String filename = args[1];
		// server setup
	    receiverSocket = new DatagramSocket(port);
		System.out.println("Server is ready :");
		long start_time = System.currentTimeMillis();
		// some variables to store data from sender
        String[] line = null;
        //prepare buffers
        byte[] receiveData = null;
        String receiverReply = null;
        SocketAddress sAddr;
		String flags = null;
		// 	1. Connection setup between sender and receiver
		while(true) {
			System.out.println("Waiting for sender to connect.....");
			receiveData = new byte[1024];
			DatagramPacket rPacket = new DatagramPacket(receiveData, receiveData.length);
			receiverSocket.receive(rPacket);
			// check if the received packet is a SYN packet
			String[] request = new String(rPacket.getData()).split("\\|");
			flags = request[2];
			if (makeFlag(flags) == "S") {
				int client_isn = Integer.parseInt(request[0]);
				int client_ack = Integer.parseInt(request[1]);
				// need IP and Port to send replies later
				InetAddress senderIPAddress = rPacket.getAddress();
				int senderPort = rPacket.getPort();
				long requestTime = System.currentTimeMillis() - start_time;
				System.out.println("Connection established for " + senderIPAddress + ".....");
				//  Create a new text file called FileReceived.txt
				create_log_file(filename);
				// write SYNACK to FileReceived.txt
				write_to_log(filename, "rcv", Double.parseDouble(Long.toString(requestTime))/1000, makeFlag(flags), server_isn, "0", (client_isn + 1));
				break;
			}
		}
		// PTP flag design D|A|S|F
		// 2. Data Transmission (repeat until end of file)
		// 		a. Receive PTP segment
		// 		b. Send ACK segment
		// 		c. Buffer data or write data into file
		while(true) {
			receiveData = new byte[1024];
			DatagramPacket fPacket = new DatagramPacket(receiveData, receiveData.length);
			receiverSocket.receive(fPacket);
			line = new String(rPacket.getData()).split("\\|");
			flags = line[2];
			if (makeFlag(flags).equals("F")) {

				write_to_log();
				// send FINACK
				// send FIN
				// if FINACK is received from receiver, tear down connection
				if (makeFlag(flags).equals("FA"))
					break;
			}
		}

		// 	3. Connection teardown
		clientSocket.close();
		write_log_summary();

		header = makeHeader(seq_receiver, ack_receiver, "0010", answer);
		sData = header.getBytes();
		DatagramPacket sPacket = new DatagramPacket(sData, sData.length, senderIPAddress, senderPort);
		receiverSocket.send(sPacket);
	}
}