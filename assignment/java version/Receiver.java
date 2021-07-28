
import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver {

	static List<String[]> content=new ArrayList<String[]>();
	static int want_seq=0;
	static int last_send_seq=0;
	static int last_send_ack=0;
	static List<String[]> log = new ArrayList<String[]>();
	static long start_time;
	static String fileName;
	static int amount_byte=0;
	static int num_duplicate=0;
	static List<Integer> duplicate=new ArrayList<Integer>();
	static InetAddress IPAddress;
	static int send_port;
	static DatagramSocket receiverSocket;
	
	public static void main(String[] args) throws Exception
	{
		start_time = System.currentTimeMillis();
		if (args.length != 2) {
		    System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + "<receiver_port> <FileToSave.txt>");
            return;
        }
	    int port = Integer.parseInt(args[0]);
	    fileName = args[1];
	    String header, flags, flag;
	    receiverSocket = new DatagramSocket(port);
		byte[] sData;
		while(true) {
			// System.out.println("Waiting for client to connect.....");
			byte[] rRequest = new byte[1024];
			DatagramPacket rPacket = new DatagramPacket(rRequest, rRequest.length);
			receiverSocket.receive(rPacket);
			InetAddress senderIPAddress = rPacket.getAddress();
			int senderPort = rPacket.getPort();
			// requestTime = System.currentTimeMillis() - start_time;
			String[] response = new String(rPacket.getData()).split(" ");
			// byte[] buf = rPacket.getData();
			// ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			// InputStreamReader isr = new InputStreamReader(bais);
			// BufferedReader br = new BufferedReader(isr);
			// String line = br.readLine();
			// System.out.println(line);
			int seq_sender = Integer.parseInt(response[0]);
			int ack_sender = Integer.parseInt(response[1]);
			String payload = response[2];
			System.out.println(response[1]);
			long requestTime = System.currentTimeMillis() - start_time;
			// System.out.println("rcv" + " " + Double.parseDouble(Long.toString(requestTime))/1000 + " " + "SA" + " " + seq_sender + " " + "0" + " " + ack_sender);
			// write_log_record("rcv", Double.parseDouble(Long.toString(requestTime))/1000, flags, seq2, 0, ack2);
			int ack_receiver = seq_sender + payload.length();
			int seq_receiver = ack_sender;
			String answer = "";
			header = makeHeader(seq_receiver, ack_receiver, "0010", answer);
			sData = header.getBytes();
			DatagramPacket sPacket = new DatagramPacket(sData, sData.length, senderIPAddress, senderPort);
			receiverSocket.send(sPacket);
		}
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
        String result = seq + " " + ack + " " + flag + " " + payload;
        return result;
    }


}