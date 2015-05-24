package ru.ifmo.ctddev.shalamov;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by viacheslav on 24.05.2015.
 */
public class Persistence {
    int nodeId;

    public String fileName;
    private FileWriter writer = null;

    public volatile int lastBallotNum = 0;

    /**
     * This is the actual storage of all data!
     * Replica has link to it!!
     * may be I need better design.
     */
    public volatile HashMap<String, String> keyValueStorage;

    public volatile int lastSlotOut = 0;

    public Persistence(int nodeId) {
        this.nodeId = nodeId;
        fileName = String.format("dkvs_%d.log", nodeId);
        try {
            writer = new FileWriter(fileName, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        keyValueStorage = new HashMap<>();
    }


    public synchronized int nextBallotNum() {
        lastBallotNum += 1;
        return lastBallotNum;
    }

    public void saveToDisk(Object data) {
        synchronized (writer) {
            try {
                writer.append(data.toString());
                writer.append('\n');
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public HashMap<String, String> restoreFromDisc() {
        return null;
    }
}
