package ru.ifmo.ctddev.year2012.nets.lab4.messages;

import ru.ifmo.ctddev.year2012.nets.lab4.Ballot;

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
