package ru.ifmo.ctddev.shalamov.messages;

import com.google.common.base.Joiner;
import ru.ifmo.ctddev.shalamov.Ballot;
import ru.ifmo.ctddev.shalamov.ProposalValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class PhaseOneResponse extends LeaderMessage {
    public Ballot originalBallot;
    public Ballot ballotNum;
    public Collection<ProposalValue> pvalues;

    public PhaseOneResponse(int fromId, Ballot originalBallot,
                            Ballot ballotNum, Collection<ProposalValue> pvalues) {
        super(fromId);
        this.originalBallot = originalBallot;
        this.ballotNum = ballotNum;
        this.pvalues = pvalues;
    }

    @Override
    public String toString() {
        return String.format("p1b %d %s %s %s", fromId, originalBallot, ballotNum,
                Joiner.on("_#_").join(pvalues));
    }

    public static PhaseOneResponse parse(String[] parts) {
        if (! "p1b".equals(parts[0]))
            throw new IllegalArgumentException("PhaseOneResponse should start by \"p1b\"");

        int fromId = Integer.parseInt(parts[1]);
        Ballot originalBallot = Ballot.parse(parts[2]);
        Ballot ballotNum = Ballot.parse(parts[3]);
        String[] ss = Joiner.on(" ").
                join(Arrays.copyOfRange(parts, 4, parts.length)).split("_#_");
        List<ProposalValue> pvalues = new ArrayList<>(Arrays.asList(ss)).stream()
                .filter(s -> s.length() > 0)
                .map(s -> ProposalValue.parse(s.split(" ")))
                .collect(Collectors.toList());

        return new PhaseOneResponse(fromId, originalBallot, ballotNum, new LinkedHashSet(pvalues));
    }


}
