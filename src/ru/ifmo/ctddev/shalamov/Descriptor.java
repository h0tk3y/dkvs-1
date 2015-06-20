package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.ClientRequest;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by viacheslav on 04.06.2015.
 */
public class Descriptor {
    public int operationId;
    private static volatile int nextId = 0;
    public ClientRequest request;

    public Descriptor(int nodeId, ClientRequest request) {
        int curId = get();
        this.operationId = curId * Node.globalConfig.nodesCount() + nodeId;
        this.request = request;
    }

    private Descriptor(ClientRequest request, int operationId) {
        this.operationId = operationId;
        this.request = request;
    }

    public static synchronized int get() {
        return nextId++;
    }

    public static Descriptor parse(String[] parts) {
        String[] tail = Arrays.copyOfRange(parts, 3, parts.length);
        String[] head = new String[1];
        head[0] = parts[1];
        String[] both = Stream.concat(Arrays.stream(head), Arrays.stream(tail))
                .toArray(String[]::new);

        // todo make code more pretty by creating function except(array, index);

        return new Descriptor(ClientRequest.parse(Integer.parseInt(parts[2]), both),
                Integer.parseInt(parts[0].substring(1, parts[0].length() - 1)));

    }

    @Override
    public String toString() {
        return "<" + operationId + "> " + request.toString();
    }

    @Override
    public boolean equals(Object other) {
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
