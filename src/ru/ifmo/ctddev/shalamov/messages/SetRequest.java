package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class SetRequest extends ClientRequest {
    //int fromId;
    public String key;
    public String value;

    public SetRequest(int fromId, String key, String value) {
        this.fromId = fromId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("[set %d, %s, %s]", fromId, key, value);
    }
}
