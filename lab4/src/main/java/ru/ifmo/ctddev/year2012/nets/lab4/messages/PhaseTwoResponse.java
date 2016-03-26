package ru.ifmo.ctddev.year2012.nets.lab4.messages;

import ru.ifmo.ctddev.year2012.nets.lab4.Ballot;
import ru.ifmo.ctddev.year2012.nets.lab4.ProposalValue;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class PhaseTwoResponse extends LeaderMessage {
    public Ballot ballot;
    public ProposalValue proposal;

    public PhaseTwoResponse(int fromId, Ballot ballot, ProposalValue proposal) {
        super(fromId);
        this.ballot = ballot;
        this.proposal = proposal;
    }

    @Override
    public String toString() {
        return String.format("p2b %d %s %s", fromId, ballot, proposal);
    }
}
