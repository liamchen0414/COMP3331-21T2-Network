public static boolean threeHandShake(DatagramSocket socket, InetAddress ip, int portNumber) throws Exception {
		try {
			String s = "";
			Random random = new Random();
			int client_isn = random.nextInt(2147483647);
			byte[] buf = ("100" + String.format("%010d", client_isn) + String.format("%010d", 0)).getBytes();
			DatagramPacket request = new DatagramPacket(buf, buf.length, ip, portNumber);
			fetchData(request);
			socket.send(request);
			// Receive datagram
			DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
			socket.receive(reply);
			String fields = fetchData(reply);

			if (fields.substring(0, 3).equals("100")) {
				int server_isn_receive = Integer.parseInt(fields.substring(3, 13));
				server_isn_receive++;
				int client_isn_receive = Integer.parseInt(fields.substring(13, 23));
				if (client_isn_receive - client_isn == 1) {
					byte[] buf__second = ("000" + String.format("%010d", client_isn_receive)
							+ String.format("%010d", server_isn_receive)).getBytes();
					DatagramPacket second_request = new DatagramPacket(buf__second, buf__second.length, ip, portNumber);
					socket.send(second_request);
					fetchData(second_request);
					System.out.println("Three hand shake has been finished.");
					return true;
				} else {
					System.out.println("Three handshake failed.aaa");
					return false;
				}
			} else {
				System.out.println("Three handshake failed.bbb");
				return false;
			}
		} catch (Exception e) {
			// If there is timeout, the client will print "Reply not
			// received."
			System.out.println(e.getMessage());
			System.out.println("Packet " + ":   Reply not recieved. Three hand shake failed.");
		}
		return false;

	}