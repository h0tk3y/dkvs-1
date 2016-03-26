package ru.ifmo.ctddev.year2012.nets.lab4.messages;

import ru.ifmo.ctddev.year2012.nets.lab4.Descriptor;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class ProposeMessage extends LeaderMessage {
    public int slot;
    public Descriptor request;

    public ProposeMessage(int fromId, int slot, Descriptor descriptor) {
        super(fromId);
        this.slot = slot;
        this.request = descriptor;
    }

    @Override
    public String toString() {
        return String.format("propose %d %d %s", fromId, slot, request);
    }
}
