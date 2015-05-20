package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class NodeMessage extends Message {
    private int fromId;

    public NodeMessage(int id) {
        fromId = id;
    }

    @Override
    public String toString() {
        return String.format("node %d", fromId).toString();
    }
}
