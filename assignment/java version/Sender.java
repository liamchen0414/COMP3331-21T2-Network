import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class Sender extends Thread {
    // Variables from supplied file
    static ArrayList<String> linesToSend;
    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static DatagramSocket senderSocket;
    static int UPDATE_INTERVAL = 1000;//milliseconds
    static ReentrantLock syncLock = new ReentrantLock();
    static int sender_seq;
    static int sender_ack;
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
    static String flags = "0000";
    static final int PORT_MIN = 1024;
    static final int PORT_MAX = 65535;
    static final int PDROP_MIN = 0;
    static final int PDROP_MAX = 1;
    static int SENDER_ISN = 0; // client_isn
    static int RECEIVER_ISN = 0; // server_isn
    static int ACK_INIT = 0;

    // Variables to store header

    // Variables to write log file
    static List<String[]> log_records = new ArrayList<String[]>();
    static int data_transferred;
    static int nSegments;
    static int nSegments_no_retrans;
    static int nDropped;
    static int nSegments_retrans;
    static int duplicate_ack;
    static long start_time;

    public static void main(String[] args) throws Exception {
        // checking the number of cmd line argument
        if (args.length != 8) {
            System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + " <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>");
            return;
        }

        // write common line arguments into variables with simple checks
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
        three_way_connection(receiver_host_ip, receiver_port);
        sendFile();
        create_sender_log();
        File f = new File("Sender_log.txt");
        write_sender_log(f);
        connection_close(0, 0);
    }

    // 3-way connection setup(SYN,SYN+ACK,ACK), bypassing PL module
    public static void three_way_connection(InetAddress receiver_host_ip, int receiver_port) throws SocketException, IOException{
        // establish socket
        senderSocket = new DatagramSocket();
        start_time = System.currentTimeMillis();
        int seq1, ack1;
        seq1 = SENDER_ISN;
        ack1 = ACK_INIT;
        // segment = "seq,ack,FSAD,payload"
        flags = "0100";
        String header = seq1 + "," + ack1 + "," + flags; // no payload
        byte[] sData = header.getBytes();
        DatagramPacket sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        long requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq1, 0, ack1);
        
        // sender receives connection granted, 2way
	    byte[] rData = new byte[1024];
	    DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
	    senderSocket.receive(rPacket);
	    requestTime = System.currentTimeMillis() - start_time;
	    String[] response = new String(rPacket.getData()).trim().split(",");
        // segment = "seq,ack,FSAD,payload"
	    int seq2 = Integer.parseInt(response[0]);
	    int ack2 = Integer.parseInt(response[1]);
        flags = response[2];
        write_log_record("rcv", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq2, 0, ack2);
        // sender sends final confirmation before sending data, 3way
        // syn3 = 0, seq3 = client_isn + 1 = 0, ack3 = server_isn + 1
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        flags = "0010";
        header = seq3 + "," + ack3 + "," + flags;
        sData = header.getBytes();
        sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq3, 0, ack3);
        System.out.println("Server is ready, connection established :");
        // seq and ack to be carried over to next phase
        sender_seq = seq3;
        sender_ack = ack3;
    }
    
    // aux function to process file
	public static void readFile() throws IOException,FileNotFoundException{
		linesToSend = new ArrayList<String>();
        FileReader f = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(f);
		String getLine = reader.readLine();
		String file = "";
		while(getLine != null){
			file += getLine + "\r\n"; // add new line to each line, TOCHECK carriage
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

    // data transmission (repeat until end of file)
    public static void sendFile() throws Exception {
        int window_start = 0;
        int window_end = 0;
        int counter = 0;
        nSegments = 0;
        nDropped = 0;
        nSegments_retrans = 0;
        boolean flag = true;

        // a. Read file
        readFile();
        String log = "";
        System.out.println("linesToSend size = "+ linesToSend.size());
        for(int i = 0; i < linesToSend.size(); i++) {
            byte[] sendData = linesToSend.get(i).getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiver_host_ip, receiver_port);
            senderSocket.send(sendPacket);
            // long send_time = System.currentTimeMillis() - start_time;
        }
        // System.out.println("Sender_seq = " + sender_seq + " Receiver_SeqACK = " + sender_ack);

    }




    // d. Send PTP segment to PL module
    // generating random number between 0 and 1 using seed
    Random random = new Random(seed);
    float rdrop = random.nextFloat();

    // if(rdrop < pdrop) don't send the packet

    // define PL module: simulation of packet loss


    // 4-way connection termination(FIN,ACK+FIN,ACK)
    public static void connection_close(int seq1, int ack1) throws SocketException, IOException{
        flags = "1000";
        String header = seq1 + "," + ack1 + "," + flags; // no payload
        byte[] sRequest = header.getBytes();
        DatagramPacket sPacket = new DatagramPacket(sRequest, sRequest.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        long requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq1, 0, ack1);

        // receiver gets end request and sends ack + fin
	    byte[] rRequest = new byte[1024];
	    DatagramPacket rPacket = new DatagramPacket(rRequest, rRequest.length);
	    senderSocket.receive(rPacket);
	    requestTime = System.currentTimeMillis() - start_time;
        String[] response = new String(rPacket.getData()).trim().split(",");
	    int seq2 = Integer.parseInt(response[0]);
	    int ack2 = Integer.parseInt(response[1]);
        flags = response[2];
        write_log_record("rcv", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq2, 0, ack2);

        // connection fully closed
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        flags = "0010";
        header = seq3 + "," + ack3 + "," + flags;
        byte[] sData = header.getBytes();
        sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq3, 0, ack3);
        System.out.println("Connection is now Closed!");
    }

    // store each row in the log record array
    public static void write_log_record(String status, double time, String type_of_packet, int seq, int length, int ack){
        String[] record = new String[6];
        record[0] = status;
        record[1] = Double.toString(time);
        record[2] = type_of_packet;
        record[3] = Integer.toString(seq);
        record[4] = Integer.toString(length);
        record[5] = Integer.toString(ack);
        log_records.add(record);
        // debug
        System.out.println(status + ", " + time + ", " + type_of_packet + ", " + seq + ", " + length + ", " + ack);
    }

    // create sender log
    public static void create_sender_log() throws Exception {
        File f = new File("Sender_log.txt");
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
            write_sender_log(f);
		}catch(Exception e){  
			e.printStackTrace();  
		}
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
        String summary1="Amount of Data Transferred (in bytes): " + data_transferred + "\n";
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