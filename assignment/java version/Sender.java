import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class Sender extends Thread {
    public volatile boolean stop=false;
    // Variables from supplied file
    static ArrayList<String> linesToSend;
    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static DatagramSocket senderSocket;
    static ReentrantLock syncLock = new ReentrantLock();

    // seq and ack variables
    static int sender_seq;
    static int sender_ack;
    static int seq_receiver;
    static int ack_receiver;
    static String segment, flags, payload;
    static int triple_counter = 0;
    static String file = "";
    // Variables for common line
	static InetAddress receiver_host_ip;
	static int receiver_port; 
	static String fileName;
	static int MWS;
	static int MSS;
	static int timeout;
	static Double pdrop;
	static int seed;
    // Constants
    // flag string "SAFD", "1000" as syn, "1100" as syn ack
    static final int PORT_MIN = 1024;
    static final int PORT_MAX = 65535;
    static final int PDROP_MIN = 0;
    static final int PDROP_MAX = 1;
    static int ACK_INIT = 0;

    static Random random;
    // Variables to store header
    static byte[] sData;
    static byte[] rData;
	static DatagramPacket sPacket;
	static DatagramPacket rPacket;
    static String[] response;

    static int LastByteSent;
    static int LastByteAcked;
    // Variables to write log file
    static List<String[]> log_records = new ArrayList<String[]>();
    static int file_size;
    static int nSegments;
    static int nSegments_no_retrans;
    static int nDropped;
    static int nSegments_retrans;
    static int duplicate_ack;
    static long start_time;
    static boolean first = true;
    public static void main(String[] args) throws Exception {
        // checking the number of cmd line argument
        if (args.length != 8) {
            System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + " <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>");
            return;
        }

        receiver_host_ip = InetAddress.getByName(args[0]);
        receiver_port = Integer.parseInt(args[1]); // to match receiver port
        fileName = args[2]; // no error checking
        MWS = Integer.parseInt(args[3]); // maximum window size
		MSS = Integer.parseInt(args[4]); // maximum segment size
        timeout = Integer.parseInt(args[5]); //  in milliseconds
        pdrop = Double.parseDouble(args[6]);
        if (pdrop < PDROP_MIN || pdrop > PDROP_MAX){
            System.out.println("Pdrop value must be between 0 and 1...");
            return;
        }
        seed = Integer.parseInt(args[7]);
        random = new Random(seed);

        // starting...
        start_time = System.currentTimeMillis();
        three_way_connection(receiver_host_ip, receiver_port);

        nSegments = 0;
        nDropped = 0;
        nSegments_retrans = 0;
        readFile();
        send(); // sending file
        File f = create_sender_log();
        write_sender_log(f);
        connection_close(sender_seq, sender_ack);
        System.out.println("Connection closed");
    }

    // 3-way connection setup(SYN,SYNACK,ACK)
    public static void three_way_connection(InetAddress receiver_host_ip, int receiver_port) throws Exception{
        // establish socket
        int sender_seq_isn, sender_ack_isn;
        sender_seq_isn = 0;
        sender_ack_isn = 0;
        senderSocket = new DatagramSocket();
        // stage 1
        flags = "0100"; // PTP segment = "seq,ack,FSAD,MWS,payload"
        payload = "";
        segment = makeSegment(sender_seq_isn, sender_ack_isn, flags, payload);
        sendSegment(segment);
        write_to_log("snd", getTime(), makeFlag(flags), sender_seq_isn, payload.length(), sender_ack_isn);
        
        // stage 2
	    response = readSegment(senderSocket);
	    seq_receiver = Integer.parseInt(response[0]);
	    ack_receiver = Integer.parseInt(response[1]);
        flags = response[2];
        payload = response[3];
        write_to_log("rcv", getTime(), flags, seq_receiver, payload.length(), ack_receiver);
        System.out.println("Received Handshake Response");

        // stage 3
        sender_seq = ack_receiver;
        sender_ack = seq_receiver + 1;
        flags = "0010";
        payload = "";
        segment = makeSegment(sender_seq, sender_ack, flags, payload);
        sendSegment(segment);
        write_to_log("snd", getTime(), makeFlag(flags), sender_seq, payload.length(), sender_ack);
        System.out.println("Connection established......");
        // seq and ack to be carried over to sending phase
    }
    
    // aux function to process file
	public static void readFile() throws IOException,FileNotFoundException{
		linesToSend = new ArrayList<String>();
        file_size = (int)(new File(fileName)).length();
        FileReader f = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(f);
		String getLine = reader.readLine();
		String file = "";
		while(getLine != null){
			file += getLine + "\n"; // add new line to each line
			getLine = reader.readLine();
		}
		reader.close();
		// divide files into MSS per string and store in the array list
		while(file.length() > MSS) {
			getLine = file.substring(0, MSS);
			linesToSend.add(getLine);
			file = file.substring(MSS, file.length());
		}
		linesToSend.add(file);
	}

    public static void send() throws Exception {
        // starting a thread
        Sender receive_thread = new Sender();
        receive_thread.start();
        while(true) {
            syncLock.lock();
            response = readSegment(senderSocket);
            seq_receiver = Integer.parseInt(response[0]);
            ack_receiver = Integer.parseInt(response[1]);
            flags = response[2];
            payload = response[3];
            System.out.println("test: " + seq_receiver + "," + ack_receiver + "," + flags);
            write_to_log("rcv", getTime(), flags, seq_receiver , payload.length(), ack_receiver);
            if (ack_receiver < LastByteSent) {
                // out of order packet
                sender_seq = LastByteSent;
                triple_counter++;
                if (triple_counter == 3) {
                    System.out.println("triple" + triple_counter);
                    sender_seq = LastByteAcked;
                }
            } else {
                // expected ack received
                LastByteAcked = ack_receiver;
                sender_seq = ack_receiver;
            }
            sender_ack = seq_receiver;
            
            TimerTask task= new TimerTask() {
                public void run() {
                    if(LastByteAcked != LastByteSent) { // if this is a retransmission pack
                        try {
                            nSegments_retrans += 1;
                            segment = makeSegment(ack_receiver, seq_receiver, flags, linesToSend.get(ack_receiver/MSS));
                            sendSegment(segment);
                            write_to_log("snd", getTime(), "D", ack_receiver, linesToSend.get(ack_receiver/MSS).length(), seq_receiver); 
                        } catch (Exception e) {
                            
                        }
                    } else {
                        cancel(); // is this right??
                    }
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, timeout);
            syncLock.unlock();
        }
    }
        // We will send from this thread
    public void run() {
        syncLock.lock();
        for (int i = 0; i < linesToSend.size(); i++) {
            System.out.println("debug line" + sender_seq);
            LastByteSent = sender_seq;
            LastByteAcked = sender_seq;
            flags = "0001";
            segment = makeSegment(sender_seq, sender_ack, flags, linesToSend.get(i));
            sData = segment.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
            LastByteSent = Math.min(LastByteSent + linesToSend.get(i).length(), LastByteSent + MSS);
            // sending to PL module
            if(random.nextFloat() > pdrop) {
                try {
                    senderSocket.send(sendPacket);
                } catch(Exception e){

                }
                write_to_log("snd", getTime(), makeFlag(flags), sender_seq, linesToSend.get(i).length(), sender_ack);             
            } else {
                sender_seq = LastByteSent;
                write_to_log("drop", getTime(), makeFlag(flags), sender_seq, linesToSend.get(i).length(), sender_ack);  
                nDropped++;
                i++;
                continue;
            }
        
        } //run ends
        syncLock.unlock();
    }



    // 4-way connection termination(FIN,ACK+FIN,ACK)
    public static void connection_close(int seq1, int ack1) throws SocketException, IOException{
        String flags = "1000";
        payload = "";
        String segment = makeSegment(seq1, ack1, flags, payload); // no payload
        sendSegment(segment);
        write_to_log("snd", getTime(), makeFlag(flags), seq1, payload.length(), ack1);
        // receiver gets end request and sends ack + fin
        try {
            response = readSegment(senderSocket);
            int seq2 = Integer.parseInt(response[0]);
            int ack2 = Integer.parseInt(response[1]);
            flags = response[2];
            payload = response[3];
            write_to_log("rcv", getTime(), flags, seq2, payload.length(), ack2);

            // connection fully closed
            int seq3 = ack2;
            int ack3 = seq2 + 1;
            flags = "0010";
            payload = "";
            segment = makeSegment(seq3, ack3, flags, payload);
            sendSegment(segment);
            write_to_log("snd", getTime(), makeFlag(flags), seq3, payload.length(), ack3);
            System.out.println("Connection is now Closed!");
        } catch (Exception e) {
            System.out.println("Unexpected error when closing connection......");
        }

    }

    public static long getTime(){
		return System.currentTimeMillis() - start_time;
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

    public static String makeSegment(int seq, int ack, String flags, String payload){
        String flag = makeFlag(flags);
        String result = seq + "|" + ack + "|" + flag + "|" + payload + "|";
        return result;
    }

	public static void sendSegment(String segment) throws IOException{
		sData = segment.getBytes();
		sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
		senderSocket.send(sPacket);
	}

    public static String[] readSegment(DatagramSocket senderSocket) throws Exception{
		byte[] segment = new byte[1024];
		DatagramPacket segmentPacket = new DatagramPacket(segment, segment.length);
		senderSocket.receive(segmentPacket);
		String[] result = new String(segmentPacket.getData()).split("\\|");
		return result;
	}
    
    // store each row in the log record array
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

    // create sender log
    public static File create_sender_log() throws Exception {
        File f = new File("Sender_log.txt");
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
		}catch(Exception e){  
			e.printStackTrace();  
		}
        return f;
    }

    // write records to sender_log.txt
    public static void write_sender_log(File f) throws FileNotFoundException, IOException{
        FileOutputStream oStream = new FileOutputStream(f);        
        String row = "";
        for(int i = 0; i < log_records.size(); i++) {
            if(log_records.get(i)[0].equals("snd"))
                nSegments += 1;
            row = log_records.get(i)[0] + "\t" + log_records.get(i)[1] + "\t" + log_records.get(i)[2] + 
                "\t" + log_records.get(i)[3] + "\t" + log_records.get(i)[4] + "\t" + log_records.get(i)[5] + "\n";
            oStream.write(row.getBytes());
        }
        String summary1="Amount of Data Transferred (in bytes): " + file_size + "\n";
		String summary2="Number of Data Segments Sent (excluding retransmissions): " + nSegments_no_retrans + "\n";
		String summary3="Number of Packets Dropped: "+ nDropped + "\n";
		String summary4="Number of Retransmitted Segments: " + nSegments_retrans + "\n";
		String summary5="Number of Duplicate Acknowledgements received: " + duplicate_ack + "\n";
        oStream.write(summary1.getBytes());
        oStream.write(summary2.getBytes());
        oStream.write(summary3.getBytes());
        oStream.write(summary4.getBytes());
        oStream.write(summary5.getBytes());
        oStream.close();
    }
}