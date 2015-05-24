package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class ProposeMessage extends LeaderMessage {
    public int slot;
    public ClientRequest request;

    ProposeMessage(int fromId, int slot, ClientRequest request) {
        super(fromId);
        this.slot = slot;
        this.request = request;
    }

    @Override
    public String toString() {
        return String.format("propose %d %d %s", fromId, slot, request);
    }
}
