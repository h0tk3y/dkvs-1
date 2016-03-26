package ru.ifmo.ctddev.year2012.nets.lab4.messages;

import ru.ifmo.ctddev.year2012.nets.lab4.Ballot;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class PhaseOneRequest extends AcceptorMessage {
    public PhaseOneRequest(int fromId, Ballot ballotNum) {
        super(fromId, ballotNum);
    }

    @Override
    public String toString() {
        return String.format("p1a %d %s", fromId, ballotNum);
    }
}
