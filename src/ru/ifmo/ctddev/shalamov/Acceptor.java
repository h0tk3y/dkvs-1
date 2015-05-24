package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.AcceptorMessage;
import ru.ifmo.ctddev.shalamov.messages.PhaseOneRequest;
import ru.ifmo.ctddev.shalamov.messages.PhaseOneResponse;
import ru.ifmo.ctddev.shalamov.messages.PhaseTwoRequest;
import ru.ifmo.ctddev.shalamov.messages.PhaseTwoResponse;

import java.util.HashMap;

/**
 * An acceptor is passive and only sends messages in response to requests.
 * It runs in an inﬁnite loop, receiving two kinds of request messages from leaders.
 * <p>
 * See full reference at [http://www.cs.cornell.edu/courses/cs7412/2011sp/paxos.pdf]
 * <p>
 * Created by viacheslav on 23.05.2015.
 */
public class Acceptor {
    private int id;
    private volatile Ballot ballotNumber;

    /**
     * link to "local machine", where acceptor is running.
     */
    private Node machine;

    //Slot -> most recent AcceptProposal
    private HashMap<Integer, ProposalValue> accepted;

    public Acceptor(int id, Node machine) {
        this.id = id;
        this.machine = machine;
        this.ballotNumber = new Ballot(-1, 0);
        //    Ballot(1,globalConfig.ids.first()
        this.accepted = new HashMap<>();
    }

    public void receiveMessage(AcceptorMessage message) {
        if (message instanceof PhaseOneRequest) {
            if (message.ballotNum.compareTo(ballotNumber) > 0)
                ballotNumber = message.ballotNum;
            machine.sendToNode(message.getSource(),
                    new PhaseOneResponse(id, message.ballotNum, ballotNumber, accepted.values()));
            return;
        }
        if (message instanceof PhaseTwoRequest) {
            if (message.ballotNum == ballotNumber)
                accepted.put(((PhaseTwoRequest) message).payload.slot, ((PhaseTwoRequest) message).payload);
            machine.sendToNode(message.getSource(),
                    new PhaseTwoResponse(id, ballotNumber, ((PhaseTwoRequest) message).payload));
            return;
        }
        throw new IllegalStateException("Incorrect message");
    }
}


