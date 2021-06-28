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
				System.out.println("ping to " + pingRequest.getAddress().getHostAddress() + ", seq = " + (i+1) + ", rtt = " + delay[i]  + " ms ");
			}
			catch (SocketTimeoutException e) {
				System.out.println("ping to " + pingRequest.getAddress().getHostAddress() + ", seq = " + (i+1) + ", rtt = " + "time out ");
				delay[i] = null;
			}
			Thread.sleep(TIMEOUT);
		}
		clientSocket.close();
		roundTripTime(delay);
	}

	private static void roundTripTime(Long[] delay) {
		ArrayList<Long> realDelay = new ArrayList<Long>();
		for (int i = 0; i < delay.length; i++) {
			if (delay[i] != null)
				realDelay.add(delay[i]);
		}
		long maximum = realDelay.get(0);
		long minimum = realDelay.get(0);
		double averageRTT;
		if (realDelay.size() == 0) {
			averageRTT = Double.valueOf(null);
		} else {
			averageRTT = 0;
		}
		
		for (int i = 0; i < realDelay.size(); i++) {
			if (realDelay.get(i) < minimum) {
				minimum = realDelay.get(i);
			}
			if (realDelay.get(i) > maximum) {
				maximum = realDelay.get(i);
			}
			averageRTT += realDelay.get(i);
		}
		averageRTT /= realDelay.size();
		System.out.printf("minimum: %d ms; maximum: %d ms; averageRTT: %.3f ms;\n", minimum, maximum, averageRTT);
	}
}
// end of class UDPClient