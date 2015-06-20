package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.ClientRequest;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Represents pvalue <b, s, c> of Multi-Paxos protocol.
 * See full reference at [http://www.cs.cornell.edu/courses/cs7412/2011sp/paxos.pdf]
 * <p>
 * Created by viacheslav on 23.05.2015.
 */
public class ProposalValue {
    public Ballot ballotNum;
    public int slot;
    public Descriptor command;

    ProposalValue(Ballot ballotNum, int slot, Descriptor command) {
        this.ballotNum = ballotNum;
        this.slot = slot;
        this.command = command;
    }

    @Override
    public String toString() {
        return String.format("%s %d %s", ballotNum, slot, command);
    }

    public static ProposalValue parse(String[] parts) {
//        String[] tail = Arrays.copyOfRange(parts, 4, parts.length);
//        String[] head = new String[1];
//        head[0] = parts[2];
//        String[] both = Stream.concat(Arrays.stream(head), Arrays.stream(tail))
//                .toArray(String[]::new);
        ProposalValue pv = new ProposalValue(Ballot.parse(parts[0]),
                Integer.parseInt(parts[1]),
                Descriptor.parse(Arrays.copyOfRange(parts, 2, parts.length)));
        return pv;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ProposalValue) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }

}