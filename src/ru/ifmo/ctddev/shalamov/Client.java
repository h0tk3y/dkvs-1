package ru.ifmo.ctddev.shalamov;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class Client {
    public static void main(String[] args) {
        String listOfPorts = "5454 5455 5456";
        System.out.println(listOfPorts+ ": picked first");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            int port = Integer.parseInt(listOfPorts.split(" ")[0]);
            Socket socket = new Socket();
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            socket.connect(address);
            System.out.println("connected: " + port);

            OutputStreamWriter socketWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
            InputStreamReader socketReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(socketReader);

            while (true) {
                String command = reader.readLine();
                System.out.println("request: " + command);

                socketWriter.write(command + "\n");
                socketWriter.flush();

                String response = bufferedReader.readLine();
                System.out.println("response: "+ response);
            }
        }catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
