public class DiceResult {
    public final int value;       // 1..6
    public final boolean green;   // true=maju, false=mundur

    public DiceResult(int value, boolean green) {
        this.value = value;
        this.green = green;
    }
}
