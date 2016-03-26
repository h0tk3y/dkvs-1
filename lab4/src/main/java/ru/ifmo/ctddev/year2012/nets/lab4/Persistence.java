package ru.ifmo.ctddev.year2012.nets.lab4;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Created by viacheslav on 24.05.2015.
 */
public class Persistence {
    int nodeId;

    public String fileName;
    private BufferedWriter writer = null;

    /**
     * the first not used high-part of ballots.
     */
    public volatile int lastBallotNum = 0;

    /**
     * This is the actual storage of all data!
     * Replica has link to it!!
     * may be I need better design.
     */
    public volatile HashMap<String, String> keyValueStorage;

    /**
     * last executed slot.
     */
    public volatile int lastSlotOut = -1;

    public Persistence(int nodeId) {
        this.nodeId = nodeId;
        fileName = String.format("dkvs_%d.log", nodeId);
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        keyValueStorage = new HashMap<>();

        BufferedReader reader = null;

        try {
            File file = new File(fileName);
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("no log file!!!");
            System.exit(1);
        }

        ArrayList<String> lines = new ArrayList<>();
        for (String line : reader.lines().collect(Collectors.toList()))
            lines.add(line);
        Collections.reverse(lines);

        HashMap<String, String> temporaryStorage = new HashMap<>();
        HashSet<String> removedKeys = new HashSet<>();


        here:
        for (String l : lines) {
            String[] parts = l.split(" ");
            switch (parts[0]) {
                case "ballot":
                    lastBallotNum = Math.max(lastBallotNum, Ballot.parse(parts[1]).ballotNum);
                    break;
                case "slot":
                    String key = (parts.length >= 5) ? parts[5] : null;
                    lastSlotOut = Math.max(lastSlotOut, Integer.parseInt(parts[1]));
                    if (temporaryStorage.containsKey(key)
                            || removedKeys.contains(key))
                        continue here;
                    switch (parts[3]) {
                        case "set":
                            temporaryStorage.put(key, parts[6]);
                            //Joiner.on(" ").join(Arrays.copyOfRange(parts, 5, parts.length - 1)));
                            break;
                        case "delete":
                            removedKeys.add(key);
                            break;
                    }
                    break;
            }
        }

        keyValueStorage = temporaryStorage;

        for (String l : lines) {
            String[] parts = l.split(" ");
            if ("ballot".equals(parts[0])) {
                lastBallotNum = Ballot.parse(parts[1]).ballotNum;
                break;
            }
        }
    }

    public void saveToDisk(String s) {
        try {
            writer.write(s);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("unable to write to file");
            System.exit(1);
        }
    }

    public int nextBallotNum() {
        return ++lastBallotNum;
    }
}
