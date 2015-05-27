package ru.ifmo.ctddev.shalamov.messages;

import com.google.common.base.Joiner;

import java.util.Arrays;

/**
 * Created by viacheslav on 20.05.2015.
 */
public abstract class ClientRequest extends ReplicaMessage {


    public static ClientRequest parse(int clientId, String[] parts) {
        switch (parts[0]) {
            case "get":
                return new GetRequest(clientId, parts[1]);
            case "set":
                return new SetRequest(clientId, parts[1], parts[2]);
                        //Joiner.on(" ").join(Arrays.copyOfRange(parts, 2, parts.length)));
            case "delete":
                return new DeleteRequest(clientId, parts[1]);
            default:
                throw new IllegalArgumentException("Unknown client request");
        }
    }
}
