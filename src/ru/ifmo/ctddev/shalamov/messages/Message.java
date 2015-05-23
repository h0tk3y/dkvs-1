package ru.ifmo.ctddev.shalamov.messages;

import java.util.Arrays;

import com.google.common.base.Joiner;

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

    public String getText() {
        return text;
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
                return new NodeMessage(fromId); // Integer.parseInt(parts[1]) - fromId is the same ans it.
            case "ping":
                return new PingMessage(fromId);
            case "pong":
                return new PongMessage(fromId);
            case "get":
                return new GetRequest(fromId, parts[1]);
            case "set":
                return new SetRequest(fromId, parts[1], parts[2]);
                        //Joiner.on(" ").join(Arrays.copyOfRange(parts, 2, parts.length)));
            case "delete":
                return new DeleteRequest(fromId, parts[1]);
            default:
                throw new IllegalArgumentException("Unknown message.");
        }
    }
}



