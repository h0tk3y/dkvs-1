package ru.ifmo.ctddev.shalamov.messages;

import com.sun.scenario.effect.impl.prism.ps.PPSBlend_SRC_INPeer;

/**
 * Created by viacheslav on 20.05.2015.
 */
public class PingMessage extends Message {
    public PingMessage(int fromId) {
        this.fromId = fromId;
    }


    @Override
    public String toString() {
        return "ping";
    }
}
