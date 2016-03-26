package ru.ifmo.ctddev.year2012.nets.lab4.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public abstract class ClientRequest extends ReplicaMessage {


    public static ClientRequest parse(int clientId, String[] parts) throws IllegalArgumentException {
        if (parts.length < 2)
            throw new IllegalArgumentException("Unknown client request");
        switch (parts[0]) {
            case "get":
                return new GetRequest(clientId, parts[1]);
            case "set":
                if (parts.length < 3)
                    throw new IllegalArgumentException("Incorrect SET request");
                return new SetRequest(clientId, parts[1], parts[2]);
            //Joiner.on(" ").join(Arrays.copyOfRange(parts, 2, parts.length)));
            case "delete":
                return new DeleteRequest(clientId, parts[1]);
            default:
                throw new IllegalArgumentException("Unknown client request");
        }
    }
}
