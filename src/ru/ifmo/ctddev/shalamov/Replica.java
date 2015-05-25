package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.*;

import java.util.*;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Replica {
    int id;

    /**
     *  The set of leaders in the current conﬁguration. The leaders of the
     *  initial conﬁguration are passed as an argument to the replica.
     */
    List<Integer> leaderIds;

    // todo
    private Persistence persistence;

    /**
     * link to "local machine", where replica is running.
     */
    private Node machine;


    /**
     *  The index of the next slot in which the replica has not yet proposed any command.
     */
    public volatile int slotIn = 0;

    /**
     *  The index of the next slot for which it needs to learn a decision
     *  before it can update its copy of the application state, equivalent
     *  to the state’s version number (i.e., number of updates).
     */
    public volatile int slotOut = 0;

    /**
     *  The replica’s copy of the application state, which we will treat as opaque.
     *  All replicas start with the same initial application state.
     */
    private HashMap<String, String> state = new HashMap<>();

    /**
     *  An initially empty set of requests that the replica has received and are not yet proposed or decided.
     */
    private HashSet<ClientRequest> requests = new HashSet<>();

    /**
     *  An initially empty set of proposals that are currently outstanding.
     */
    private HashMap<Integer, ClientRequest> proposals = new HashMap<>();

    /**
     *  Another set of proposals that are known to have been decided (also initially empty).
     */
    private HashMap<Integer, ClientRequest> decisions = new HashMap<>();


    /**
     * clients not yet responced.
     */
    private HashMap<ClientRequest, Integer> awaitingClients = new HashMap<>();

    private HashSet<ClientRequest> performed = new HashSet<>();

    public Replica(int id, Node machine, List<Integer> leaderIds) {
        this.id = id;
        this.machine = machine;
        this.leaderIds = leaderIds;
    }

    /**
     * pass to the replica each message, addressed to it.
     * @param message Message that should be handled by the replica.
     */
    public void receiveMessage(ReplicaMessage message) {
        if (message instanceof GetRequest) {
            String key = ((GetRequest) message).key;
            String value = state.get(key);
            if (value == null)
                value = "NOT_FOUND";
            else
                value = "VALUE " + value;

            machine.sendToClient(message.getSource(), new ClientResponse(message.getSource(), value));
            return;
        }
        else if (message instanceof ClientRequest) {
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
    }

    private void propose() {
        while (!requests.isEmpty()) {
            ClientRequest c = requests.iterator().next();
            machine.log.info(String.format("PROPOSING %s to slot %d", c, slotIn));
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
            return;   // already performed.

        if (c instanceof SetRequest) {
            state.put(((SetRequest) c).key, ((SetRequest) c).value);
            Integer awaitingClient = awaitingClients.get(c);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new ClientResponse(c.getSource(), "STORED"));
                awaitingClients.remove(awaitingClient);
            }
        }
        if (c instanceof DeleteRequest) {
            boolean result = (state.remove(((DeleteRequest) c).key)) != null;
            Integer awaitingClient = awaitingClients.get(c);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new ClientResponse(c.getSource(), (result)? "DELETED": "NOT_FOUND"));
                awaitingClients.remove(awaitingClient);
            }
        }
        performed.add(c);

        // todo: maintain persistence
//        if (! (c instanceof GetRequest))
//            persistance.saveToDisk(String.format("slot %d %s", slotOut, c));
    }
}
