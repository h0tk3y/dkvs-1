package ru.ifmo.ctddev.year2012.nets.lab4.messages;

/**
 * Created by viacheslav on 23.05.2015.
 */
public abstract class LeaderMessage extends Message {
    public LeaderMessage(int fromId) {
        this.fromId = fromId;
    }
}
