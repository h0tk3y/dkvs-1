package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class DataMessage extends ReplicaMessage {
    public String data;

    public DataMessage(int fromId, String data) {
        this.fromId = fromId;
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }
}

