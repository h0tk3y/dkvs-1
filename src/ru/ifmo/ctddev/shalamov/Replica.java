package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.GetRequest;
import ru.ifmo.ctddev.shalamov.messages.Message;
import ru.ifmo.ctddev.shalamov.messages.ReplicaMessage;

import java.util.HashMap;
import java.util.List;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Replica {
    int id;
    List<Integer> leaderIds;
    private HashMap<String, String> map;

    //public void send(int id, Message m);

    public volatile int slotIn = 0;
    public volatile int slotOut = 0;

    public Replica(int id, List<Integer> leaderIds) {
        this.id = id;
        this.leaderIds = leaderIds;
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
            String value = map.get(((GetRequest)message).key);
            if(value == null)
                value = "none";
            // TODO: replica should send the message BACK;
        }
    }
}
