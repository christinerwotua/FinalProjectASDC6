import java.awt.Color;
import java.util.Stack;

public class Player {
    public final String name;
    public final Color color;

    public int position;               // 1..64
    public final Stack<Integer> steps; // history positions

    public Player(String name, Color color) {
        this.name = name;
        this.color = color;
        this.position = 1;
        this.steps = new Stack<>();
        this.steps.push(1);
    }

    public void pushStep(int pos) {
        steps.push(pos);
        position = pos;
    }

    public void popStepIfPossible() {
        if (steps.size() > 1) {
            steps.pop();
            position = steps.peek();
        }
    }
}
