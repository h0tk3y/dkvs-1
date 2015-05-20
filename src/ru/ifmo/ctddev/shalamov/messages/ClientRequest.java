package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public abstract class ClientRequest extends ReplicaMessage {
    int clientId;

    ClientRequest(int fromId) {
        super(fromId);
    }
}
