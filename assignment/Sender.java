import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class Sender {
    // variables
	private static DatagramSocket senderSocket;


    // argument types
	private static InetAddress receiverIPAddress;
	private static String receiver_host_ip;
	private static int receiver_port; 
	private static String fileName;
	private static int MWS;
	private static int MSS;
	private static int timeout;
	private static Double pdrop;
	private static int seed;

    public static void main(String[] args) {
        if (args.length != 8) {
            System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + " <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>");
            return;
        }

        // write arguments into variables
        receiver_host_ip = InetAddress.getByName(args[0]);
        receiver_port = Integer.parseInt(args[1]);
        fileName = args[2];
        MWS = Integer.parseInt(args[3]); // maximum window size
		MSS = Integer.parseInt(args[4]); // maximum segment size
        timeout = Integer.parseInt(args[5]); //  in milliseconds
        pdrop = Double.parseDouble(args[6]);
        seed = Integer.parseInt(args[7]);

    }

    
}
