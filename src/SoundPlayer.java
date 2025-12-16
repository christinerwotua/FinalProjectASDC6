import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GameUI {

    private final JFrame frame = new JFrame("Graph Board Game (8x8) - Adjacency Matrix");
    private final JButton playBtn = new JButton("Play");
    private final JButton rollBtn = new JButton("Roll Dice");

    private final JTextArea infoArea = new JTextArea(10, 30);
    private final JTextArea graphArea = new JTextArea(10, 30);

    private final JPanel boardPanel = new JPanel(new GridLayout(8, 8, 2, 2));
    private final CellPanel[] cells = new CellPanel[65];

    private final Random rnd = new Random();

    private BoardGraph graph;
    private LinkedList<Player> turnQueue;
    private Player currentPlayer;

    private boolean gameRunning = false;
    private boolean animating = false;

    public void show() {
        buildUI();
        frame.setVisible(true);
    }

    private void buildUI() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        boardPanel.setBorder(BorderFactory.createTitledBorder("Board 8x8 (64 nodes)"));
        for (int i = 1; i <= 64; i++) {
            cells[i] = new CellPanel(i);
            boardPanel.add(cells[i]);
        }

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        rollBtn.setEnabled(false);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRow.add(playBtn);
        btnRow.add(rollBtn);

        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBorder(BorderFactory.createTitledBorder("Game Info"));

        graphArea.setEditable(false);
        graphArea.setLineWrap(true);
        graphArea.setWrapStyleWord(true);
        graphArea.setBorder(BorderFactory.createTitledBorder("Graph / Links"));

        rightPanel.add(btnRow);
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(new JScrollPane(infoArea));
        rightPanel.add(Box.createVerticalStrut(8));
        rightPanel.add(new JScrollPane(graphArea));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, rightPanel);
        split.setDividerLocation(700);
        frame.add(split);

        playBtn.addActionListener(e -> onPlay());
        rollBtn.addActionListener(e -> onRollDice());

        refreshBoardOccupants(Collections.emptyList());
        logInfo("Klik Play untuk memulai.");
    }

    private void onPlay() {
        graph = new BoardGraph();
        renderGraphInfo();

        List<Player> players = promptPlayers();
        if (players.isEmpty()) return;

        turnQueue = new LinkedList<>(players);
        currentPlayer = null;

        gameRunning = true;
        animating = false;

        rollBtn.setEnabled(true);
        playBtn.setEnabled(false);

        refreshBoardOccupants(players);
        nextTurn();
    }

    private List<Player> promptPlayers() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Jumlah pemain:"));
        SpinnerNumberModel model = new SpinnerNumberModel(2, 1, 6, 1);
        JSpinner spinner = new JSpinner(model);
        top.add(spinner);
        panel.add(top, BorderLayout.NORTH);

        JTextArea namesArea = new JTextArea(6, 25);
        namesArea.setBorder(BorderFactory.createTitledBorder("Nama pemain (1 per baris). Jika kurang, auto-generate."));
        namesArea.setText("Player 1\nPlayer 2");
        panel.add(new JScrollPane(namesArea), BorderLayout.CENTER);

        int ok = JOptionPane.showConfirmDialog(frame, panel, "Inisiasi Pemain", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return List.of();

        int count = (Integer) spinner.getValue();
        String[] lines = namesArea.getText().split("\\R+");

        List<String> names = new ArrayList<>();
        for (String s : lines) {
            String t = s.trim();
            if (!t.isEmpty()) names.add(t);
        }
        while (names.size() < count) names.add("Player " + (names.size() + 1));

        Color[] palette = new Color[]{
                new Color(0x1f77b4),
                new Color(0xff7f0e),
                new Color(0x2ca02c),
                new Color(0xd62728),
                new Color(0x9467bd),
                new Color(0x8c564b)
        };

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new Player(names.get(i), palette[i % palette.length]));
        }

        logInfo("Pemain dibuat: " + String.join(", ", names.subList(0, count)));
        return players;
    }

    private void onRollDice() {
        if (!gameRunning || animating) return;
        if (currentPlayer == null) return;

        DiceResult dr = rollDice();
        String colorText = dr.green ? "HIJAU (maju)" : "MERAH (mundur)";
        logInfo("🎲 " + currentPlayer.name + " roll: " + dr.value + " [" + colorText + "]");

        if (dr.green) {
            boolean primeStart = BoardGraph.isPrime(currentPlayer.position);
            if (primeStart) {
                logInfo("✨ Start " + currentPlayer.position + " prima -> shortest path aktif (menuju 64).");
                moveForwardAlongShortestPath(currentPlayer, dr.value);
            } else {
                moveForwardSequential(currentPlayer, dr.value);
            }
        } else {
            moveBackwardWithStack(currentPlayer, dr.value);
        }
    }

    private DiceResult rollDice() {
        int value = 1 + rnd.nextInt(6);
        double r = rnd.nextDouble(); // 0..1
        boolean green = (r <= 0.7);
        return new DiceResult(value, green);
    }

    private void moveForwardSequential(Player p, int steps) {
        List<Integer> path = new ArrayList<>();
        int pos = p.position;

        for (int i = 0; i < steps; i++) {
            if (pos >= 64) break;
            pos += 1;
            path.add(pos);
        }

        if (path.isEmpty()) {
            endTurnAfterMove(p);
            return;
        }

        SoundPlayer.playWavSafe("yeay.wav");
        animatePathPush(p, path, () -> endTurnAfterMove(p));
    }

    private void moveForwardAlongShortestPath(Player p, int diceSteps) {
        List<Integer> sp = graph.shortestPath(p.position, 64);
        if (sp.isEmpty()) {
            moveForwardSequential(p, diceSteps);
            return;
        }

        int moveCount = Math.min(diceSteps, sp.size() - 1);
        List<Integer> path = new ArrayList<>();
        for (int i = 1; i <= moveCount; i++) {
            path.add(sp.get(i));
        }

        if (path.isEmpty()) {
            endTurnAfterMove(p);
            return;
        }

        SoundPlayer.playWavSafe("yeay.wav");
        animatePathPush(p, path, () -> endTurnAfterMove(p));
    }

    private void moveBackwardWithStack(Player p, int steps) {
        List<Integer> path = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            int before = p.position;
            p.popStepIfPossible();
            int after = p.position;
            if (after == before) break;
            path.add(after);
        }

        if (path.isEmpty()) {
            endTurnAfterMove(p);
            return;
        }

        SoundPlayer.playWavSafe("yaaah.wav");
        animatePathRepaintOnly(p, path, () -> endTurnAfterMove(p));
    }

    private void animatePathPush(Player p, List<Integer> path, Runnable onDone) {
        animating = true;
        rollBtn.setEnabled(false);

        final int[] idx = {0};
        Timer timer = new Timer(250, null);
        timer.addActionListener(e -> {
            int pos = path.get(idx[0]);
            p.pushStep(pos);

            refreshBoardOccupants(getAllPlayers());
            if (checkWinnerAndStopIfNeeded(p, timer)) return;

            idx[0]++;
            if (idx[0] >= path.size()) {
                timer.stop();
                animating = false;
                rollBtn.setEnabled(true);
                onDone.run();
            }
        });
        timer.start();
    }

    private void animatePathRepaintOnly(Player p, List<Integer> path, Runnable onDone) {
        animating = true;
        rollBtn.setEnabled(false);

        final int[] idx = {0};
        Timer timer = new Timer(250, null);
        timer.addActionListener(e -> {
            int pos = path.get(idx[0]);
            p.position = pos;

            refreshBoardOccupants(getAllPlayers());
            if (checkWinnerAndStopIfNeeded(p, timer)) return;

            idx[0]++;
            if (idx[0] >= path.size()) {
                timer.stop();
                // sync back with stack peek
                p.position = p.steps.peek();
                animating = false;
                rollBtn.setEnabled(true);
                onDone.run();
            }
        });
        timer.start();
    }

    private boolean checkWinnerAndStopIfNeeded(Player p, Timer timer) {
        if (p.position >= 64) {
            timer.stop();
            animating = false;
            gameRunning = false;

            rollBtn.setEnabled(false);

            logInfo("🏆 WINNER: " + p.name + " mencapai node 64!");
            JOptionPane.showMessageDialog(frame,
                    "WINNER: " + p.name + " 🎉",
                    "Game Selesai",
                    JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    private void endTurnAfterMove(Player p) {
        if (!gameRunning) return;

        boolean doubleTurn = (p.position % 5 == 0);
        if (doubleTurn) {
            logInfo("🔥 " + p.name + " landing kelipatan 5 (" + p.position + ") -> DOUBLE TURN!");
            // push ke depan queue (main lagi)
            turnQueue.addFirst(p);
        } else {
            // normal ke belakang queue
            turnQueue.addLast(p);
        }

        nextTurn();
    }

    private void nextTurn() {
        if (!gameRunning) return;

        // poll from queue
        currentPlayer = turnQueue.poll();
        if (currentPlayer == null) return;

        logInfo("\n➡️ Giliran: " + currentPlayer.name + " (pos: " + currentPlayer.position + ")");
        refreshBoardOccupants(getAllPlayers());
    }

    private List<Player> getAllPlayers() {
        List<Player> all = new ArrayList<>();
        if (currentPlayer != null) all.add(currentPlayer);
        if (turnQueue != null) all.addAll(turnQueue);
        return all;
    }

    private void refreshBoardOccupants(List<Player> allPlayers) {
        Map<Integer, List<Player>> map = new HashMap<>();
        for (int i = 1; i <= 64; i++) map.put(i, new ArrayList<>());

        for (Player p : allPlayers) {
            int pos = Math.max(1, Math.min(64, p.position));
            map.get(pos).add(p);
        }

        for (int i = 1; i <= 64; i++) {
            cells[i].setOccupants(map.get(i));
        }
    }

    private void renderGraphInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Default edges: i <-> i+1 (1..64)\n");
        sb.append("Random Links (5 buah):\n");
        for (int[] link : graph.randomLinks) {
            sb.append(" - ").append(link[0]).append(" <-> ").append(link[1]).append("\n");
        }
        sb.append("\nAdjacency Matrix: graph.adjMatrix (65x65)\n");
        sb.append("Adjacency List: graph.adjList\n");
        graphArea.setText(sb.toString());
    }

    private void logInfo(String msg) {
        infoArea.append(msg + "\n");
        infoArea.setCaretPosition(infoArea.getDocument().getLength());
    }
}
