package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 24.05.2015.
 */
public class DecisionMessage extends ReplicaMessage {
    public int slot;
    public ClientRequest request;

    public DecisionMessage(int slot, ClientRequest request) {
        super();
        this.slot = slot;
        this.request = request;
    }

    @Override
    public String toString() {
        return String.format("decision %d %s", slot, request);
    }

}
