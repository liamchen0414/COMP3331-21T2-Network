import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Server to process ping requests over UDP. 
 * The server sits in an infinite loop listening for incoming UDP packets. 
 * When a packet comes in, the server simply sends the encapsulated data back to the client.
 */

public class Receiver {
	static long systemTime = System.currentTimeMillis();
	static long systemEndTime = -1;
	private static int MSS = 50;
	static int senderPort;
	static InetAddress senderHost;

	public static boolean threeHandShake(DatagramSocket socket, FileWriter logFileWriter) throws Exception {
		int client_isn = 0;
		int server_isn = 0;
		long client_isn_second;
		long server_isn_second;
		// Create random number generator for use in simulating
		// packet loss and network delay.
		Random random = new Random();

		// Create a datagram socket for receiving and sending UDP packets
		// through the port specified on the command line.

		// Processing loop.
		// Create a datagram packet to hold incomming UDP packet.
		DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

		// Block until the host receives a UDP packet.
		socket.receive(request);
		socket.setSoTimeout(500);
		systemTime = System.currentTimeMillis();
		String fields = fetchData(request);
		senderHost = request.getAddress();
		senderPort = request.getPort();
		// Print the recieved data.
		systemEndTime = System.currentTimeMillis();
		logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  S  " + Integer.parseInt(fields.substring(3, 13))
				+ "  0  0" + "\n");
		if (fields.substring(0, 3).equals("100")) {
			client_isn = Integer.parseInt(fields.substring(3, 13));
			server_isn = random.nextInt(2147483647);
			client_isn++;
			byte[] buf = ("101" + String.format("%010d", server_isn) + String.format("%010d", client_isn)).getBytes();
			DatagramPacket reply = new DatagramPacket(buf, buf.length, senderHost, senderPort);
			socket.send(reply);
			systemEndTime = System.currentTimeMillis();
			logFileWriter.write(
					"snd  " + (systemEndTime - systemTime) + "  SA  " + server_isn + "  0  " + client_isn + "\n");
		} else {
			System.out.println("Three handshake failed.");
		}
		while (!fields.substring(0, 3).equals("001")) {
			try {
				socket.receive(request);
				fields = fetchData(request);
			} catch (SocketTimeoutException e) {
				socket.send(request);
				continue;
			}
		}

