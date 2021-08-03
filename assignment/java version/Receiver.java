import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {
	static long seed = System.currentTimeMillis();
	static int server_isn = 0;
	static DatagramSocket receiverSocket;
	static int seq_receiver;
	static int ack_receiver;
	static int seq_sender;
	static int ack_sender;
	static int ack_out;
	static List<String[]> log_records = new ArrayList<String[]>();
	static InetAddress senderIPAddress;
	static int senderPort;
	static byte[] sData = null;
	static byte[] rData = null;
	static DatagramPacket sPacket;
	static DatagramPacket rPacket;
	static long start_time;

	// summary variable
	static File log;
	static int data_transferred;
	static int nSegments;
	static int nDuplicate;

	PriorityQueue<Map.Entry<Integer, String>> queue = new PriorityQueue<>();

	public static void main(String[] args) throws Exception {
		// check arguments
		if (args.length != 2) {
		    System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + "<receiver_port> <FileReceived.txt>");
            System.exit(1);
        }
		// assign variables from cmd line arguments
	    int port = Integer.parseInt(args[0]);
	    String filename = args[1];

		// server setup
	    receiverSocket = new DatagramSocket(port);
		System.out.println("Server is ready: ");
		start_time = System.currentTimeMillis(); // starting time
		log = create_log_file();
		// some variables to store data from sender
        String[] line = null;
		String payload = null;
		String segment = null;
		String flags = null; // PTP flag design F|S|A|D
		// 1. Connection setup between sender and receiver, listening state
		// Three way handshake
		System.out.println("Waiting for sender to connect.....");
		boolean handshake = true;
		while(handshake) {
			// receive SYN from sender, decode segment
			byte[] initial = new byte[1024];
			DatagramPacket rPacket = new DatagramPacket(initial, initial.length);
			receiverSocket.receive(rPacket);
			line = new String(rPacket.getData()).split("\\|");
			seq_sender = Integer.parseInt(line[0]);
			ack_sender = Integer.parseInt(line[1]);
			flags = line[2];
			// SYN received
			if (flags.equals("S"))
			{
				write_to_log("rcv", 0, "S", seq_sender, 0, ack_sender);
				// need IP and Port to send replies later
				senderIPAddress = rPacket.getAddress();
				senderPort = rPacket.getPort();
				// prepare receiver information and send SYNACK
				seq_receiver = server_isn;
				ack_receiver = (seq_sender + 1);
				flags = "0110";
				payload = "";
				// make segment and send
				segment = makeSegment(seq_receiver, ack_receiver, makeFlag(flags), payload);
				sendSegment(segment);
				write_to_log("snd", getTime(), makeFlag(flags), seq_receiver, payload.length(), ack_receiver);
			}
			// stage 3, ack
			if (flags.equals("A"))
			{
				write_to_log("rcv", getTime(), "A", seq_sender, 0, ack_sender);
				System.out.println("Connection established for " + senderIPAddress + ".....");
				handshake = false;
			}
		}

		System.out.println("File transfer initiating.....Receiving");
		// 2. Data Transmission (repeat until end of file)
		File f = new File(filename);
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
		} catch(Exception e) {
			e.printStackTrace();  
		}
		Map<Integer, String> buffer = new HashMap<Integer, String>();
		boolean transfer = true;
		while(transfer) {
			// a. Receive PTP segment
			line = readSegment(receiverSocket);
			seq_sender = Integer.parseInt(line[0]);
			ack_sender = Integer.parseInt(line[1]);
			flags = line[2];
			payload = line[3];
			// if it is data packet
			if (flags.equals("D")){
				if (seq_sender == ack_receiver) {
					// b. two scenarios, 1 is normal packet, 2 is retransmission
					if(buffer.containsKey(ack_receiver + payload.length())){
						// ack the current packet
						write_to_log("rcv", getTime(), "D", seq_sender, payload.length(), ack_sender);
						while(buffer.containsKey(ack_receiver + payload.length())) {
							// get packets from buffer and write to file
							String value = buffer.get(ack_receiver + payload.length());
							writeFile(f, value);
							// update ack_receiver and remove entry from buffer
							ack_receiver += payload.length();
							seq_sender = seq_sender + payload.length();
							System.out.println("Moving packet + " + seq_sender + " with length +" + payload.length());
							buffer.remove(seq_sender);
							
						}
						ack_receiver += payload.length();
						segment = makeSegment(seq_receiver, ack_receiver, "A", "");
						sendSegment(segment);
						write_to_log("snd", getTime(), "A", seq_receiver, 0, ack_receiver);
					} else {
						write_to_log("rcv", getTime(), "D", seq_sender, payload.length(), ack_sender);
						writeFile(f, payload); // write the payload to the received file
						seq_receiver = ack_sender;
						ack_receiver = seq_sender + payload.length(); // add payload length
						flags = "0010";
						payload = "";
						// send ack back to sender
						segment = makeSegment(seq_receiver, ack_receiver, makeFlag(flags), payload);
						sendSegment(segment);
						write_to_log("snd", getTime(), makeFlag(flags), seq_receiver, payload.length(), ack_receiver);
					}

				} else if (seq_sender > ack_receiver) {
					System.out.println("Dectecting out of order packet....." + seq_sender);
					write_to_log("rcv", getTime(), "D", seq_sender, payload.length(), ack_sender);
					// 1. put the out of order packet in buffer
					buffer.put(seq_sender, payload);
					ack_out = seq_sender + payload.length();
					flags = "0010";
					payload = "";
					// 2. send the corresponding ack without updating receiver seq and ack
					segment = makeSegment(seq_receiver, ack_receiver, makeFlag(flags), payload);
					sendSegment(segment);
					write_to_log("snd", getTime(), makeFlag(flags), seq_receiver, payload.length(), ack_receiver);
				} else {
					System.out.println("Debug: " + seq_sender + " " + ack_receiver);
				}
			} else if (flags.equals("F")) {
				// if the receiver gets FINbit
				write_to_log("rcv", getTime(), flags, seq_sender, 0, ack_sender);
				seq_receiver = ack_sender;
				ack_receiver = seq_sender + 1;
				flags = "1010"; // FINACK
				segment = makeSegment(seq_receiver, ack_receiver, makeFlag(flags), "");
				sendSegment(segment);
				write_to_log("snd", getTime(), makeFlag(flags), seq_receiver, 0, ack_receiver);
			} else if (flags.equals("A")) {
				// this else if is only for connection close period
				// ACK is received from receiver, tear down connection
				write_to_log("rcv", getTime(), flags, seq_sender, 0, ack_sender);
				transfer = false;
				System.out.println("Connection close...");
			} else {
				System.out.println("Unexpected error, aborting...");
				System.exit(1);
			}
		}
		// 	3. Connection teardown
		receiverSocket.close();
		write_receiver_log(log);
	}
	// checked
	public static long getTime(){
		return System.currentTimeMillis() - start_time;
	}
	// checked
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

	// checked
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
	// checked
    public static String makeSegment(int seq, int ack, String flags, String payload) {
        return (seq + "|" + ack + "|" + flags + "|" + payload);
    }
	// checked
	public static void sendSegment(String segment) throws IOException{
		sData = segment.getBytes();
		sPacket = new DatagramPacket(sData, sData.length, senderIPAddress, senderPort);
		receiverSocket.send(sPacket);
	}
	// checked
	public static String[] readSegment(DatagramSocket receiverSocket) throws Exception{
		byte[] segment = new byte[1024];
		DatagramPacket segmentPacket = new DatagramPacket(segment, segment.length);
		receiverSocket.receive(segmentPacket);
		String[] result = new String(segmentPacket.getData()).split("\\|");
		return result;
	}
	// checked
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
	// checked
    public static void writeFile(File f, String payload) throws FileNotFoundException, IOException{
		try{
            FileWriter fw = new FileWriter(f, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(payload);
			bw.close();
		} catch(Exception e) {  
			e.printStackTrace();  
		}
    }
	// checked
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