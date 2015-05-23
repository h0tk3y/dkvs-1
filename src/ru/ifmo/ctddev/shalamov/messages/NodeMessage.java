package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class NodeMessage extends Message {

    public NodeMessage(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return String.format("node %d", fromId);
    }
}
