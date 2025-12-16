import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CellPanel extends JPanel {
    public final int nodeId;
    private final JLabel label;
    private final List<Player> occupants = new ArrayList<>();

    public CellPanel(int nodeId) {
        this.nodeId = nodeId;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        label = new JLabel(String.valueOf(nodeId));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label, BorderLayout.NORTH);

        setBackground(Color.WHITE);
    }

    public void setOccupants(List<Player> playersHere) {
        occupants.clear();
        occupants.addAll(playersHere);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int tokenSize = 12;
        int padding = 4;
        int x = padding;
        int y = getHeight() - tokenSize - padding;

        for (Player p : occupants) {
            g.setColor(p.color);
            g.fillOval(x, y, tokenSize, tokenSize);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, tokenSize, tokenSize);

            x += tokenSize + 4;
            if (x + tokenSize > getWidth()) {
                x = padding;
                y -= (tokenSize + 4);
            }
        }
    }
}
