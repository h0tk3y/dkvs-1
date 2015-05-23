package ru.ifmo.ctddev.shalamov;

/**
 * Created by viacheslav on 23.05.2015.
 */
public class Ballot implements Comparable<Ballot> {
    int ballotNum;
    int leaderId;

    public Ballot(int ballotNum, int leaderId) {
        this.ballotNum = ballotNum;
        this.leaderId = leaderId;
    }


    @Override
    public int compareTo(Ballot other) {
        int result = new Integer(ballotNum).compareTo(other.ballotNum);
        if (result != 0)
            return result;
        else
            return new Integer(leaderId).compareTo(other.leaderId);
    }

    @Override
    public String toString() {
        return String.format("%d_%d", ballotNum, leaderId);
    }

    public static Ballot parse(String s) {
        String[] parts = s.split("_");
        return new Ballot(Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]));
    }
}
