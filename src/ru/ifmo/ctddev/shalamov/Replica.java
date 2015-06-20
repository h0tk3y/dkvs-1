package ru.ifmo.ctddev.shalamov;

import com.sun.crypto.provider.DESCipher;
import ru.ifmo.ctddev.shalamov.messages.*;

import java.util.*;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class Replica {
    int id;

    /**
     * The set of leaders in the current conﬁguration. The leaders of the
     * initial conﬁguration are passed as an argument to the replica.
     */
    List<Integer> leaderIds;


    //private Persistence persistence;

    /**
     * link to "local machine", where replica is running.
     */
    private Node machine;


    /**
     * The index of the next slot in which the replica has not yet proposed any command.
     */
    public volatile int slotIn = 0;

    /**
     * The index of the next slot for which it needs to learn a decision
     * before it can update its copy of the application state, equivalent
     * to the state’s version number (i.e., number of updates).
     */
    public volatile int slotOut = 0;

    /**
     * The replica’s copy of the application state, which we will treat as opaque.
     * All replicas start with the same initial application state.
     */
    private HashMap<String, String> state;

    /**
     * An initially empty set of requests that the replica has received and are not yet proposed or decided.
     */
    private HashSet<Descriptor> requests = new HashSet<>();

    /**
     * An initially empty set of proposals that are currently outstanding.
     */
    private HashMap<Integer, Descriptor> proposals = new HashMap<>();

    /**
     * Another set of proposals that are known to have been decided (also initially empty).
     */
    private HashMap<Integer, Descriptor> decisions = new HashMap<>();


    /**
     * clients not yet responced.
     */
    private HashMap<Descriptor, Integer> awaitingClients = new HashMap<>();

    private HashSet<Descriptor> performed = new HashSet<>();

    public Replica(int id, Node machine) {
        this.id = id;
        this.machine = machine;
        this.leaderIds = machine.globalConfig.ids();

        state = machine.persistence.keyValueStorage;
        slotOut = machine.persistence.lastSlotOut + 1;
        slotIn = slotOut;
    }

    /**
     * pass to the replica each message, addressed to it.
     *
     * @param message Message that should be handled by the replica.
     */
    public void receiveMessage(ReplicaMessage message) {
        if (message instanceof GetRequest) {
            // todo make code more pretty
            String key = ((GetRequest) message).key;
            String value = state.get(key);
            if (value == null)
                value = "NOT_FOUND";
            else
                value = "VALUE " + value;

            machine.sendToClient(message.getSource(), new ClientResponse(message.getSource(), value));
            return;
        } else if (message instanceof ClientRequest) {
            Descriptor descriptor = new Descriptor(id, (ClientRequest) message);
            requests.add(descriptor);
            awaitingClients.put(descriptor, message.getSource());
        }

        if (message instanceof DecisionMessage) {
            Descriptor actualRequestDescriptor = ((DecisionMessage) message).request;
            int actualSlot = ((DecisionMessage) message).slot;

            machine.logger.logPaxos("receiveMessage(message)", String.format("DECISION %s", message));
            decisions.put(actualSlot, actualRequestDescriptor);

            // do some operations if possible:
            while (decisions.containsKey(slotOut)) {
                Descriptor command = decisions.get(slotOut);
                if (proposals.containsKey(slotOut)) {
                    Descriptor proposalCommand = proposals.get(slotOut);
                    proposals.remove(slotOut);

                    // some other command decided. repeat rejected command:
                    if (!command.equals(proposalCommand)) {
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
            Descriptor descriptor = requests.iterator().next();
            machine.logger.logPaxos("propose()", String.format("PROPOSING %s to slot %d", descriptor, slotIn));
            if (!decisions.containsKey(slotIn)) {
                requests.remove(descriptor);
                proposals.put(slotIn, descriptor);
                leaderIds.forEach(l -> machine.sendToNode(l, new ProposeMessage(id, slotIn, descriptor)));
            }
            ++slotIn;
        }
    }

    private void perform(Descriptor descriptor) {
        machine.logger.logPaxos("perform()", String.format("PERFORMING %s at %d", descriptor, slotOut));
        if (performed.contains(descriptor))
            return;   // already performed.

        if (descriptor.request instanceof SetRequest) {
            state.put(((SetRequest) descriptor.request).key, ((SetRequest) descriptor.request).value);
            Integer awaitingClient = awaitingClients.get(descriptor);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, new ClientResponse(descriptor.request.getSource(), "STORED"));
                awaitingClients.remove(awaitingClient);
            }
        }
        if (descriptor.request instanceof DeleteRequest) {
            if (performed.contains(descriptor))
                return;
            boolean result = state.containsKey(((DeleteRequest) descriptor.request).key);
            state.remove(((DeleteRequest) descriptor.request).key);

            ClientResponse resp = new ClientResponse(descriptor.request.getSource(), (result) ? "DELETED" : "NOT_FOUND");

            Integer awaitingClient = awaitingClients.get(descriptor);
            if (awaitingClient != null) {
                machine.sendToClient(awaitingClient, resp);
                awaitingClients.remove(awaitingClient);
            }
        }
        performed.add(descriptor);

        if (!(descriptor.request instanceof GetRequest))
            machine.persistence.saveToDisk(String.format("slot %d %s", slotOut, descriptor));
    }
}
