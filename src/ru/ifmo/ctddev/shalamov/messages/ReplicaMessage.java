package ru.ifmo.ctddev.shalamov.messages;

/**
 * messages, addressed to replicas.
 * <p>
 * Created by viacheslav on 20.05.2015.
 */
public abstract class ReplicaMessage extends Message {
    int fromId;

    ReplicaMessage(int fromId) {
        this.fromId = fromId;
    }
}
