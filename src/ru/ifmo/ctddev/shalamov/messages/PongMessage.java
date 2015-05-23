package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class PongMessage extends Message {
    public PongMessage(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return "pong";
    }
}
