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
    static final int PORT_MIN = 1024;
    static final int PORT_MAX = 65535;
    static final int PDROP_MIN = 0;
    static final int PDROP_MAX = 1;
    static int ACK_INIT = 0;
    static int seq_receiver;
    static int ack_receiver;
    static Random random;
    // Variables to store header
    static byte[] sData;
    static byte[] rData;
    // Variables to write log file
    static List<String[]> log_records = new ArrayList<String[]>();
    static int data_transferred;
    static int nSegments;
    static int nSegments_no_retrans;
    static int nDropped;
    static int nSegments_retrans;
    static int duplicate_ack;
    static long start_time;
    static long requestTime;
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
        three_way_connection(receiver_host_ip, receiver_port);
        send();
        File f = create_sender_log();
        write_sender_log(f);
        connection_close(sender_seq, sender_ack);
        System.out.println("Connection closed");
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

    public static String makeHeader(int seq, int ack, String flags, String payload){
        String flag = makeFlag(flags);
        String result = seq + "|" + ack + "|" + flag + "|" + payload + "|";
        return result;
    }

    // 3-way connection setup(SYN,SYN+ACK,ACK), bypassing PL module
    public static void three_way_connection(InetAddress receiver_host_ip, int receiver_port) throws SocketException, IOException{
        // establish socket
        String header, flags;
        int sender_seq_isn, sender_ack_isn;
        sender_seq_isn = 0;
        sender_ack_isn = 0;
        // stage 1
        senderSocket = new DatagramSocket();
        start_time = System.currentTimeMillis();
         // PTP segment = "seq,ack,FSAD,MWS,payload"
        flags = "0100";
        header = makeHeader(sender_seq_isn, sender_ack_isn, flags, "");
        sData = header.getBytes();
        DatagramPacket sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", requestTime, makeFlag(flags), sender_seq_isn, 0, sender_ack_isn);
        
       // stage 2
	    rData = new byte[1024];
	    DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
	    senderSocket.receive(rPacket);
	    requestTime = System.currentTimeMillis() - start_time;
	    String[] response = new String(rPacket.getData()).trim().split("\\|");
	    int seq2 = Integer.parseInt(response[0]);
	    int ack2 = Integer.parseInt(response[1]);
        System.out.println("Received Handshake Response, Receiver_ACK= "+ ack2);
        flags = response[2];
        write_log_record("rcv", requestTime, flags, seq2, 0, ack2);

       // stage 3
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        flags = "0010";
        header = makeHeader(seq3, ack3, flags, "");
        sData = header.getBytes();
        sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        System.out.println("Sending Handshake Acknowledgement, Sender_Seq= "+ seq3);
        senderSocket.send(sPacket);
        requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", requestTime, makeFlag(flags), seq3, 0, ack3);
        System.out.println("Server is ready, connection established :");
        // seq and ack to be carried over to sending phase
        sender_seq = seq3;
        sender_ack = ack3;
    }
    
    // aux function to process file
	public static void readFile() throws IOException,FileNotFoundException{
		linesToSend = new ArrayList<String>();
        data_transferred = (int)(new File(fileName)).length();
        FileReader f = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(f);
		String getLine = reader.readLine();
		String file = "";
		while(getLine != null){
			file += getLine + "\n"; // add new line to each line, TOCHECK carriage
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

    }


    public static void send() throws Exception {
        nSegments = 0;
        nDropped = 0;
        nSegments_retrans = 0;
        readFile();
        int LastByteSent = sender_seq;
        int LastByteAcked = sender_seq;
        int triple_counter = 0;
        String header, flags;
        for(int i = 0; i < linesToSend.size(); i++) {
            while(LastByteSent - LastByteAcked <= MWS && LastByteSent < data_transferred) {
                flags = "0001";
                header = makeHeader(sender_seq, sender_ack, flags, linesToSend.get(i));
                sData = header.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
                LastByteSent = (LastByteSent + linesToSend.get(i).length() > data_transferred) ? LastByteSent + linesToSend.get(i+1).length(): LastByteSent + MSS;
                // sending
                if(random.nextFloat() > pdrop) {
                    System.out.println("This is to check: " + "LastByteAcked" + LastByteAcked + "sender_seq" + sender_seq);
                    senderSocket.send(sendPacket);
                    write_log_record("snd", requestTime, makeFlag(flags), sender_seq, linesToSend.get(i).length(), sender_ack);             
                } else {
                    sender_seq = LastByteSent;
                    write_log_record("drop", requestTime, makeFlag(flags), sender_seq, linesToSend.get(i).length(), sender_ack);  
                    nDropped++;
                    i++;
                    continue;
                }

                try {
                    rData = new byte[1024];
                    DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
                    senderSocket.receive(rPacket);
                    String[] response = new String(rPacket.getData()).stripTrailing().split("\\|");
                    seq_receiver = Integer.parseInt(response[0]);
                    ack_receiver = Integer.parseInt(response[1]);
                    System.out.println("Final: " + " LastByteAcked " + LastByteAcked + " sender_seq " + sender_seq);
                    write_log_record("rcv", requestTime, "A", seq_receiver, 0, ack_receiver);
                    if (ack_receiver < LastByteSent) {
                        // two scenario, either it is an out of order packet
                        // or it is a retransmission ack
                        if (LastByteAcked + linesToSend.get(i).length() == ack_receiver) { // retrans
                            LastByteAcked = ack_receiver;
                            sender_seq = ack_receiver;
                        } else { // out of order
                            sender_seq = LastByteSent;
                            triple_counter++;
                            if (triple_counter == 3) {
                                sender_seq = LastByteAcked;
                                triple_counter = 0;
                            }
                        }
                    } else {
                        // expected ack received
                        LastByteAcked = ack_receiver;
                        sender_seq = ack_receiver;
                        break;
                    }
                    sender_ack = seq_receiver;
                } catch (Exception e) { // time out even
                    sender_seq = LastByteSent;
                    sender_ack = seq_receiver;
                }
            } // end of while
        }
    }


    // 4-way connection termination(FIN,ACK+FIN,ACK)
    public static void connection_close(int seq1, int ack1) throws SocketException, IOException{
        String flags = "1000";
        String header = makeHeader(seq1, ack1, flags, ""); // no payload
        byte[] sRequest = header.getBytes();
        DatagramPacket sPacket = new DatagramPacket(sRequest, sRequest.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        long requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", requestTime, makeFlag(flags), seq1, 0, ack1);

        // receiver gets end request and sends ack + fin
	    byte[] rRequest = new byte[1024];
	    DatagramPacket rPacket = new DatagramPacket(rRequest, rRequest.length);
	    senderSocket.receive(rPacket);
	    requestTime = System.currentTimeMillis() - start_time;
        String[] response = new String(rPacket.getData()).trim().split("\\|");
	    int seq2 = Integer.parseInt(response[0]);
	    int ack2 = Integer.parseInt(response[1]);
        flags = response[2];
        write_log_record("rcv", requestTime, flags, seq2, 0, ack2);

        // connection fully closed
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        flags = "0010";
        header = makeHeader(seq3, ack3, flags, "");
        byte[] sData = header.getBytes();
        sPacket = new DatagramPacket(sData, sData.length, receiver_host_ip, receiver_port);
        senderSocket.send(sPacket);
        requestTime = System.currentTimeMillis() - start_time;
        write_log_record("snd", requestTime, makeFlag(flags), seq3, 0, ack3);
        System.out.println("Connection is now Closed!");
    }

    // store each row in the log record array
    public static void write_log_record(String status, long time, String type_of_packet, int seq, int length, int ack){
        String[] record = new String[6];
        record[0] = status;
        record[1] = Long.toString(time);
        record[2] = type_of_packet;
        record[3] = Integer.toString(seq);
        record[4] = Integer.toString(length);
        record[5] = Integer.toString(ack);
        log_records.add(record);
        // debug
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