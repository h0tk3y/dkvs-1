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
        return String.format("get %d, %s", fromId, key);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GetRequest) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }
}
