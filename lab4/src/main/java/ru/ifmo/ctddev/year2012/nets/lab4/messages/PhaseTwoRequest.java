package ru.ifmo.ctddev.year2012.nets.lab4.messages;

import ru.ifmo.ctddev.year2012.nets.lab4.ProposalValue;


/**
 * Created by viacheslav on 23.05.2015.
 */
public class PhaseTwoRequest extends AcceptorMessage {
    public ProposalValue payload;

    public PhaseTwoRequest(int fromId, ProposalValue payload) {
        super(fromId, payload.ballotNum);
        this.payload = payload;
    }

    @Override
    public String toString() {
        return String.format("p2a %d %s", fromId, payload);
    }
}