		systemEndTime = System.currentTimeMillis();
		logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  A  " + Integer.parseInt(fields.substring(3, 13))
				+ "  0  " + Integer.parseInt(fields.substring(13, 23)) + "\n");

		if (fields.substring(0, 3).equals("001")) {
			client_isn_second = Integer.parseInt(fields.substring(3, 13));
			server_isn_second = Integer.parseInt(fields.substring(13, 23));
			if (client_isn_second == client_isn && server_isn_second == server_isn + 1) {
				System.out.println("Three hand-shake finished, TCP established, Redeay to receive data");
				return true;
			} else {
				System.out.println("Three handshake failed.");
				return false;
			}

		} else {
			System.out.println("Three handshake failed.");
			return false;
		}

	}

	public static void closeTCP(DatagramSocket socket, boolean ifThreeHandShake, String fields,
			FileWriter logFileWriter) throws Exception {
		try {
			socket.setSoTimeout(500);
			if (ifThreeHandShake) {
				DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
				if (fields.substring(0, 3).equals("010")) {
					byte[] buf = ("001" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
					DatagramPacket reply = new DatagramPacket(buf, buf.length, senderHost, senderPort);
					socket.send(reply);
					// Thread.sleep(1000);
					buf = ("010" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
					reply = new DatagramPacket(buf, buf.length, senderHost, senderPort);
					socket.send(reply);
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  FA  " + "  0  0  0  " + "\n");
					while (!fields.substring(0, 3).equals("001")) {
						try {
							socket.receive(request);
							fields = fetchData(request);
						} catch (SocketTimeoutException e) {
							buf = ("001" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
							reply = new DatagramPacket(buf, buf.length, senderHost, senderPort);
							socket.send(reply);
							// Thread.sleep(1000);
							buf = ("010" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
							reply = new DatagramPacket(buf, buf.length, senderHost, senderPort);
							socket.send(reply);
							continue;
						}
					}

					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  A  " + "  0  0  0  " + "\n");
					fetchData(reply);

					if (fields.substring(0, 3).equals("001")) {
						socket.close();
						System.out.println("TCP disconnected");
					} else {
						System.out.println("TCP connection wrong!");
					}
				} else {
					System.out.println("TCP connection wrong!");
				}
			} else {
				System.out.println("There is no TCP connection to destroy.");
			}
		} catch (Exception e) {
			// If there is timeout, the client will print "Reply not
			// received."
			System.out.println("There are some problems in TCP disconnection.");
		}

	}

	public static int countByte(byte[] b) {
		int length = 0;
		for (int i = 0; i < b.length; i++) {
			if (b[i] != 0) {
				length++;
			}
		}
		return length;
	}

	private static String fetchData(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		byte[] buf = request.getData();
		String str = new String(buf);
		return str;
	}

	public static boolean isNull(byte[] b) {
		// check if a byte array is null
		int leng = 0;
		for (int i = 0; i < b.length; i++) {
			if (b[i] == 0) {
				leng++;
			}
		}
		if (leng == b.length) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) throws Exception {
		int totalAmountData = 0;
		int numRcvPac = 0;
		int numDupSeg = 0;
		String log_file = "./Receiver_log.txt";
		FileWriter logFileWriter = new FileWriter(log_file);
		byte[][] receivedPackets = new byte[1024][MSS];
		boolean ifThreeHandShake = false;
		int accACK = 0;
		// Get command line argument.
		if (args.length != 2) {
			System.out.println("Required arguments: port");
			return;
		}
		DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));
		DatagramPacket data = new DatagramPacket(new byte[1024], 1024);
		// Establish TCP Connection
		ifThreeHandShake = threeHandShake(socket, logFileWriter);
		String file = args[1];
		FileWriter fileWriter = new FileWriter(file);
		// Data receiving
		System.out.println("Data receiving.....");
		while (true) {
			data = new DatagramPacket(new byte[1024], 1024);
			socket.receive(data);
			String fields = fetchData(data);
			if (fields.substring(0, 3).equals("000")) {
				systemEndTime = System.currentTimeMillis();
				logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  D  "
						+ Integer.parseInt(fields.substring(3, 13)) + "  " + countByte(fields.substring(23).getBytes())
						+ "  " + Integer.parseInt(fields.substring(13, 23)) + "\n");
			}
			if (fields.substring(0, 3).equals("010")) {
				systemEndTime = System.currentTimeMillis();
				logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  F  "
						+ Integer.parseInt(fields.substring(3, 13)) + "  " + countByte(fields.substring(23).getBytes())
						+ "  " + Integer.parseInt(fields.substring(13, 23)) + "\n");
			}

			if (Integer.parseInt(fields.substring(3, 13)) == 800) {
				System.out.println("Received seq 800 the current accACK is " + accACK);
			}
			// System.out.println(accACK + " "
			// +Integer.parseInt(fields.substring(3, 13)) + " " +
			// fields.substring(23));
			if (fields.substring(0, 3).equals("000")) {
				if (accACK == Integer.parseInt(fields.substring(3, 13))) {
					if (countByte(receivedPackets[0]) != 0) {
						numDupSeg++;
					}
					receivedPackets[0] = fields.substring(23).getBytes();
					totalAmountData += countByte(receivedPackets[0]);
					numRcvPac++;
					// check which ack should be sent back
					int numberOfUnackInByte = 0;
					int numberOfAckByte = 0;
					for (int i = 0; i < receivedPackets.length; i++) {
						if (isNull(receivedPackets[i]) == false) {
							numberOfUnackInByte += countByte(receivedPackets[i]);
							numberOfAckByte++;
						} else {
							break;
						}
					}
					if (Integer.parseInt(fields.substring(3, 13)) == 750) {
						System.out.println("aaaaaaa:  " + numberOfUnackInByte);
					}
					accACK += numberOfUnackInByte;
					byte[] buf = ("000" + String.format("%010d", 0) + String.format("%010d", accACK)).getBytes();
					// System.out.println(accACK + " 1");
					data = new DatagramPacket(buf, buf.length, senderHost, senderPort);
					socket.send(data);
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  A  0  " + "  0  " + accACK + "\n");
					for (int i = 0; i < receivedPackets.length; i++) {
						if (isNull(receivedPackets[i]) == false) {
							System.out.print(new String(receivedPackets[i]));
							byte[] output = new byte[countByte(receivedPackets[i])];
							for (int j = 0; j < output.length; j++) {
								output[j] = receivedPackets[i][j];
							}
							fileWriter.write(new String(output));
						} else {
							break;
						}
					}
					byte[][] tempReceivedPackets = new byte[1024][MSS];
					System.arraycopy(receivedPackets, numberOfAckByte, tempReceivedPackets, 0,
							receivedPackets.length - numberOfAckByte);
					System.arraycopy(tempReceivedPackets, 0, receivedPackets, 0, receivedPackets.length);
					// System.out.println(fields.substring(23) + " " + accACK);

				} else if (accACK > Integer.parseInt(fields.substring(3, 13))) {
					numDupSeg++;
					byte[] buf = ("000" + String.format("%010d", 0) + String.format("%010d", accACK)).getBytes();
					data = new DatagramPacket(buf, buf.length, senderHost, senderPort);
					socket.send(data);
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  A  0  " + "  0  " + accACK + "\n");
					// System.out.println(accACK + " 3");
					// System.out.println(fields.substring(23) + " " + accACK);
				} else if (accACK < Integer.parseInt(fields.substring(3, 13))) {
					// Buffer the out-of-order packet
					if (countByte(receivedPackets[(Integer.parseInt(fields.substring(3, 13)) - accACK) / MSS]) == 0) {
						receivedPackets[(Integer.parseInt(fields.substring(3, 13)) - accACK) / MSS] = fields
								.substring(23).getBytes();
						totalAmountData += countByte(
								receivedPackets[(Integer.parseInt(fields.substring(3, 13)) - accACK) / MSS]);
						numRcvPac++;
					} else {
						numDupSeg++;
						receivedPackets[(Integer.parseInt(fields.substring(3, 13)) - accACK) / MSS] = fields
								.substring(23).getBytes();
					}
					System.out.println((Integer.parseInt(fields.substring(3, 13)) - accACK) / MSS);
					// for(int i = 0; i < 10; i++){
					// System.out.print(new String(receivedPackets[i]));
					// }
					System.out.println();
					// System.out.println(fields.substring(23));
					// System.out.println("buffer: " + new
					// String(receivedPackets[0]) + " "+
					// new String(receivedPackets[1]) + " "+
					// new String(receivedPackets[2]) + " "+
					// new String(receivedPackets[3]) + " "+
					// new String(receivedPackets[4]) + " ");
					byte[] buf = ("000" + String.format("%010d", 0) + String.format("%010d", accACK)).getBytes();
					data = new DatagramPacket(buf, buf.length, senderHost, senderPort);
					socket.send(data);
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  A  0  " + "  0  " + accACK + "\n");
					// System.out.println(accACK + " 2");
					// System.out.println(fields.substring(23) + " " + accACK);
				}
			}

			if (fields.substring(0, 3).equals("010")) {
				// Destroy TCP connection
				System.out.println();
				closeTCP(socket, ifThreeHandShake, fields, logFileWriter);
				logFileWriter.write("Amount of Data Received (in bytes): " + totalAmountData + "\n");
				logFileWriter.write("Number of Data Segments Received: " + numRcvPac + "\n");
				logFileWriter.write("Number of Duplicate Segments Received(if any): " + numDupSeg + "\n");
				fileWriter.close();
				logFileWriter.close();
				break;
			}
		}
	}
}
