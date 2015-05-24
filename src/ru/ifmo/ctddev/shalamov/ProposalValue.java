package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.ClientRequest;

import java.util.Arrays;

/**
 * Represents pvalue <b, s, c> of Multi-Paxos protocol.
 * See full reference at [http://www.cs.cornell.edu/courses/cs7412/2011sp/paxos.pdf]
 * <p>
 * Created by viacheslav on 23.05.2015.
 */
public class ProposalValue {
    public Ballot ballotNum;
    public int slot;
    public ClientRequest command;

    ProposalValue(Ballot ballotNum, int slot, ClientRequest command) {
        this.ballotNum = ballotNum;
        this.slot = slot;
        this.command = command;
    }

    @Override
    public String toString() {
        return String.format("<%s %d %s>", ballotNum, slot, command);
    }

    public static ProposalValue parse(String[] parts) {
        return new ProposalValue(Ballot.parse(parts[0]),
                Integer.parseInt(parts[1]),
                ClientRequest.parse(0, Arrays.copyOfRange(parts, 2, parts.length)));
    }
}