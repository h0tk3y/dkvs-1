package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class SetRequest extends ClientRequest {
    //int fromId;
    String key;
    String value;

    public SetRequest(int fromId, String key, String value) {
        super(fromId);
        //this.fromId = fromId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return System.out.format("set %d %s %s", fromId, key, value).toString();
    }
}
