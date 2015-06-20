package ru.ifmo.ctddev.shalamov.messages;

import ru.ifmo.ctddev.shalamov.Descriptor;

/**
 * Created by viacheslav on 24.05.2015.
 */
public class DecisionMessage extends ReplicaMessage {
    public int slot;
    public Descriptor request;

    public DecisionMessage(int slot, Descriptor descriptor) {
        super();
        this.slot = slot;
        this.request = descriptor;
    }

    @Override
    public String toString() {
        return String.format("decision %d %s", slot, request);
    }

}
