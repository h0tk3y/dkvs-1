package ru.ifmo.ctddev.shalamov;

import ru.ifmo.ctddev.shalamov.messages.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by viacheslav on 24.05.2015.
 */
public class DKVSlogger {

    // todo: not implementes yet.

    private int id;
    public java.util.logging.Logger log;

    public DKVSlogger(int id) {
        this.id = id;
        this.log = java.util.logging.Logger.getLogger("node." + id);
    }


    public void logConnection(String where, String s) {
        log.info(where + ":\n=== " + s);
    }

    public void logMessageOut(String where, String message) {
        log.info(where + ":\n<< " + message);
    }

    public void logMessageIn(String where, String message) {
        log.info(where + ":\n>> " + message);
    }

    public void logPaxos(String where, String s) {
        log.info(where + ":\n### " + s);
    }

    public void logError(String where, String s) {
        log.log(Level.INFO, where + ":\n!!! error: " + s);
    }

}
