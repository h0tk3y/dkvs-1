package ru.ifmo.ctddev.year2012.nets.lab4;


import ru.ifmo.ctddev.year2012.nets.lab4.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;


/**
 * Represents an instance of Server (a machine in the system).
 * Should have unique identifier.
 * <p>
 * Created by viacheslav on 19.05.2015.
 */
public class Node implements Runnable, AutoCloseable {
    private int id;

    //public Logger log = Logger.getLogger("node." + id);

    private ServerSocket inSocket = null;
    public static Config globalConfig = null;

    private volatile boolean started = false;
    private volatile boolean stopping = false;

    private static final String CHARSET = "UTF-8";

    /**
     * Messages from this queue are polled and handled by handleMessages.
     * Every communication thread puts its received messages into the queue.
     */
    private LinkedBlockingDeque<Message> incomingMessages = new LinkedBlockingDeque<>();

    /**
     * An object for communication between remote Instances through sockets and message queue.
     */
    private class CommunicationEntry {
        Socket input = null;
        Socket output = null;
        LinkedBlockingDeque<Message> messages = new LinkedBlockingDeque<>();

        public void resetOutput() {
            try {
                if (output != null)
                    output.close();
            } catch (IOException e) {
            }
            output = new Socket();
            ready = false;
            messages.retainAll(messages.stream().filter(m ->
                    !(m instanceof PingMessage)).collect(Collectors.toList()));
        }

        public void setReady() {
            ready = true;
        }

        public volatile boolean ready = false; // synchronization. retaining messages, connecting
        // and creating output writer should be synchronized.

        public volatile boolean inputAlive = false;
        public volatile boolean outputAlive = false;
    }

    /**
     * existing Nodes.
     */
    private HashMap<Integer, CommunicationEntry> nodes;

    /**
     * map for clients
     */
    private SortedMap<Integer, CommunicationEntry> clients = new TreeMap<>();

    /**
     * Each node has a Replica, Leader and Acceptor instances.
     */
    private Replica localReplica;
    private Acceptor localAcceptor;
    private Leader localLeader;

    public Persistence persistence;
    public DKVSLogger logger;

