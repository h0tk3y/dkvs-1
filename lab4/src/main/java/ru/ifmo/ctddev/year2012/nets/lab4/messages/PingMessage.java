package ru.ifmo.ctddev.year2012.nets.lab4.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class PingMessage extends Message {
    public PingMessage(int fromId) {
        this.fromId = fromId;
    }


    @Override
    public String toString() {
        return "ping from " + fromId;
    }
}
