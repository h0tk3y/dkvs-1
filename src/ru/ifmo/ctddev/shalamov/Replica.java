package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.DataMessage;
import ru.ifmo.ctddev.shalamov.messages.GetRequest;
import ru.ifmo.ctddev.shalamov.messages.Message;
import ru.ifmo.ctddev.shalamov.messages.ReplicaMessage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Replica {
    int id;
    List<Integer> leaderIds;
    private HashMap<String, String> map;

    /**
     * link to "local machine", where replica is running.
     */
    private Node machine;

    //public void send(int id, Message m);

    public volatile int slotIn = 0;
    public volatile int slotOut = 0;

    public Replica(int id, Node machine, List<Integer> leaderIds) {
        this.id = id;
        this.machine = machine;
        this.leaderIds = leaderIds;
        map = new HashMap<>();
    }

    /**
     * Should be called from the replica's container to pass to the replica each message
     * addressed to it.
     *
     * @param message Message that should be handled by the replica.
     */
    public void receiveMessage(ReplicaMessage message) {
        if(message instanceof GetRequest)
        {
            String key = ((GetRequest)message).key;
            String value = map.get(key);
            if(value == null)
                value = "none";

            machine.sendToClient(message.getSource(), new DataMessage(message.getSource(), value));
        }
        //TODO paxos required.
        throw new IllegalArgumentException("now implemented yet");
    }
}
