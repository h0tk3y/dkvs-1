package ru.ifmo.ctddev.shalamov;

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


    public void logConnection() {
    }

    public void logMessageOut() {

    }

    public void logMessageIn() {

    }

    public void LogPaxos() {
    }

    public void LogError() {
    }

}