    private Timer timer;


//------------------METHODS----------------------------------------------------

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: Node i");
            System.exit(1);
        }
        int thisNodenumber = Integer.parseInt(args[0]);
        new Node(thisNodenumber).run();
    }

    public Node(int id) {
        this.id = id;
        persistence = new Persistence(id);

        try {
            logger = new DKVSLogger(id);

            if (globalConfig == null) {
                globalConfig = Config.readDkvsProperties();
            }

            String inAddr = globalConfig.address(id);
            int inPort = globalConfig.port(id);
            inSocket = new ServerSocket(inPort, 10, InetAddress.getByName(inAddr));
            logger.logConnection("Node()", "Starting server socket on " + inAddr + ", port " + inPort);

            nodes = new HashMap<>(globalConfig.nodesCount());

            localReplica = new Replica(id, this);
            localLeader = new Leader(id, this);
            localAcceptor = new Acceptor(id, this);

            for (int i = 0; i < globalConfig.nodesCount(); ++i) {
                nodes.put(i, new CommunicationEntry());
            }
        } catch (IOException e) {
            logger.logError("Node()", e.getMessage());
        }

        timer = new Timer();
    }

    private static final int PACKET_SIZE = 1024;

    @Override
    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node twice");
        started = true;

        logger.logConnection("run()", "Starting node");

        localLeader.startLeader();

        // create output sockets and try to process output messages.
        for (int i = 0; i < globalConfig.nodesCount(); ++i) {
            if (i != id) {
                final int nodeId = i;
                new Thread(() -> {
                    speakToNode(nodeId);
                }).start();
            }
        }

        // start processing incoming messages from queue
        new Thread(this::handleMessages).start();

        // listen the server socket and try to accept external connections
        new Thread(() -> {
            while (!stopping)
                try {
                    Socket client = inSocket.accept();
                    new Thread(() -> {
                        handleRequest(client);
                    }).start();
                } catch (IOException ignored) {
                }
        }).start();

        new Thread(() -> {
            int anycastPort = globalConfig.getAnycastPort();
            String anycastAddress = globalConfig.getAnycastAddress();
            logger.logConnection("run()", "Starting anycast socket on " + anycastAddress + ", port " + anycastPort);
            try (DatagramSocket serverSocket = new DatagramSocket(anycastPort, InetAddress.getByName(anycastAddress))) {
                while (!stopping) {
                    DatagramPacket requestPacket = new DatagramPacket(new byte[PACKET_SIZE], 0, PACKET_SIZE);
                    serverSocket.receive(requestPacket);
                    String responseString = globalConfig.address(id) + ":" + globalConfig.port(id);
                    byte[] responseBytes = responseString.getBytes("UTF-8");
                    DatagramPacket responsePacket = new DatagramPacket(responseBytes, 0, responseBytes.length);
                    responsePacket.setAddress(requestPacket.getAddress());
                    responsePacket.setPort(requestPacket.getPort());
                    serverSocket.send(responsePacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Anycast routine failed, shutting down.");
                System.exit(1);
            }
        }).start();


        TimerTask pingTask = new TimerTask() {
            @Override
            public void run() {
                pingIfIdle();
            }
        };

        TimerTask monitorFaultsTask = new TimerTask() {
            @Override
            public void run() {
                monitorFaults();
            }
        };

        timer.scheduleAtFixedRate(pingTask, globalConfig.getTimeout(), globalConfig.getTimeout());
        timer.scheduleAtFixedRate(monitorFaultsTask, 4 * globalConfig.getTimeout(), 4 * globalConfig.getTimeout());
    }

    /**
     * handles the incoming request from specified socket.
     *
     * @param client
     */
    private void handleRequest(Socket client) {
        try {
            InputStreamReader reader = new InputStreamReader(client.getInputStream(), CHARSET);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String msg = bufferedReader.readLine();
            String[] parts = msg.split(" ");

            logger.logMessageIn("handleRequest():", "GOT message [" + msg + "] with request");

            switch (parts[0]) {
                case "node":
                    // (re)connection
                    int nodeId = Integer.parseInt(parts[1]);
                    try {
                        if (nodes.get(nodeId).input != null)
                            nodes.get(nodeId).input.close();
                    } catch (IOException e) {
                    }
                    nodes.get(nodeId).input = client;

                    logger.logConnection("handleRequest(node:" + nodeId + ")",
                            String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));
                    listenToNode(bufferedReader, nodeId);
                    break;
                case "get":
                case "set":
                case "delete":
                    final int newClientId = (clients.keySet().size() == 0) ? 1 :
                            (clients.keySet().stream().max(Comparator.<Integer>naturalOrder()).get()) + 1;

                    CommunicationEntry entry = new CommunicationEntry();
                    entry.input = client;
                    clients.put(newClientId, entry);

                    // We've already read a message from stream. Have to handle it:
                    Message firstMessage = ClientRequest.parse(newClientId, parts);
                    sendToNode(id, firstMessage);


                    /** Spawn communication thread. */
                    new Thread(() -> {
                        speakToClient(newClientId);
                    }).start();


                    logger.logConnection("handleRequest(client:" + newClientId + ")",
                            String.format("Client %d connected to %d.", newClientId, id));
                    listenToClient(bufferedReader, newClientId);
                    break;
                default:
                    logger.logMessageIn("handleRequest( ... )",
                            "something goes wrong: \"" + parts[0] + "\" received");
                    break;
            }

        } catch (IOException e) {
            logger.logError("handleRequest()", e.getMessage());
        }
    }

    /**
     * Takes messages in infinite loop from incoming queue and process them.
     */
    private void handleMessages() {
        while (!stopping) {
            Message m = null;
            try {
                m = incomingMessages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (m == null)
                continue;

            logger.logMessageIn("handleMessages()",
                    String.format("Handling message: %s", m));

            if (m instanceof ReplicaMessage) {
                localReplica.receiveMessage((ReplicaMessage) m);
                continue;
            }
            if (m instanceof LeaderMessage) {
                localLeader.receiveMessage((LeaderMessage) m);
                continue;
            }
            if (m instanceof AcceptorMessage) {
                localAcceptor.receiveMessage((AcceptorMessage) m);
                continue;
            }

            logger.logMessageIn("handleMessages()",
                    String.format("Unknown message: %s", m));
        }
    }

    @Override
    public void close() throws Exception {
        stopping = true;
        inSocket.close();
        for (CommunicationEntry n : nodes.values()) {
            if (n.input != null) n.input.close();
            if (n.output != null) n.output.close();
        }

        for (CommunicationEntry n : clients.values()) {
            if (n.input != null) n.input.close();
            if (n.output != null) n.output.close();
        }
    }

//---------------------SPEAK-&-LISTEN------------------------------------------

    /**
     * A Communication method, it puts all the messages received from
     * another nodes into [incomingMessages]. Should be executed in a separate thread.
     *
     * @param breader is BufferedReader from this node's socket stream.
     * @param nodeId  id of node, which is on the other end of this socket.
     */
    private void listenToNode(BufferedReader breader, int nodeId) {
//        logger.logConnection("listenToNode(nodeId:" + nodeId + ")",
//                "started listening to Node " + nodeId);

        nodes.get(nodeId).inputAlive = true;
        while (!stopping) {
            try {
                String data = breader.readLine();
                nodes.get(nodeId).inputAlive = true;
                Message m = Message.parse(nodeId, data.split(" "));

                if (m instanceof PingMessage) {
                    sendToNode(m.getSource(), new PongMessage(id));
                    continue;
                }

                if (m instanceof PongMessage) {
                    continue;
                }

                logger.logMessageIn("listenToNode(nodeId:" + nodeId + ")",
                        "GOT message [" + m + "] from " + nodeId);
                sendToNode(id, m);
            } catch (IOException e) {
                logger.logError("listenToNode(nodeId:" + nodeId + ")",
                        nodeId + ": " + e.getMessage());
                break;
            }
        }
    }

    /**
     * A Communication method, it puts all the messages received client
     * into [incomingMessages]. Should be executed in a separate thread.
     *
     * @param reader
     * @param clientId
     */
    private void listenToClient(BufferedReader reader, Integer clientId) {
        logger.logConnection("listenToClient()", String.format("#%d: Client %d connected. Started listening.", id, clientId));
        while (!stopping) {
            try {
                String fromClient = reader.readLine();
                if (fromClient == null)
                    throw new IOException("Client Disconnected.");
                String[] parts = fromClient.split(" ");
                ClientRequest message = ClientRequest.parse(clientId, parts);
                if (message != null) {
                    logger.logMessageIn("listenToClient()",
                            String.format("received message %s from client %d", message, message.getSource()));
                    sendToNode(id, message);
                }
            } catch (IOException e) {
                logger.logError("listenToClient()",
                        String.format("Lost connection to Client %d: %s", clientId, e.getMessage()));
                break;
            } catch (IllegalArgumentException e) {
                sendToClient(clientId, new ClientResponse(id, e.getMessage()));
            }
        }
    }

    /**
     * Creates an output socket to the specified node.
     * sends messages from corresponding queue through network to destination using socket.
     *
     * @param nodeId
     */
    private void speakToNode(int nodeId) {
        String address = globalConfig.address(nodeId);
        int port = globalConfig.port(nodeId);

        if (address == null) {
            logger.logError("speakToNode(nodeId" + nodeId + ")",
                    String.format("#%d: Couldn't get address for %d, closing.", id, nodeId));
            return;
        }

        while (!stopping) {
            try {
                nodes.get(nodeId).resetOutput();
                Socket clientSocket = nodes.get(nodeId).output;

                clientSocket.connect(new InetSocketAddress(address, port));
                logger.logConnection("speakToNode(nodeId: " + nodeId + ")",
                        String.format("#%d: CONNECTED to node.%d", id, nodeId));

                sendToNodeAtFirst(nodeId, new NodeMessage(id));

                logger.logMessageOut("speakToNode(nodeId: " + nodeId + ")",
                        String.format("adding node %d to queue for %d", id, nodeId));

                OutputStreamWriter writer = new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET);

                nodes.get(nodeId).setReady();

                while (!stopping) {
                    nodes.get(nodeId).outputAlive = true;
                    Message m = null;
                    try {
                        m = nodes.get(nodeId).messages.takeFirst();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (m == null) {
                        continue;
                    }
                    try {
                        writer.write(m + "\n");
                        writer.flush();
                        if(!(m instanceof PingMessage) && !(m instanceof PongMessage))
                        logger.logMessageOut("speakToNode(nodeId: " + nodeId + ")",
                                String.format("SENT to %d: %s", nodeId, m));
                    } catch (IOException ioe) {
                        logger.logError("speakToNode(nodeId: " + nodeId + ")",
                                String.format(
                                        "Couldn't send a message from %d to %d. Retrying.",
                                        id, nodeId));
                        nodes.get(nodeId).messages.addFirst(m);
                        break;
                    }
                }
            } catch (SocketException e) {
                logger.logError("speakToNode(nodeId: " + nodeId + ")",
                        String.format("DISCONNECTION: Connection from %d to node.%d lost: %s",
                                id, nodeId, e.getMessage()));
                try {
                    Thread.sleep(globalConfig.getTimeout());
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                logger.logError("speakToNode(nodeId: " + nodeId + ")",
                        String.format("Connection from %d to node.%d lost: %s",
                                id, nodeId, e.getMessage()));
            }
        }
    }

    /**
     * sends messages from corresponding queue through network to client.
     *
     * @param clientId
     */
    private void speakToClient(int clientId) {
        try {
            CommunicationEntry entry = clients.get(clientId);
            BlockingDeque<Message> queue = entry.messages;

            //writer BACK to the client.
            OutputStreamWriter writer = new OutputStreamWriter(entry.input.getOutputStream(), CHARSET);
            while (!stopping) {
                Message m = null;
                try {
                    m = queue.take();
                } catch (InterruptedException e) {
                }
                if (m == null)
                    continue;
                try {
                    logger.logMessageOut("speakToClient(clientId: " + clientId + ")",
                            String.format("#%d: Sending to client %d: %s", id, clientId, m));
                    writer.write(String.format("%s\n", m));
                    writer.flush();
                } catch (IOException ioe) {
                    logger.logMessageOut("speakToClient(clientId: " + clientId + ")",
                            "Couldn't send a message. Retrying.");
                    clients.get(clientId).messages.addFirst(m);
                }
            }
        } catch (IOException e) {
        }
        //TODO: suppose, after sending message back, resources can be freed.
    }

    /**
     * Adds given message to the appropriate Nodes's queue.
     *
     * @param to
     * @param message
     */
    public void sendToNode(int to, Message message) {
        while (!stopping) {
            try {
                if (to == id)
                    incomingMessages.put(message);
                else
                    nodes.get(to).messages.put(message);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public void sendToNodeAtFirst(int to, Message message) {
        while (!stopping) {
            try {
                if (to == id)
                    incomingMessages.putFirst(message);
                else
                    nodes.get(to).messages.putFirst(message);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * Adds given message to the appropriate Client's queue.
     *
     * @param to
     * @param message
     */
    public void sendToClient(int to, Message message) {
        while (!stopping) {
            try {
                clients.get(to).messages.putLast(message);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //------------------FAULTS-----------------------------------------------------

    /**
     * Pings nodes, which aren't spoken to last time.
     */
    private void pingIfIdle() {

        nodes.entrySet().stream()
                .filter(it -> (it.getKey() != id))
                .filter(it -> it.getValue().ready)
                .forEach(it -> {
                    if (!it.getValue().outputAlive)
                        sendToNode(it.getKey(), new PingMessage(id));
                    it.getValue().outputAlive = false;
                });
    }

    /**
     * Looks for nodes, which didn't respond to us last time.
     */
    private void monitorFaults() {
        HashSet<Integer> faultyNodes = new HashSet<>();

        nodes.entrySet().stream()
                .filter(it -> it.getKey() != id)
                .forEach(it -> {
                    if (!it.getValue().inputAlive) {
                        if (it.getValue().input != null)
                            try {
                                it.getValue().input.close();
                            } catch (IOException e) {
                            }
                        faultyNodes.add(it.getKey());
                        logger.logConnection("monitorFaults()", "Node " + it.getKey() + " is faulty, closing its connection.");
                    }
                    it.getValue().inputAlive = false;
                });

        if (faultyNodes.size() > 0)
            localLeader.notifyFault(faultyNodes);
    }


}

