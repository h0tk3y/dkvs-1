package ru.ifmo.ctddev.shalamov.messages;

import ru.ifmo.ctddev.shalamov.Ballot;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class PhaseOneRequest extends AcceptorMessage {
    PhaseOneRequest(int fromId, Ballot ballotNum) {
        super(fromId, ballotNum);
    }

    @Override
    public String toString() {
        return String.format("p1a %d %s", fromId, ballotNum);
    }
}
