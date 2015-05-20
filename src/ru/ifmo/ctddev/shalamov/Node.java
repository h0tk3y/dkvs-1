package ru.ifmo.ctddev.shalamov;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;


import ru.ifmo.ctddev.shalamov.messages.*;
import ru.ifmo.ctddev.shalamov.messages.Message;
import sun.plugin2.message.*;

/**
 * Created by viacheslav on 19.05.2015.
 */
public class Node implements Runnable, AutoCloseable {
    private int id;
    private String persistenceFileName;
    private FileWriter diskPersistence;

    private Logger log = Logger.getLogger("node." + id);

    private ServerSocket inSocket = null;
    private static Config globalConfig = null;

    private volatile boolean started = false;
    private volatile boolean stopping = false;

    private static final String CHARSET = "UTF-8";

    private LinkedBlockingDeque<Message> incomingMessages = new LinkedBlockingDeque<>();

    private Replica localReplica;

    private class NeighborEntry {
        Socket input = null;
        Socket output = null;

        LinkedBlockingDeque<Message> messages = new LinkedBlockingDeque<>();
    }

    private NeighborEntry[] neighbors;

    public Node(int id) {
        this.id = id;
        persistenceFileName = String.format("dkvs_%d.log", id).toString();
        try {
            if (globalConfig == null)
                globalConfig = Config.readDkvsProperties();
            inSocket = new ServerSocket(globalConfig.port(id));
            diskPersistence = new FileWriter(persistenceFileName, true);
            neighbors = new NeighborEntry[globalConfig.nodesCount()];
            localReplica = new Replica(id, globalConfig.ids());
            for (int i = 0; i < globalConfig.nodesCount(); ++i) {
                neighbors[i] = new NeighborEntry();
            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    private void saveToDisk(Object data) {
        synchronized (diskPersistence) {
            try {
                diskPersistence.append(data.toString());
                diskPersistence.append('\n');
                diskPersistence.flush();
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node which has already been started");

        started = true;

        for (int i = 0; i < globalConfig.nodesCount(); ++i) {
            if (i != id) {
                final int nodeId = i;
                new Thread(() -> {
                    speakToNode(nodeId);
                }).start();
            }
        }

        new Thread(() -> {
            handleMessages();
        }).start();


        new Thread(() -> {
            while (!stopping)
                try {
                    Socket client = inSocket.accept();
                    new Thread(() -> {
                        handleRequest(client);
                    }).start();
                } catch (SocketException e) {
                } catch (IOException e) {
                }
        }).start();

//        while (!stopping)
//            try {
//                Socket client = inSocket.accept();
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        handleRequest(client);
//                    }
//                }).run();
//            } catch (IOException e) {
//                log.info(e.getMessage());
//            }
    }

    private void handleRequest(Socket client) {
        InputStreamReader reader = null;

        try {
            reader = new InputStreamReader(client.getInputStream(), CHARSET);
            BufferedReader br = new BufferedReader(reader);
            String[] parts = br.readLine().split(" ");
            switch (parts[0]) {
                case "node":
                    listenToNode(client, Integer.parseInt(parts[1]));
                default:
                    break;
            }

        } catch (IOException e) {
            log.info(e.getMessage());
            return;
        }
    }

    private void handleMessages() {
        while (true) {
            Message m = incomingMessages.poll();
            if (m instanceof ReplicaMessage) {
                localReplica.receiveMessage((ReplicaMessage) m);
            }
        }
    }

    @Override
    public void close() throws Exception {
        stopping = true;
        inSocket.close();
        for (NeighborEntry n : neighbors) {
            if (n.input != null) n.input.close();
            if (n.output != null) n.output.close();
        }
    }

    private void listenToNode(Socket client, int nodeId) {
        try {
            neighbors[nodeId].input.close();
            neighbors[nodeId].input = client;
            BufferedReader breader = new BufferedReader(new InputStreamReader(client.getInputStream(), CHARSET));

            log.info(String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));

            String data = breader.readLine();
            Message m = Message.parse(data.split(""));
            incomingMessages.offer(m);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }


    private void speakToNode(int nodeId) {
        Socket clientSocket = new Socket();
        neighbors[nodeId].output = clientSocket;

        String address = globalConfig.address(nodeId);
        int port = globalConfig.port(nodeId);

        if (address == null) {
            log.info(String.format("#%d: Couldn't get address for %d, closing.", id, nodeId));
            return;
        }

        while (!stopping) {
            try {
                clientSocket.connect(new InetSocketAddress(address, port));
                log.info(String.format("#%d: Connected to node.%d", id, nodeId));
                send(nodeId, new NodeMessage(id));
                OutputStreamWriter writer = new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET);

                while (true) {
                    Message m = neighbors[nodeId].messages.pollFirst();

                    if (m == null) {
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        log.info(String.format("#%d: Sending to %d: %s", id, nodeId, m));
                        writer.write(m + "\n");
                    } catch (IOException ioe) {
                        log.info(String.format("#%d: Couldn't send a message. Retrying.", id));
                        neighbors[nodeId].messages.addFirst(m);
                    }
                }
            } catch (SocketException e) {
                log.info(String.format("#%d: Connection to node.%d lost: %s", id, nodeId, e.getMessage()));
            } catch (IOException e) {
            }
        }
    }

    private void send(int to, Message message) {
        neighbors[to].messages.addLast(message);
    }
}

