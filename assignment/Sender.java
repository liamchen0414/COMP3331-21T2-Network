
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.locks.*;

public class Sender {
    // variables
	private static DatagramSocket senderSocket;
	private static InetAddress receiver_host_ip;
	private static int receiver_port; 
	private static String fileName;
	private static int MWS;
	private static int MSS;
	private static int timeout;
	private static Double pdrop;
	private static int seed;
    private static final int PORT_MIN = 1024;
    private static final int PORT_MAX = 65535;
    private static final int PDROP_MIN = 0;
    private static final int PDROP_MAX = 1;

    public static void main(String[] args) throws Exception {
        if (args.length != 8) {
            System.out.println("Usage: java " + Thread.currentThread().getStackTrace()[1].getClassName() 
                + " <receiver_host_ip> <receiver_port> <FileToSend.txt> <MWS> <MSS> <timeout> <pdrop> <seed>");
            return;
        }

        // write arguments into variables
        receiver_host_ip = InetAddress.getByName(args[0]);
        receiver_port = Integer.parseInt(args[1]);
        if (receiver_port < PORT_MIN || receiver_port > PORT_MAX){
            System.out.println("Invalid Port Number, aborting...");
            return;
        }
        fileName = args[2]; // no error checking as per spec
        MWS = Integer.parseInt(args[3]); // maximum window size
		MSS = Integer.parseInt(args[4]); // maximum segment size
        timeout = Integer.parseInt(args[5]); //  in milliseconds
        pdrop = Double.parseDouble(args[6]);
        if (pdrop < PDROP_MIN || pdrop > PDROP_MAX){
            System.out.println("Pdrop value must be between 0 and 1, aborting...");
            return;
        }        
        seed = Integer.parseInt(args[7]);

        // PL module: simulation of packet loss
        // while (true) {
        //     // Create a datagram packet to hold incoming UDP packet.
        //     DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        //     // Block until the host receives a UDP packet.
        //     socket.receive(request);
        //     // Print the received data.
        //     printData(request);
        //     // Decide whether to reply, or simulate packet loss.
        //     if (random.nextDouble() < pdrop) {
        //     System.out.println("   Reply not sent.");
        //     continue; 
        //     }

        //     // Simulate network delay.
        //     Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

        //     // Send reply.
        //     InetAddress clientHost = request.getAddress();
        //     int clientPort = request.getPort();
        //     byte[] buf = request.getData();
        //     DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
        //     socket.send(reply);

        //     System.out.println("   Reply sent.");
        // }
        
        // generating random number between 0 and 1 using seed
        Random random = new Random(seed);
        float x = random.nextFloat();
        System.out.println(x);


        // 3-way connection setup(SYN,SYN+ACK,ACK)


        // 4-way connection termination(FIN,ACK,FIN,ACK)


        // single timer and transmit the oldest unack segment

        // receiver should buffer out of order segments

        // fast retransmit(triple duplicates)


        // include sequence and ack number like TCP

        // Use MWS as window size

        // use MSS as the size of data payload



        // design PTP segment format
        // UDP header + PTP header + PTP Payload
        // UDP already has source destination port number
    }    
}
