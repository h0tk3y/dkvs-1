package ru.ifmo.ctddev.shalamov.messages;

import java.util.Arrays;

import com.google.common.base.Joiner;

/**
 * Messages between [Node]s and Clients. Abstract message class.
 * <p>
 * Created by viacheslav on 20.05.2015.
 */
public abstract class Message {

    /**
     * dispatches the message text and creates appropriate Message-subclass.
     *
     * @param parts
     * @return
     */
    public static Message parse(String[] parts) {
        switch (parts[0]) {
            case "node":
                return new NodeMessage(Integer.parseInt(parts[1]));
            case "ping":
                return new PingMessage();
            case "pong":
                return new PongMessage();
            case "get":
                return new GetRequest(Integer.parseInt(parts[1]), parts[2]);
            case "set":
                return new SetRequest(Integer.parseInt(parts[1]), parts[2],
                        Joiner.on(" ").join(Arrays.copyOfRange(parts, 3, parts.length)));
            case "delete":
                return new DeleteRequest(Integer.parseInt(parts[1]), parts[2]);
            default:
                throw new IllegalArgumentException("Unknown message.");
        }
    }
}



