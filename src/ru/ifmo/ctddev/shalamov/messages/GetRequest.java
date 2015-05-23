package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class GetRequest extends ClientRequest {
    public String key;

    public GetRequest(int fromId, String key) {
        this.fromId = fromId;
        this.key = key;
    }

    @Override
    public String toString() {
        return System.out.format("get %d %s", fromId, key).toString();
    }
}
