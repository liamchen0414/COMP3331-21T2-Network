import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class test{
	public static DatagramSocket senderSocket;
	public static InetAddress receiverIPAddress;
	public static String receiver_host_ip;
	public static int receiver_port; 
	public static String fileName;
	public static int MWS; // maximum window size
	public static int MSS; // Maximum Segment Size
	public static int timeout;
	public static Double pdrop;
	public static int seed;

	public static BufferedReader reader;
	public static ArrayList<String> linesToSend;
	public static ArrayList<String> sender_status;
	public static int Sender_Seq; // sender's sequence number
	public static int Reicever_Seq; //receiver's sequnce number;
	public static int Receiver_SeqACK; // sequence ack number replied from receiver
	public static int Sender_SeqACK; // ack sending to receiver
	public static Random random;
	public static long start_time;
	public static long end_time;

	public static int num_timeout;
	public static int num_drop;
	public static int num_retransmitted;
	public static int num_DupACK;
	public static int numSeg; 
	public static boolean retransmit_flag;

	public static void main(String[] args) throws IOException,FileNotFoundException, InterruptedException{
		linesToSend = new ArrayList<String>();
		reader = new BufferedReader(new FileReader("test.txt"));
		String getLine = reader.readLine();
		String file = "";
		while(getLine != null){
			file += getLine + "\r\n"; // add new line to each line
			getLine = reader.readLine();
		}
		reader.close();
		MSS = 32;
		// divide files into MSS per string and store in the array list
		while(file.length() > MSS) {
			getLine = file.substring(0, MSS);
			linesToSend.add(getLine);
			file = file.substring(MSS, file.length());
		}
		linesToSend.add(file);
	}
}