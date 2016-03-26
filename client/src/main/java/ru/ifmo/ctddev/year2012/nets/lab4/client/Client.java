package ru.ifmo.ctddev.year2012.nets.lab4.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.nio.charset.Charset;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class Client {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: client anycastAddress anycastPort");
        }

        String anycastAddress = args[0];
        int anycastPort = Integer.parseInt(args[1]);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String nodeAddress;
            int nodePort;
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(1000);
                while (true) {
                    try {
                        DatagramPacket requestPacket = new DatagramPacket(new byte[1], 0, 1);
                        requestPacket.setAddress(InetAddress.getByName(anycastAddress));
                        requestPacket.setPort(anycastPort);
                        socket.send(requestPacket);
                        System.out.println("Sent anycast request to " + anycastAddress + ", port " + anycastPort + ".");
                        DatagramPacket responsePacket = new DatagramPacket(new byte[1024], 0, 1024);
                        socket.receive(responsePacket);
                        String response = new String(responsePacket.getData(), 0, responsePacket.getLength(), Charset.forName("UTF-8"));
                        System.out.println("Received anycast response: " + response);
                        int lastColon = response.lastIndexOf(':');
                        if (lastColon == -1) {
                            continue;
                        }
                        nodeAddress = response.substring(0, lastColon);
                        nodePort = Integer.parseInt(response.substring(lastColon + 1));
                        break;
                    } catch (NumberFormatException | IOException ignored) {
                    }
                }
            }
            Socket socket = new Socket();
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(nodeAddress), nodePort);
            socket.connect(address);
            System.out.println("connected: " + anycastPort);

            OutputStreamWriter socketWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            InputStreamReader socketReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(socketReader);

            while (true) {
                String command = reader.readLine();
                System.out.println("request: " + command);
                if (command == null) {
                    socketWriter.close();
                    return;
                }

                socketWriter.write(command + "\n");
                socketWriter.flush();

                String response = bufferedReader.readLine();
                System.out.println("response: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
