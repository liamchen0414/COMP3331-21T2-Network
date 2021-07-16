import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 Server to process ping requests over UDP. 
 The server sits in an infinite loop listening for incoming UDP packets. 
 When a packet comes in, the server simply sends the encapsulated data back to the client.
 */

public class Sender {
	static long systemTime = System.currentTimeMillis();
	static long systemEndTime = -1;
	private static int MWS = 500;
	private static int MSS = 50;
	private static double LOSS_RATE = 0.3;
	private static int seed = 300;
	private static int timeout = 200;

	public static boolean threeHandShake(DatagramSocket socket, InetAddress receiverHost, int receiverPort,
			FileWriter logFileWriter) throws Exception {
		try {
			socket.setSoTimeout(timeout);
			String s = "";
			Random random = new Random();
			int client_isn = random.nextInt(2147483647);
			byte[] buf = ("100" + String.format("%010d", client_isn) + String.format("%010d", 0)).getBytes();
			DatagramPacket request = new DatagramPacket(buf, buf.length, receiverHost, receiverPort);
			fetchData(request);
			socket.send(request);
			systemEndTime = System.currentTimeMillis();
			logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  S  " + client_isn + "  0  0" + "\n");
			// Receive datagram
			DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
			String fields = null;
			
			while (fields == null || !fields.substring(0, 3).equals("101")) {
				try {
					socket.receive(reply);
					fields = fetchData(reply);
				} catch (SocketTimeoutException e) {
					socket.send(request);
					continue;
				}
			}
			
			systemEndTime = System.currentTimeMillis();
			logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  A  " + fields.substring(3, 13) + "  0  "
					+ fields.substring(13, 23) + "\n");
			if (fields.substring(0, 3).equals("101")) {
				int server_isn_receive = Integer.parseInt(fields.substring(3, 13));
				server_isn_receive++;
				int client_isn_receive = Integer.parseInt(fields.substring(13, 23));
				if (client_isn_receive - client_isn == 1) {
					byte[] buf__second = ("001" + String.format("%010d", client_isn_receive)
							+ String.format("%010d", server_isn_receive)).getBytes();
					DatagramPacket second_request = new DatagramPacket(buf__second, buf__second.length, receiverHost,
							receiverPort);
					socket.send(second_request);
					fetchData(second_request);
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  A  " + client_isn_receive + "  0  "
							+ server_isn_receive + "\n");
					System.out.println("Three hand shake has been finished.");
					return true;
				} else {
					System.out.println("Three handshake failed.aaa");
					return false;
				}
			} else {
				System.out.println("Three handshake failed.bbb");
				return false;
			}
		} catch (Exception e) {
			// If there is timeout, the client will print "Reply not
			// received."
			System.out.println(e.getMessage());
			System.out.println("Packet " + ":   Reply not recieved. Three hand shake failed.");
		}
		return false;
	}

	public static boolean closeTCP(DatagramSocket socket, InetAddress clientHost, int clientPort,
			FileWriter logFileWriter) throws Exception {
		String fields = "XXX";
		byte[] buf = ("010" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
		DatagramPacket request = new DatagramPacket(buf, buf.length, clientHost, clientPort);
		DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
		socket.send(request);
		systemEndTime = System.currentTimeMillis();
		logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  F  " + 0 + "  0  " + 0 + "\n");
		System.out.println("Enter FIN_WAIT_1 State");
		while (!fields.substring(0, 3).equals("001")) {
			try {
				socket.receive(reply);
				fields = fetchData(reply);
			} catch (SocketTimeoutException e) {
				socket.send(request);
				continue;
			}
		}
		if (fields.substring(0, 3).equals("001")) {
			System.out.println("Enter FIN_WAIT_2 State");
			while (!fields.substring(0, 3).equals("010")) {
				try{
					socket.receive(reply);
					fields = fetchData(reply);
				}catch (SocketTimeoutException e) {
					socket.send(request);
					continue;
				}
			}
			systemEndTime = System.currentTimeMillis();
			logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  FA  " + 0 + "  0  " + 0 + "\n");
			if (fields.substring(0, 3).equals("010")) {
				buf = ("001" + String.format("%010d", 0) + String.format("%010d", 0)).getBytes();
				request = new DatagramPacket(buf, buf.length, clientHost, clientPort);
				socket.send(request);
				systemEndTime = System.currentTimeMillis();
				logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  A  " + 0 + "  0  " + 0 + "\n");
				System.out.println("Enter Time-wait phase.");
				// Thread.sleep(1000);
				socket.close();
				System.out.println("TCP Connection released.");
				return true;
			} else if (fields.substring(0, 3).equals("000")) {
				return false;
			}
		} else if (fields.substring(0, 3).equals("000")) {
			return false;
		}
		System.out.println("TCP connection is not properly terminated.");
		return false;
	}

	/*
	 * Print ping data to the standard output stream.
	 */
	private static String fetchData(DatagramPacket request) throws Exception {
		// Obtain references to the packet's array of bytes.
		byte[] buf = request.getData();
		String str = new String(buf);
		return str;
	}

	public static void Timer(int millisec) {
		long ll;
		long l = System.currentTimeMillis();
		ll = l;
		System.out.println("Timer starte");
		while (true) {

			l = System.currentTimeMillis();
			long d = l - ll;
			if (d > 60000) {
				System.out.println("Time out!");
				break;
			}
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

	public static void main(String[] args) throws Exception {
		if (args.length != 8) {
			System.out.println("Not enough arguments.");
			return;
		}
		int totalAmountData = 0;
		int numSentPac = 0;
		int numDropPac = 0;
		int numRetrans = 0;
		int numDupAck = 0;
		int preAck = -1;
		int currentAck = -1;
		InetAddress receiverHost = InetAddress.getByName(args[0]);
		int receiverPort = Integer.parseInt(args[1]);
		MWS = Integer.parseInt(args[3]);
		MSS = Integer.parseInt(args[4]);
		LOSS_RATE = Float.parseFloat(args[6]);
		seed = Integer.parseInt(args[7]);
		timeout = Integer.parseInt(args[5]);
		String log_file = "./Sender_log.txt";
		FileWriter logFileWriter = new FileWriter(log_file);
		int[] fastRetrans = new int[4];
		for (int i = 0; i < fastRetrans.length; i++) {
			fastRetrans[i] = -1;
		}
		int fastRetransPackeNumber = 0;
		byte[][] window = new byte[MWS / MSS][MSS];
		Random random = new Random(seed);
		int NextSeqNum = 0;
		int SendBase = 0;
		int ack = -1;
		int unackPacket = 0;
		int timeoutConstant = timeout;
		int timeoutVariable = timeout;
		// Get command line argument.
		String file = args[2];
		InputStream input = new FileInputStream(file);
		boolean ifThreeHandShake = false;
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket ackPacket = new DatagramPacket(new byte[1024], 1024);
		String fields = null;
		// Establish TCP connection
		if (ifThreeHandShake == false) {
			ifThreeHandShake = threeHandShake(socket, receiverHost, receiverPort, logFileWriter);
		} else {
			System.out.println("The three hand shake is done.");
		}
		boolean receiveACK = true;
		String content = null;
		int numberOfPacketSent = MWS / MSS;
		boolean endOfFile = false;
		int fileENDSeq = -1;

		// Data sending
		if (ifThreeHandShake == true) {
			while (true) {

				// sending data
				if (receiveACK == true) {
					// System.out.println("aaaa");
					socket.setSoTimeout(timeoutConstant);
					for (int i = 0; i < numberOfPacketSent; i++) {
						byte[] byt = new byte[MSS];
						int flag = input.read(byt);
						// System.out.println((flag)+ " " + (endOfFile ==
						// false));
						if (flag == -1 && endOfFile == false) {

							endOfFile = true;
							fileENDSeq = NextSeqNum;
							break;
						}
						window[unackPacket + i] = byt;
						content = new String(byt);
						totalAmountData += countByte(byt);
						byte[] buf = ("000" + String.format("%010d", NextSeqNum) + String.format("%010d", 0) + content)
								.getBytes();
						DatagramPacket data = new DatagramPacket(buf, buf.length, receiverHost, receiverPort);
						// socket.send(data);
						// NextSeqNum = NextSeqNum + countByte(byt);
						if (random.nextDouble() > LOSS_RATE) {
							socket.send(data);
							numSentPac++;
							systemEndTime = System.currentTimeMillis();
							logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  D  " + NextSeqNum + "  "
									+ countByte(byt) + "  0" + "\n");
							// System.out.println("send data: " + content +
							// "Base: "
							// + SendBase);
							// Thread.sleep(1000);
						} else {
							System.out.println("dropped!!!");
							numDropPac++;
							systemEndTime = System.currentTimeMillis();
							logFileWriter.write("drop  " + (systemEndTime - systemTime) + "  D  " + NextSeqNum + "  "
									+ countByte(byt) + "  0" + "\n");
						}
						NextSeqNum = NextSeqNum + countByte(byt);
					}
					unackPacket = (NextSeqNum - SendBase) / MSS;
				}
				/**
				 * System.out.println("******************"); for(int k = 0; k <
				 * window.length; k++){
				 * 
				 * System.out.println(new String(window[k])); }
				 * System.out.println("******************");
				 **/
				try {
					// mark the time of starting waiting for ACK
					long startTimeStamp = System.currentTimeMillis();
					socket.receive(ackPacket);

					// mark the time of receiving ACK
					long endTimeStamp = System.currentTimeMillis();
					int duration = (int) (endTimeStamp - startTimeStamp);
					System.out.println("Ack received: " + fetchData(ackPacket).substring(13, 23));
					ack = Integer.parseInt(fetchData(ackPacket).substring(13, 23));
					currentAck = ack;

					if (currentAck == preAck) {
						numDupAck++;
					}
					preAck = currentAck;
					systemEndTime = System.currentTimeMillis();
					logFileWriter.write("rcv  " + (systemEndTime - systemTime) + "  A" + "  0  0  " + ack + "\n");
					// Fast retransmission mode

					fastRetrans[fastRetransPackeNumber] = ack;
					fastRetransPackeNumber++;
					if (fastRetransPackeNumber == 4) {
						if (fastRetrans[0] == fastRetrans[1] && fastRetrans[1] == fastRetrans[2]
								&& fastRetrans[2] == fastRetrans[3]) {
							for (int p = 0; p < fastRetrans.length; p++) {
								fastRetrans[p] = -1;
							}
							fastRetransPackeNumber = 0;
							System.out.println("Fast retransmission initiated.");
							throw new SocketTimeoutException();
						} else {
							for (int p = 0; p < fastRetrans.length - 1; p++) {
								fastRetrans[p] = fastRetrans[p + 1];
							}
							fastRetrans[fastRetrans.length - 1] = -1;
							fastRetransPackeNumber--;
						}
					}

					// System.out.println("aaaaa " + ack + " " + SendBase);
					if (ack > SendBase) {
						// if ack > SendBase, move SendBase
						numberOfPacketSent = (int) Math.ceil((((float) ack - SendBase) / MSS));
						System.out.println("numberOfPacketSent:  " + numberOfPacketSent);
						unackPacket = window.length - numberOfPacketSent;
						for (int j = 0; j < window.length - numberOfPacketSent; j++) {
							window[j] = window[j + numberOfPacketSent];
						}

						SendBase = ack;
						System.out.println("sendbase: " + SendBase + "ack:  " + ack);
						receiveACK = true;
						if (endOfFile && ack == fileENDSeq) {
							break;
						}
					} else {
						System.out.println("duplicated");
						// if replicated ACK received, do noting and keep timing
						timeoutVariable = timeoutVariable - duration;
						socket.setSoTimeout(timeoutVariable);
						receiveACK = false;
					}
				} catch (SocketTimeoutException e) {
					// System.out.println(ack +" "+fileENDSeq);
					// if timeout happens
					content = new String(window[0]);
					// System.out.println("retransmitting data: " + content);
					byte[] buf = ("000" + String.format("%010d", SendBase) + String.format("%010d", 0) + content)
							.getBytes();
					DatagramPacket data = new DatagramPacket(buf, buf.length, receiverHost, receiverPort);
					if (random.nextDouble() > LOSS_RATE) {
						System.out.println("retransmission sent");
						numRetrans++;
						if (endOfFile && ack == fileENDSeq) {
							break;
						}
						socket.send(data);
						systemEndTime = System.currentTimeMillis();
						logFileWriter.write("snd  " + (systemEndTime - systemTime) + "  D  " + SendBase + "  "
								+ countByte(window[0]) + "  0" + "\n");
					} else {
						systemEndTime = System.currentTimeMillis();
						logFileWriter.write("drop  " + (systemEndTime - systemTime) + "  D  " + SendBase + "  "
								+ countByte(window[0]) + "  0" + "\n");
						System.out.println("retransmission dropped");
						numDropPac++;
					}

					// System.out.println("time out.");
					receiveACK = false;
				}
			}
		}

		// TCP close
		socket.setSoTimeout(5000);
		if (ifThreeHandShake == true) {
			closeTCP(socket, receiverHost, receiverPort, logFileWriter);
			logFileWriter.write("Amount of Data Transferred (in bytes): " + totalAmountData + "\n");
			logFileWriter.write("Number of Data Segments Sent (excluding retransmissions): " + numSentPac + "\n");
			logFileWriter.write("Number of Packets Dropped (by the PLD module): " + numDropPac + "\n");
			logFileWriter.write("Number of Retransmitted Segments: " + numRetrans + "\n");
			logFileWriter.write("Number of Duplicate Acknowledgements received: " + numDupAck + "\n");
			logFileWriter.close();
		} else {
			System.out.println("The three hand shake is not established thus cannot be terminated.");
		}

	}
}

class SenderThread implements Runnable {
	public DatagramSocket socket = null;
	public InetAddress clientHost = null;
	public int clientPort;
	BufferedReader bufferReader = null;
	public static int NextSeqNum = 0;
	public static int SendBase = 0;

	SenderThread(DatagramSocket s, InetAddress c, int p, BufferedReader br) {
		socket = s;
		clientHost = c;
		clientPort = p;
		bufferReader = br;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			this.sendData();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendData() throws Exception {
		String content = bufferReader.readLine();
		System.out.println(content + "hahahahha");
		byte[] buf = ("000" + String.format("%010d", NextSeqNum) + String.format("%010d", 0) + content).getBytes();
		DatagramPacket data = new DatagramPacket(buf, buf.length, clientHost, clientPort);
		Timer timer = new Timer();
		// timer.schedule(new TestTimerTask(socket, data, SendBase, NextSeqNum),
		// 3000, 5000);
		NextSeqNum = NextSeqNum + content.length();
	}

}

class TestTimerTask extends TimerTask {
	DatagramPacket data = null;
	DatagramSocket socket = null;
	int sendbase;

	TestTimerTask(DatagramSocket s, DatagramPacket d, int sb) {
		socket = s;
		data = d;
		sendbase = sb;
	}

	@Override
	public void run() {
		this.retransmit();
	}

	public void retransmit() {
		try {
			socket.send(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("data resent.");
	}
}
