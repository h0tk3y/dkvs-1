package ru.ifmo.ctddev.shalamov.messages;

import ru.ifmo.ctddev.shalamov.Ballot;

/**
 * Created by viacheslav on 23.05.2015.
 */
public abstract class AcceptorMessage extends Message {

    public Ballot ballotNum;

    AcceptorMessage(int fromId, Ballot ballotNum) {
        this.ballotNum = ballotNum;
        this.fromId = fromId;
    }
}
