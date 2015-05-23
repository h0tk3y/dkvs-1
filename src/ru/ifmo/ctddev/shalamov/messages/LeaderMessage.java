package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 23.05.2015.
 */
public abstract class LeaderMessage extends Message {
    public LeaderMessage(int fromId) {
        this.fromId = fromId;
    }
}
