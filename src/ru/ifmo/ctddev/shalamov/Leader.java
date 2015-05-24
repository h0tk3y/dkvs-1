package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.*;

import java.util.*;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class Leader {
    int id;
    Node machine;
    List<Integer> replicaIds;
    List<Integer> acceptorIds;

    public volatile boolean active;

    /**
     * A monotonically increasing ballot number, initially (0,id);
     */
    public volatile Ballot currentBallot;

    /**
     * A map of slot numbers to proposed commands in the form of a set of
     * (slot number, command) pairs, initially empty. At any time, there is
     * at most one entry per slot number in the set).
     */
    private HashMap<Integer, ClientRequest> proposals;


    private HashMap<ProposalValue, Commander> commanders;
    private HashMap<Ballot, Scout> scouts;


    public Leader(int id, Node machine, List<Integer> replicaIds, List<Integer> acceptorIds) {
        this.id = id;
        this.machine = machine;
        this.acceptorIds = acceptorIds;
        this.replicaIds = replicaIds;
        proposals = new HashMap<>();
        currentBallot = new Ballot(1, id);
        active = (id == 1);

        commanders = new HashMap<>();
        scouts = new HashMap<>();
    }

    public void receiveMessage(LeaderMessage message) {
        if (message instanceof ProposeMessage) {
            if (!proposals.containsKey(((ProposeMessage) message).slot)) {
                proposals.put(((ProposeMessage) message).slot, ((ProposeMessage) message).request);
                if (active) {
                    command(new ProposalValue(currentBallot, ((ProposeMessage) message).slot,
                            ((ProposeMessage) message).request));
                }
            }
        }
        if (message instanceof PhaseOneResponse) {
            Ballot ballot = ((PhaseOneResponse) message).ballotNum;
            Scout scout = scouts.get(ballot);
            scout.receiveResonse((PhaseOneResponse) message);
        }
        if (message instanceof PhaseTwoResponse) {
            ProposalValue proposal = ((PhaseTwoResponse) message).proposal;
            Commander commander = commanders.get(proposal);
            commander.receiveResponse((PhaseTwoResponse) message);
        }

    }

    /**
     * Sent by either a scout or a commander, it means that some acceptor has adopted (r, L').
     * If(r, L') > ballot_num, it may no longer be possible
     * to use ballot ballot num to choose a command.
     *
     * @param b
     */
    private void preempted(Ballot b) {
        machine.log.info(String.format("PREEMPTED: there's ballot %s", b));
        if (b.compareTo(currentBallot) > 0) {
            active = false;
//            currentBallot = new Ballot(nextBallotNum(), id);
            machine.log.info(String.format("WAITING for %d to fail", b.leaderId));
        }
    }

    /**
     * Sent by a scout,this message signiﬁes that the current ballot number
     * ballot_num has been adopted by a majority of acceptors.
     * (If an adopted message arrives for an old ballot number, it is ignored [by Scout].)
     * The set pvalues contains all pvalues accepted by these acceptors prior to ballot_num.
     *
     * @param ballot
     * @param pvalues
     */
    private void adopted(Ballot ballot, Map<Integer, ProposalValue> pvalues) {
        machine.log.info(String.format("ADOPTED with ballot %s", ballot));

        for (Map.Entry<Integer, ProposalValue> entry : pvalues.entrySet()) {
            Integer key = entry.getKey();
            ProposalValue value = entry.getValue();
            proposals.put(key, value.command);
        }
        active = true;

        for (Map.Entry<Integer, ClientRequest> entry : proposals.entrySet()) {
            Integer key = entry.getKey();
            ClientRequest value = entry.getValue();
            command(new ProposalValue(ballot, key, value));
        }
    }

    private class Scout {
        HashSet<Integer> waitFor;
        HashMap<Integer, ProposalValue> proposals;
        Ballot b;

        public Scout(Ballot b) {
            this.b = b;
            waitFor = new HashSet<>(acceptorIds);
            proposals = new HashMap<>();
        }

        public void receiveResonse(PhaseOneResponse response) {
            if (response.ballotNum == currentBallot) {
                response.pvalues.forEach(r ->
                        {
                            if (!proposals.containsKey(r.slot) ||
                                    r.ballotNum.compareTo(proposals.get(r.slot).ballotNum) > 0) ;
                            proposals.put(r.slot, r);
                        }
                );
                waitFor.remove(response.getSource());

                if (waitFor.size() < acceptorIds.size() / 2) {
                    adopted(currentBallot, proposals);
                    scouts.remove(currentBallot);
                }
            } else {
                preempted(response.ballotNum);
            }
        }
    }

    private void startScouting(Ballot ballot) {
        scouts.put(ballot, new Scout(currentBallot));
        acceptorIds.forEach(a -> machine.sendToNode(a, new PhaseOneRequest(id, ballot)));
    }

    private class Commander {
        ProposalValue proposal;
        HashSet<Integer> waitFor;

        public Commander(ProposalValue proposal) {
            this.proposal = proposal;
            this.waitFor = new HashSet<>(acceptorIds);
        }

        public void receiveResponse(PhaseTwoResponse response) {
            if (response.ballot == currentBallot) {
                waitFor.remove(response.getSource());
                if (waitFor.isEmpty() || waitFor.size() < acceptorIds.size() / 2) {
                    replicaIds.forEach(r ->
                                    machine.sendToNode(r, new DecisionMessage(response.proposal.slot,
                                            response.proposal.command))
                    );
                    commanders.remove(proposal);
                }
            } else
                preempted(response.ballot);
        }
    }


    private void command(ProposalValue proposal) {
        machine.log.info(String.format("COMMANDER started for %s", proposal));
        commanders.put(proposal, new Commander(proposal));
        acceptorIds.forEach(a -> machine.sendToNode(a, new PhaseTwoRequest(id, proposal)));
    }

}



