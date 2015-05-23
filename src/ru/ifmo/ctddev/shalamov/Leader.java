package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.ClientRequest;
import ru.ifmo.ctddev.shalamov.messages.LeaderMessage;

import java.util.HashMap;
import java.util.List;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class Leader {
    int id;
    Node machine;
    List<Integer> replicaIds;
    List<Integer> acceptorIds;

    public volatile boolean active;
    public volatile Ballot currentBallot;

    private HashMap<Integer, ClientRequest> proposals;

    public Leader(int id, Node machine, List<Integer> replicaIds, List<Integer> acceptorIds) {
        this.id = id;
        this.machine = machine;
        this.acceptorIds = acceptorIds;
        this.replicaIds = replicaIds;
        proposals = new HashMap<>();
        currentBallot = new Ballot(1, id);
        active = (id == 1);
    }

    public void receiveMessage(LeaderMessage message) {

    }


}
