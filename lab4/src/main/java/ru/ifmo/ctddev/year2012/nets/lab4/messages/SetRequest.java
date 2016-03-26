package ru.ifmo.ctddev.year2012.nets.lab4.messages;

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
        return String.format("set %d %s %s", fromId, key, value);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SetRequest) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }
}
