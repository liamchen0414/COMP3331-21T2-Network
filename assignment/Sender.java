
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class Sender extends Thread {
    // Variables from supplied file
    static List<SocketAddress> clients=new ArrayList<SocketAddress>();
    static byte[] sendData = new byte[1024];
    static DatagramSocket senderSocket;
    static int UPDATE_INTERVAL = 1000;//milliseconds
    static ReentrantLock syncLock = new ReentrantLock();
    static int sender_Seq;
    static int sender_ACK;

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
    static int segments_send_total;
    static int segments_sent_no_retrans;
    static int packet_dropped;
    static int segment_retrans;
    static int duplicate_ack;


    public static void main(String[] args) throws Exception {
        // checking the number of cmd line argument
        if (args.length != 8) {
            System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + " <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>");
            return;
        }

        // write common line arguments into variables with some simple checks
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

        // generating random number between 0 and 1 using seed
        Random random = new Random(seed);
        float rdrop = random.nextFloat();

        // if(rdrop < pdrop) don't send the packet

        // define PL module: simulation of packet loss
    }

    // 3-way connection setup(SYN,SYN+ACK,ACK), bypassing PL module
    public static void three_way_connection(InetAddress receiver_host_ip, int receiver_port){
        // sender sends connection request 1way
        senderSocket = new DatagramSocket(receiver_port);
        long start_time = System.currentTimeMillis();
        
        seq1 = SENDER_ISN;
        ack1 = ACK_INIT;
        int syn1 = 1;
        String header1 = "syn=" + syn1 + ", " + "seq=" + seq1;
        byte[] way1 = header1.getBytes();
        DatagramPacket connection_request = new DatagramPacket(way1, way1.length, receiver_host_ip, receiver_port);
        senderSocket.send(connection_request);
        long connectionTime1 = System.currentTimeMillis() - start_time;
        write_log_record("snd",Long.toString(connectionTime1),"S",Integer.toString(seq1),"0",Integer.toString(ack1));
        
        // sender receives connection granted, 2way
	    byte[] way2 = new byte[1024];
	    DatagramPacket connection_granted = new DatagramPacket(way2, way2.length);
	    sendSocket.receive(connection_granted);
	    long connectionTime2 = System.currentTimeMillis()-start_time;
	    String[] header2 = new String(connection_granted.getData()).trim().split(", ");
        // msg format (syn=1, seq=server_isn, ack=client_isn+1)
	    int syn2 = Integer.parseInt(header2[0].substring(4));
	    int seq2 = Integer.parseInt(header2[1].substring(4));
	    int ack2 = Integer.parseInt(header2[2].substring(4));
        // introducing additional check mechanism to see if connection is successful
        write_in_log("rcv",Long.toString(connectionTime2),"SA",Integer.toString(seq2),"0",Integer.toString(ack2));
        // sender sends final confirmation before sending data, 3way
        // syn3 = 0, seq3 = client_isn + 1 = 0, ack3 = server_isn + 1
        int syn3 = 0;
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        String header3 = "syn=" + syn3 + ", " + "seq=" + seq3;
        byte[] way3 = header3.getBytes();
        DatagramPacket connection_confirmed = new DatagramPacket(way3, way3.length, receiver_host_ip, receiver_port);
        senderSocket.send(connection_confirmed);
        long connectionTime3 = System.currentTimeMillis() - start_time;
        write_log_record("snd",Long.toString(connectionTime3),"A",Integer.toString(seq3),"0",Integer.toString(ack3));
        System.out.println("Server is ready, connection established :");
    }
        

    // Use MWS as window size, use MSS as the size of data payload, include sequence and ack number like TCP


    // fast retransmit(triple duplicates)  ACK received, with ACK field value of y
    public static void retransmit(int sendBase, int y){
        if (y > sendBase) {
            sendBase = y;
            if (not_yet_acknowledged )
        }
    }

    public static void segmentation(String fileName, int MSS) throws Exception {
        File f = new File(fileName);
        if(f.exists() && f.isFile()) {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            
        }
    }

    // single timer and transmit the oldest unack segment


    // receiver should buffer out of order segments



    // 4-way connection termination(FIN,ACK+FIN,ACK)
    public static void connection_close(int seq1, int ack1) {
        // sender sends FIN to close connection
        int fin1 = 1;
        String endHeader1 = "fin=" + fin1 + ", " + "seq=" + seq1 + ", " + "ack=" + ack1;
        byte[] end1 = endHeader1.getBytes();
        DatagramPacket end_request = new DatagramPacket(end1, end1.length, receiver_host_ip, receiver_port);
        senderSocket.send(end_request);
        long endConnectionTime1 = System.currentTimeMillis() - start_time;
        write_log_record("snd",Long.toString(endConnectionTime1),"F",Integer.toString(seq1),"0",Integer.toString(ack1));
        
        // receiver gets end request and sends ack + fin
	    byte[] end2 = new byte[1024];
	    DatagramPacket close_connection = new DatagramPacket(end2, end2.length);
	    sendSocket.receive(close_connection);
	    long endConnectionTime2 = System.currentTimeMillis()-start_time;
        Strin[] end_header2 = new String(close_connection.getData()).trim().split(", ");
	    int fin2 = Integer.parseInt(header2[0].substring(4));
	    int seq2 = Integer.parseInt(header2[1].substring(4));
	    int ack2 = Integer.parseInt(header2[2].substring(4));
        write_in_log("rcv",Long.toString(endConnectionTime2),"FA",Integer.toString(seq2),"0",Integer.toString(ack2));

        // connection fully closed
        int fin3 = 0;
        int seq3 = ack2;
        int ack3 = seq2 + 1;
        String endHeader3 = "fin=" + fin3 + ", " + "seq=" + seq3 + ", " + "ack=" + ack3;
        byte[] end3 = header3.getBytes();
        DatagramPacket connection_confirmed = new DatagramPacket(end3, end3.length, receiver_host_ip, receiver_port);
        senderSocket.send(connection_confirmed);
        long connectionTime3 = System.currentTimeMillis() - start_time;
        write_log_record("snd",Long.toString(connectionTime3),"A",Integer.toString(seq3),"0",Integer.toString(ack3));
        System.out.println("Connection is now Closed!");
    }





    // store each row in the log record array
    public static void write_log_record(String status, String time, String type_of_packet, String seq, String length, String ack){
        String[] record = new String[6];
        record[0] = status;
        record[1] = time;
        record[2] = type_of_packet;
        record[3] = seq;
        record[4] = length;
        record[5] = ack;
        log_records.add(record);
        // debug
        System.out.println(status + ", " + time + ", " + type_of_packet + ", " + seq + ", " + length + ", " + ack);
    }

    // create the sender log
    public static void create_sender_log() throws Exception {
        File f = new File("Sender_log.txt");
        try {
			if (f.exists())
                f.delete();
            f.createNewFile();
            write_sender_log();
		}catch(Exception e){  
			e.printStackTrace();  
		}
    }

    // write records to sender_log.txt
    public static void write_sender_log(File f){
        FileOutputStream oStream = new FileOutputStream(f);        
        String row = "";

        for(int i = 0; i < log_records.size(); i++) {
            if(log_records.get(i)[0].equals("snd"))
                segments_send_total += 1;
            row = log_records.get(i)[0] + "\t" + log_records.get(i)[1] + "\t" + log_records.get(i)[2] + 
                "\t" + log_records.get(i)[3] + "\t" + log_records.get(i)[4] + "\t" + log_records.get(i)[5];
            oStream.write(row.getBytes());
        }
        String summary1="Amount of Data Transferred (in bytes): " + data_transferred + "\n";
		String summary2="Number of Data Segments Sent (excluding retransmissions): " + segments_sent_no_retrans + "\n";
		String summary3="Number of Packets Dropped: "+ packet_dropped + "\n";
		String summary4="Number of Retransmitted Segments: " + segment_retrans + "\n";
		String summary5="Number of Duplicate Acknowledgements received: " + duplicate_ack + "\n";
        oStream.write(summary1.getBytes());
        oStream.write(summary2.getBytes());
        oStream.write(summary3.getBytes());
        oStream.write(summary4.getBytes());
        oStream.write(summary5.getBytes());
        oStream.close();
    }
}