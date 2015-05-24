package ru.ifmo.ctddev.shalamov;


import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import ru.ifmo.ctddev.shalamov.messages.*;
import ru.ifmo.ctddev.shalamov.messages.Message;


/**
 * Represents an instance of Server (a machine in the system).
 * Should have unique identifier.
 * <p>
 * Created by viacheslav on 19.05.2015.
 */
public class Node implements Runnable, AutoCloseable {
    private int id;

    //todo: implement a better logger
    public Logger log = Logger.getLogger("node." + id);

    private ServerSocket inSocket = null;
    private static Config globalConfig = null;

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
    }

    /**
     * fixed-size array of existing Nodes.
     */
    private CommunicationEntry[] nodes;

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

    private Persistence localPersistence;


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
        localPersistence = new Persistence(id);

        // todo: try to restore from disc first of all;
        try {
            if (globalConfig == null)
                globalConfig = Config.readDkvsProperties();
            inSocket = new ServerSocket(globalConfig.port(id));
            nodes = new CommunicationEntry[globalConfig.nodesCount()];
            localReplica = new Replica(id, this, Arrays.asList(0)); // todo: only one leader.!!!!
            localLeader = new Leader(id,this,globalConfig.ids(), globalConfig.ids());
            localAcceptor = new Acceptor(id, this);
            for (int i = 0; i < globalConfig.nodesCount(); ++i) {
                nodes[i] = new CommunicationEntry();
            }
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    @Override
    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node which has already been started");
        started = true;

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
        new Thread(() -> {
            handleMessages();
        }).start();

        // listen the server socket and try to accept external connections
        new Thread(() -> {
            while (!stopping)
                try {
                    Socket client = inSocket.accept();
                    new Thread(() -> {
                        handleRequest(client);
                    }).start();
                } catch (IOException e) {
                }
        }).start();

        // todo: what is no requests exist??
        localLeader.startLeader();
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
            String[] parts = bufferedReader.readLine().split(" ");
            switch (parts[0]) {
                case "node":
                    // (re)connection
                    int nodeId = Integer.parseInt(parts[1]);
                    try {
                        nodes[nodeId].input.close();
                    } catch (IOException e) {
                    }
                    nodes[nodeId].input = client;
                    log.info(String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));
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
                    Message firstMessage = Message.parse(newClientId, parts);
                    while (!stopping) {
                        try {
                            incomingMessages.put(firstMessage);
                            break;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    /** Spawn communication thread. */
                    new Thread(() -> {
                        speakToClient(newClientId);
                    }).start();

                    log.info(String.format("#%d: Client %d connected.", id, newClientId));
                    listenToClient(bufferedReader, newClientId);
                    break;
                default:
                    break;
            }

        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    /**
     * Poll's messages from incoming queue and process them.
     */
    private void handleMessages() {
        while (true) {
            Message m = null;
            try {
                m = incomingMessages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(m == null)
                continue;
            log.info(String.format("Handling message: %s", m));

            if (m instanceof ReplicaMessage) {
                localReplica.receiveMessage((ReplicaMessage) m);
            }
            if (m instanceof LeaderMessage) {
                //localLeader.startLeader();
                localLeader.receiveMessage((LeaderMessage) m);
            }
            if (m instanceof AcceptorMessage) {
                localAcceptor.receiveMessage((AcceptorMessage) m);
            }

            if (m instanceof PingMessage) {
                sendToNode(m.getSource(), new PongMessage(id));
            }
        }
    }

    @Override
    public void close() throws Exception {
        stopping = true;
        inSocket.close();
        for (CommunicationEntry n : nodes) {
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
        while (!stopping) {
            try {
                String data = breader.readLine();
                Message m = Message.parse(nodeId, data.split(""));
                incomingMessages.offer(m);
            } catch (IOException e) {
                log.info(e.getMessage());
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
        log.info(String.format("#%d: Client %d connected.", id, clientId));
        while (!stopping) {
            try {
                String[] parts = reader.readLine().split(" ");
                ClientRequest message = ClientRequest.parse(clientId, parts);
                if (message != null) {
                    log.info(String.format("receiver message %s from client %d", message, message.getSource()));
                    incomingMessages.add(message);
                }
            } catch (IOException e) {
                log.info(String.format("Lost connection to Client %d: %s", clientId, e.getMessage()));
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
        Socket clientSocket = new Socket();
        nodes[nodeId].output = clientSocket;

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
                sendToNode(nodeId, new NodeMessage(id));
                OutputStreamWriter writer = new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET);

                while (!stopping) {
                    Message m = null;
                    try {
                        m = nodes[nodeId].messages.takeFirst();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

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
                        nodes[nodeId].messages.addFirst(m);
                    }
                }
            } catch (SocketException e) {
                log.info(String.format("#%d: Connection to node.%d lost: %s", id, nodeId, e.getMessage()));
            } catch (IOException e) {
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
                    log.info(String.format("#%d: Sending to client %d: %s", id, clientId, m));
                    writer.write(String.format("%s\n", m));
                    writer.flush();
                } catch (IOException ioe) {
                    log.info("Couldn't send a message. Retrying.");
                    nodes[clientId].messages.addFirst(m);
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
                    nodes[to].messages.put(message);
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
                continue;
            }
        }
    }
}

