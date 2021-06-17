import java.io.*;
import java.net.*;
import java.util.*;

public class PingClient {
	private static final int TIMEOUT = 600;  // milliseconds
	private static final int PING_REQUESTS = 15;

	public static void main(String[] args) throws Exception {
		// Get command line argument.
		if (args.length != 2) {
			System.out.println("Required arguments: host port");
			return;
		}

		// Define socket parameters, address and Port No
        InetAddress host = InetAddress.getByName(args[0]);
		int port = Integer.parseInt(args[1]); 
		
		// create socket which connects to server
		// set the timeout to be 600ms
		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(TIMEOUT);
		int sequence_number = 3331;
		Long[] delay = new Long[PING_REQUESTS]; // used to store results
		for(int i = 0; i < PING_REQUESTS; i++, sequence_number++) {
			// set up msg and timer
			String sentence;
			Date date = new Date();
			long startTime = date.getTime();
			// Each message contains a payload of data that includes the keyword PING, a sequence number starting from 3,331, and a timestamp.
			sentence = "PING " + Integer.toString(sequence_number) + " " + startTime + " \r\n";

			//prepare for sending
			byte[] sendData=new byte[1024];
			sendData=sentence.getBytes();

			// write to server, need to create DatagramPacket with server address and port No
			DatagramPacket pingRequest=new DatagramPacket(sendData,sendData.length,host,port);
			//actual send call
			clientSocket.send(pingRequest);
			
			//prepare buffer to receive reply
			byte[] receiveData=new byte[1024];
			// receive from server
			DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
			try {
				clientSocket.receive(receivePacket);
				date = new Date();
				long endTime = date.getTime();
				delay[i] = endTime - startTime;
				System.out.println("ping to " + host + ", seq = " + sequence_number + ", rtt = " + delay[i]  + " ms; ");
			}
			catch (SocketTimeoutException e) {
				System.out.println("ping to " + host + ", seq = " + sequence_number + ", rtt = " + "timeout; ");
				delay[i] = Long.valueOf(0);
			}
			Thread.sleep(2 * TIMEOUT);
		}
		clientSocket.close();
		roundTripTime(delay);
	}

	private static void roundTripTime(Long[] delay) {
		int first;
		for (first = 0; first < delay.length; first++) {
			if (delay[first] == Long.valueOf(0))
				first++;
		}
		long minimum = delay[0];
		long maximum = delay[0]; 
		long averageRTT = 0;
		int size = delay.length;

		for (int i = 0; i < delay.length; i++) {
			long d = delay[i];
			if (d == Long.valueOf(0)){
				size--;
			} else {
				if (d < minimum) {
					minimum = d;
				}

				if (d > maximum) {
					maximum = d;
				}
				averageRTT += d;
			}
		}
		averageRTT /= size;
		System.out.println("minimum: " + minimum + "ms; maximum: " + maximum + "ms; averageRTT: " + averageRTT + "ms;");
	}

} // end of class UDPClient