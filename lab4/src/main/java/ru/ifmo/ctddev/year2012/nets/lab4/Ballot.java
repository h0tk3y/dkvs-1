package ru.ifmo.ctddev.year2012.nets.lab4;

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

    public boolean lessThan(Ballot other) {
        return this.compareTo(other) < 0;
    }


    @Override
    public int compareTo(Ballot other) {
        int result = new Integer(ballotNum).compareTo(other.ballotNum);
        if (result == 0)
            result = new Integer(other.leaderId).compareTo(leaderId);
        // inverted order.
        return result;

    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Ballot)
            return (ballotNum == ((Ballot) other).ballotNum) && (leaderId == ((Ballot) other).leaderId);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
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
