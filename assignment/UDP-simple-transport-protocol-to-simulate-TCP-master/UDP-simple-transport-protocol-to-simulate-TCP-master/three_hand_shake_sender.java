public static boolean threeHandShake(DatagramSocket socket) throws Exception {
		int client_isn = 0;
		int server_isn = 0;
		long client_isn_second;
		long server_isn_second;

		// Create random number generator for use in simulating
		// packet loss and network delay.
		Random random = new Random();

		// Create a datagram socket for receiving and sending UDP packets
		// through the port specified on the command line.

		// Processing loop.
		// Create a datagram packet to hold incomming UDP packet.
		DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

		// Block until the host receives a UDP packet.
		socket.receive(request);
		String fields = fetchData(request);
		// Print the recieved data.

		if (fields.substring(0, 3).equals("100")) {
			client_isn = Integer.parseInt(fields.substring(3, 13));
			server_isn = random.nextInt(2147483647);
			client_isn++;
			byte[] buf = ("100" + String.format("%010d", server_isn) + String.format("%010d", client_isn)).getBytes();
			clientHost = request.getAddress();
			clientPort = request.getPort();
			DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
			socket.send(reply);
		} else {
			System.out.println("Three handshake failed.");
		}
		socket.receive(request);
		fields = fetchData(request);
		if (fields.substring(0, 3).equals("000")) {
			client_isn_second = Integer.parseInt(fields.substring(3, 13));
			server_isn_second = Integer.parseInt(fields.substring(13, 23));
			if (client_isn_second == client_isn && server_isn_second == server_isn + 1) {
				System.out.println("Three hand-shake finished, TCP established, Redeay to transfer data");
				return true;
			} else {
				System.out.println("Three handshake failed.");
				return false;
			}

		} else {
			System.out.println("Three handshake failed.");
			return false;
		}
	}
