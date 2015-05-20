package ru.ifmo.ctddev.shalamov.messages;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class DeleteRequest extends ClientRequest {
    //int fromId;
    String key;

    public DeleteRequest(int fromId, String key) {
        super(fromId);
        //this.fromId = fromId;
        this.key = key;
    }

    @Override
    public String toString() {
        return System.out.format("delete %d %s", fromId, key).toString();
    }
}
