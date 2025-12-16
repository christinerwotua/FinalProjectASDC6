package org.example;

public class RandomLink {
    private final int from;
    private final int to;
    private final boolean ladder;

    public RandomLink(int from, int to, boolean isLadder) {
        this.from = from;
        this.to = to;
        this.ladder = isLadder;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public boolean isLadder() {
        return ladder;
    }

    @Override
    public String toString() {
        return (ladder ? "Ladder" : "Snake") + ": " + from + " -> " + to;
    }
}
