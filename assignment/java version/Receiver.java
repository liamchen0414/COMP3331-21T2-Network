
import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
	static long seed = System.currentTimeMillis();
	static int server_isn = new Random(seed).nextInt(2048);
	static DatagramSocket receiverSocket;
	static int seq_receiver;
	static int ack_receiver;
	static int seq_sender;
	static int ack_sender;
	static List<String[]> log_records = new ArrayList<String[]>();
	static InetAddress senderIPAddress;
	static int senderPort;
	static byte[] sData = null;
	static byte[] rData = null;
	static DatagramPacket sPacket;
	static DatagramPacket rPacket;

	// summary variable
	static File sender_log;
	static int data_transferred;
	static int nSegments;
	static int nDuplicate;
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
		long requestTime = 0;

		// some variables to store data from sender
        String[] line = null;
		String payload = null;
		String message = null;
		int receiveWindow = 0;
		String flags = null; // PTP flag design F|S|A|D
		
		// 1. Connection setup between sender and receiver, listening state
		// Three way handshake
		System.out.println("Waiting for sender to connect.....");
		while(true) {
			rData = new byte[1024];
			DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
			receiverSocket.receive(rPacket);
			requestTime = System.currentTimeMillis() - start_time;
			String[] request = new String(rPacket.getData()).split("\\|");
			sender_log = create_log_file();
			seq_sender = Integer.parseInt(request[0]);
			ack_sender = Integer.parseInt(request[1]);
			flags = request[2];
			// stage 1, SYN received
			if (flags.equals("S"))
			{
				receiveWindow = Integer.parseInt(request[3]);
				write_to_log("rcv", requestTime, "S", seq_sender, request[4].length(), ack_sender);
				// need IP and Port to send replies later
				senderIPAddress = rPacket.getAddress();
				senderPort = rPacket.getPort();

				// stage 2, sending SYNACK
				seq_receiver = server_isn;
				ack_receiver = (seq_sender + 1);
				payload = "";
				flags = "0110";
				message = makeHeader(seq_receiver, ack_receiver, flags, receiveWindow, payload);
				sData = message.getBytes();
				DatagramPacket sPacket = new DatagramPacket(sData, sData.length, senderIPAddress, senderPort);
				receiverSocket.send(sPacket);
				requestTime = System.currentTimeMillis() - start_time;
				write_to_log("snd", requestTime, makeFlag(flags), seq_receiver, payload.length(), ack_receiver);
			}
			// stage 3, ack
			if (flags.equals("A"))
			{
				write_to_log("rcv", requestTime, "A", seq_sender, 0, ack_sender);
				System.out.println("Connection established for " + senderIPAddress + ".....");
				break;
			}
		}

		System.out.println("File transfer initiating.....Receiving");
		// 2. Data Transmission (repeat until end of file)
		Map<Integer, String> buffer = new HashMap<Integer, String>();
		File f = new File(filename);
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
		} catch(Exception e) {
			e.printStackTrace();  
		}

		while(true) {
			// a. Receive PTP segment
			rData = new byte[1024];
			DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
			receiverSocket.receive(rPacket);
			requestTime = System.currentTimeMillis() - start_time;
			line = new String(rPacket.getData()).stripTrailing().split("\\|");
			seq_sender = Integer.parseInt(line[0]);
			ack_sender = Integer.parseInt(line[1]);
			flags = line[2];
			payload = line[4];
			// Receving a packet, if it is data packet
			if (flags.equals("D")){
				if (seq_sender == ack_receiver) {
					// b. two scenarios, 1 is normal packet, 2 is retransmission
					receiveFile(f, payload);
					write_to_log("rcv", requestTime, "D", seq_sender, payload.length(), ack_sender);

					seq_receiver = ack_sender;
					ack_receiver = seq_sender + payload.length();
					payload = "";
					// send ack back to sender
					message = makeHeader(seq_receiver, ack_receiver, "A", receiveWindow, payload);
					send_message(message);
					requestTime = System.currentTimeMillis() - start_time;
					write_to_log("snd", requestTime, "A", seq_receiver, 0, ack_receiver);
				} else if (seq_sender > ack_receiver) {
					System.out.println("Dectecting out of order packet.....");
					// 1. put the out of order packet in buffer
					buffer.put(seq_sender, payload);
					// 2. send the corresponding ack
					message = makeHeader(seq_receiver, ack_receiver, "0010", receiveWindow, payload);
					send_message(message);
					write_to_log("snd", requestTime, "A", seq_receiver, 0, ack_receiver);
				} else {
					System.out.println("Debug: " + seq_sender + ack_receiver);
				}
			} else if (flags.equals("F") && seq_sender == ack_receiver) {
				// if the receiver gets FINbit and seq_sender is same as current ack,
				write_to_log("rcv", requestTime, "F", seq_sender, 0, ack_sender);
				seq_receiver = ack_sender;
				ack_receiver = seq_sender + 1;
				// sending FINACK "1010" = FA
				flags = "1010";
				message = makeHeader(seq_receiver, ack_receiver, flags, receiveWindow, payload);
				send_message(message);
				requestTime = System.currentTimeMillis() - start_time;
				write_to_log("snd", requestTime, "FA", seq_receiver, 0, ack_receiver);
			} else if (flags.equals("A")) {
				// this else if is only for connection close period
				// ACK is received from receiver, tear down connection
				write_to_log("rcv", requestTime, "A", seq_sender, 0, ack_sender);
				break;
			} else {
				System.out.println("Unexpected error, aborting...");
				System.exit(1);
			}
		}
		// 	3. Connection teardown
		receiverSocket.close();
		write_receiver_log(sender_log);
	}

	public static void write_to_log(String status, long time, String type_of_packet, int seq, int length, int ack){
        String[] record = new String[6];
        record[0] = status;
        record[1] = Long.toString(time);
        record[2] = type_of_packet;
        record[3] = Integer.toString(seq);
        record[4] = Integer.toString(length);
        record[5] = Integer.toString(ack);
        log_records.add(record);
        System.out.println(status + ", " + time + ", " + type_of_packet + ", " + seq + ", " + length + ", " + ack);
    }

	public static void send_message(String message) throws IOException{
		sData = message.getBytes();
		sPacket = new DatagramPacket(sData, sData.length, senderIPAddress, senderPort);
		receiverSocket.send(sPacket);
	}

	public static String makeFlag(String flags) {
        String flag;
        if(flags.equals("0100"))
            flag = "S";
        else if(flags.equals("0010")){
            flag = "A";
        } else if(flags.equals("1000")){
            flag = "F";
        } else if(flags.equals("0001")){
            flag = "D";
        } else if(flags.equals("1010")){
            flag = "FA";
        } else if(flags.equals("0110")){
            flag = "SA";
        } else {
            flag = "ERROR";
        }
        return flag;
    }

    public static String makeHeader(int seq, int ack, String flags, int receiveWindow, String payload){
        String flag = makeFlag(flags);
        String result = seq + "|" + ack + "|" + flag + "|" + receiveWindow + "|" + payload;
        return result;
    }

    public static File create_log_file() throws Exception {
        File f = new File("Receiver_log.txt");
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
		}catch(Exception e){  
			e.printStackTrace();  
		}
		return f;
    }

    public static void receiveFile(File f, String payload) throws FileNotFoundException, IOException{
		try{
            FileWriter fw = new FileWriter(f, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(payload);
			bw.close();
		} catch(Exception e) {  
			e.printStackTrace();  
		}
    }

    public static void write_receiver_log(File f) throws FileNotFoundException, IOException{
        FileOutputStream oStream = new FileOutputStream(f);        
        String row = "";
        for(int i = 0; i < log_records.size(); i++) {
            if(log_records.get(i)[0].equals("snd"))
                nSegments += 1;
            row = log_records.get(i)[0] + "\t" + log_records.get(i)[1] + "\t" + log_records.get(i)[2] + 
                "\t" + log_records.get(i)[3] + "\t" + log_records.get(i)[4] + "\t" + log_records.get(i)[5] + "\n";
            oStream.write(row.getBytes());
        }
        // String summary1="Amount of (original) Data Received (in bytes): " + data_transferred + "\n";
		// String summary2="Number of (original) Data Segments Received: " + nSegments + "\n";
		// String summary3="Number of duplicate segments received (if any): "+ nDuplicate + "\n";
        // oStream.write(summary1.getBytes());
        // oStream.write(summary2.getBytes());
        // oStream.write(summary3.getBytes());
        oStream.close();
    }
}