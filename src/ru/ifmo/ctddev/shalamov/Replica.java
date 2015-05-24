package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.*;

import java.util.*;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Replica {
    int id;
    List<Integer> leaderIds;
    private Persistence persistence;

    /**
     * link to "local machine", where replica is running.
     */
    private Node machine;


    public volatile int slotIn = 0;
    public volatile int slotOut = 0;

    private HashMap<String, String> data = new HashMap<>();

    private HashMap<ClientRequest, Integer> awaitingClients = new HashMap<>();
    private SortedSet<ClientRequest> requests = new TreeSet<>();
    private HashMap<Integer, ClientRequest> proposals = new HashMap<>();
    private HashMap<Integer, ClientRequest> decisions = new HashMap<>();
    private HashSet<ClientRequest> performed = new HashSet<>();

    public Replica(int id, Node machine, List<Integer> leaderIds) {
        this.id = id;
        this.machine = machine;
        this.leaderIds = leaderIds;
    }

    /**
     * Should be called from the replica's container to pass to the replica each message
     * addressed to it.
     *
     * @param message Message that should be handled by the replica.
     */
    public void receiveMessage(ReplicaMessage message) {
        if (message instanceof GetRequest) {
            String key = ((GetRequest) message).key;
            String value = data.get(key);
            if (value == null)
                value = "none";

            // todo make better protocol
            machine.sendToClient(message.getSource(), new DataMessage(message.getSource(), value));
        }
        if (message instanceof ClientRequest) {
            requests.add((ClientRequest) message);
            awaitingClients.put((ClientRequest) message, message.getSource());
        }


        if (message instanceof DecisionMessage) {
            ClientRequest actualRequest = ((DecisionMessage) message).request;
            int actualSlot = ((DecisionMessage) message).slot;

            machine.log.info(String.format("DECISION %s", message));
            decisions.put(actualSlot, actualRequest);

            // do some operations if possible:
            while (decisions.containsKey(slotOut)) {
                ClientRequest command = decisions.get(slotOut);
                if (proposals.containsKey(slotOut)) {
                    ClientRequest proposalCommand = proposals.get(slotOut);
                    proposals.remove(slotOut);

                    // some other command decided. repeat rejected command:
                    if (command != proposalCommand) {
                        requests.add(proposalCommand);
                    }
                }

                perform(command);
                ++slotOut;
            }
        }
        propose();

        //TODO need propose messages to paxos.

        throw new IllegalArgumentException("now implemented yet");
    }

    private void propose() {
        while (!requests.isEmpty()) {
            ClientRequest c = requests.first();
            machine.log.info(String.format("PROPOSING %s to %d", c, slotIn));
            if (!decisions.containsKey(slotIn)) {
                requests.remove(c);
                proposals.put(slotIn, c);
                leaderIds.forEach(l -> machine.sendToNode(l, new ProposeMessage(id, slotIn, c)));
            }
            ++slotIn;
        }
    }

    private void perform(ClientRequest c) {
        machine.log.info(String.format("PERFORMING %s at %d", c, slotOut));
        if (performed.contains(c))
            return;
        if (c instanceof SetRequest) {
            data.put(((SetRequest) c).key, ((SetRequest) c).value);
            Integer awaitingClient = awaitingClients.get(c);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new DataMessage(c.getSource(), "STORED"));
                awaitingClients.remove(awaitingClient);
            }
        }
        if (c instanceof DeleteRequest) {
            boolean result = (data.remove(((DeleteRequest) c).key)) != null;
            Integer awaitingClient = awaitingClients.get(c);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new DataMessage(c.getSource(), (result)? "DELETED": "NOT_FOUND"));
                awaitingClients.remove(awaitingClient);
            }
        }
        performed.add(c);

        // todo: maintain persistence
//        if (! (c instanceof GetRequest))
//            persistance.saveToDisk(String.format("slot %d %s", slotOut, c));
    }
}
