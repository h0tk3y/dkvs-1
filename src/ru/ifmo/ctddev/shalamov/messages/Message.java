package ru.ifmo.ctddev.shalamov.messages;

import java.util.Arrays;

import com.google.common.base.Joiner;
import ru.ifmo.ctddev.shalamov.Ballot;
import ru.ifmo.ctddev.shalamov.ProposalValue;

/**
 * Messages between [Node]s and Clients. Abstract message class.
 * <p>
 * Created by viacheslav on 20.05.2015.
 */
public abstract class Message {
    protected String text;
    protected int fromId;

    public int getSource() {
        return fromId;
    }


    /**
     * dispatches the message text and creates appropriate Message-subclass.
     *
     * @param parts
     * @return
     */
    public static Message parse(int fromId, String[] parts) {
        //System.out.println(Joiner.on(" ").join(parts));
        switch (parts[0]) {
            case "node":
                return new NodeMessage(Integer.parseInt(parts[1])); //- fromId is the same ans it.
            case "ping":
                return new PingMessage(fromId);
            case "pong":
                return new PongMessage(fromId);
//            case "get":
//                return new GetRequest(fromId, parts[1]);
//            case "set":
//                return new SetRequest(fromId, parts[1], parts[2]);
//                        //Joiner.on(" ").join(Arrays.copyOfRange(parts, 2, parts.length)));
//            case "delete":
//                return new DeleteRequest(fromId, parts[1]);

            case "decision":
                return new DecisionMessage(Integer.parseInt(parts[1]),
                        ClientRequest.parse(fromId, Arrays.copyOfRange(parts, 2, parts.length)));

            case "propose": return new ProposeMessage(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                     ClientRequest.parse(fromId, Arrays.copyOfRange(parts, 3, parts.length)));
            case "p1a": return new PhaseOneRequest(Integer.parseInt(parts[1]), Ballot.parse(parts[2]));
            case "p2a": return new PhaseTwoRequest(Integer.parseInt(parts[1]),
                    ProposalValue.parse(Arrays.copyOfRange(parts, 2, parts.length)));
            case "p1b": return PhaseOneResponse.parse(parts);
            case "p2b": return new PhaseTwoResponse(Integer.parseInt(parts[1]), Ballot.parse(parts[2]),
                    ProposalValue.parse(Arrays.copyOfRange(parts, 3, parts.length)));
            default:
                throw new IllegalArgumentException("Unknown message.");
        }
    }
}



