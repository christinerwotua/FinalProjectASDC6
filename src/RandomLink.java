package org.example;

public class RandomLink {
    private int from;
    private int to;
    private boolean isLadder; // true = ladder, false = snake

    public RandomLink(int from, int to, boolean isLadder) {
        this.from = from;
        this.to = to;
        this.isLadder = isLadder;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public boolean isLadder() {
        return isLadder;
    }
}