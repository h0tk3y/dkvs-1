package ru.ifmo.ctddev.shalamov.messages;

import ru.ifmo.ctddev.shalamov.ProposalValue;


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
        return String.format("<p2a %d, %s>", fromId, payload);
    }
}
