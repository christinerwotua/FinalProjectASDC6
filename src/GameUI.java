package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

public class GameUI extends JFrame {

    private GameBoard gameBoard;
    private JPanel controlPanel;
    private JButton playButton;
    private JButton rollDiceButton;
    private JLabel diceResultLabel;
    private JPanel dicePanel;
    private JTextArea gameLogArea;
    private JTextArea scoreboardArea;
    private JLabel currentPlayerLabel;
    private JPanel playersInfoPanel;

    private List<Player> players;
    private Queue<Player> playerQueue;
    private Player currentPlayer;

    private boolean gameStarted = false;
    private boolean isAnimating = false;

    private Random random;
    private int[][] adjacencyMatrix;
    private List<RandomLink> randomLinks;
    private DefaultListModel<String> linksModel;

    private JLabel statusLabel;

    public GameUI() {
        random = new Random();
        players = new ArrayList<>();
        playerQueue = new LinkedList<>();
        randomLinks = new ArrayList<>();
        linksModel = new DefaultListModel<>();

        generateRandomLinks();
        initializeUI();
    }

    // =========================================================
    //  GRAPH / SHORTEST PATH
    // =========================================================

    private void initializeAdjacencyMatrix() {
        adjacencyMatrix = new int[65][65];
        for (int i = 0; i <= 64; i++) {
            Arrays.fill(adjacencyMatrix[i], 0);
        }

        for (int i = 1; i < 64; i++) {
            adjacencyMatrix[i][i + 1] = 1;
            adjacencyMatrix[i + 1][i] = 1;
        }

        int[] primes = {
                2, 3, 5, 7, 11, 13, 17, 19,
                23, 29, 31, 37, 41, 43, 47, 53, 59, 61
        };

        for (int prime : primes) {
            for (int step = 2; step <= 6; step++) {
                if (prime + step <= 64) adjacencyMatrix[prime][prime + step] = step;
                if (prime - step >= 1) adjacencyMatrix[prime][prime - step] = step;
            }
        }
    }

    private void generateRandomLinks() {
        initializeAdjacencyMatrix();
        randomLinks.clear();
        if (linksModel != null) linksModel.clear();

        Set<String> usedPairs = new HashSet<>();

        while (randomLinks.size() < 5) {
            int node1 = random.nextInt(54) + 6;  // 6..59
            int node2 = random.nextInt(54) + 6;  // 6..59
            int diff = Math.abs(node1 - node2);

            if (node1 != node2 && diff > 3 && diff < 20) {
                String p1 = node1 + "-" + node2;
                String p2 = node2 + "-" + node1;

                if (!usedPairs.contains(p1) && !usedPairs.contains(p2)) {
                    boolean isLadder = node2 > node1;
                    int from = isLadder ? node1 : node2;
                    int to = isLadder ? node2 : node1;

                    RandomLink link = new RandomLink(from, to, isLadder);
                    randomLinks.add(link);

                    adjacencyMatrix[from][to] = 1;

                    usedPairs.add(p1);
                    usedPairs.add(p2);

                    linksModel.addElement((isLadder ? "Ladder" : "Snake") + ": " + from + " → " + to);
                }
            }
        }
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private List<Integer> findShortestPath(int start, int end) {
        int n = 65;
        int[] dist = new int[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];

        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[start] = 0;

        for (int i = 0; i < n; i++) {
            int minNode = -1;
            int minDist = Integer.MAX_VALUE;

            for (int j = 1; j < n; j++) {
                if (!visited[j] && dist[j] < minDist) {
                    minDist = dist[j];
                    minNode = j;
                }
            }

            if (minNode == -1) break;
            visited[minNode] = true;

            for (int j = 1; j < n; j++) {
                if (adjacencyMatrix[minNode][j] > 0 && !visited[j]) {
                    int newDist = dist[minNode] + adjacencyMatrix[minNode][j];
                    if (newDist < dist[j]) {
                        dist[j] = newDist;
                        prev[j] = minNode;
                    }
                }
            }
        }

        List<Integer> path = new ArrayList<>();
        if (dist[end] != Integer.MAX_VALUE) {
            int cur = end;
            while (cur != -1) {
                path.add(0, cur);
                cur = prev[cur];
            }
        }
        return path;
    }

    // =========================================================
    //  UI
    // =========================================================

    private void initializeUI() {
        setTitle("Snakes & Ladders - Prime Path Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(227, 242, 253));
        setLayout(new BorderLayout(8, 8));

        setJMenuBar(createMenuBar());
        add(createTopPanel(), BorderLayout.NORTH);

        gameBoard = new GameBoard();
        gameBoard.setRandomLinks(randomLinks);

        JScrollPane boardScroll = new JScrollPane(gameBoard);
        boardScroll.setBorder(new LineBorder(new Color(187, 222, 251)));
        boardScroll.getViewport().setBackground(new Color(227, 242, 253));

        JTabbedPane rightTabs = createRightTabs();

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                boardScroll,
                rightTabs
        );
        split.setResizeWeight(0.72);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1200, 750));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 16, 10, 16));
        panel.setBackground(new Color(25, 118, 210));

        JLabel title = new JLabel("Snakes & Ladders - Prime Path Edition");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(227, 242, 253));

        JLabel subtitle = new JLabel("64 squares • Random snakes & ladders • Prime nodes use shortest path to 64");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(225, 245, 254));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JTabbedPane createRightTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        controlPanel = createControlPanel();
        tabs.addTab("Controls", controlPanel);

        JPanel infoTab = createInfoTab();
        tabs.addTab("Game Info", infoTab);

        return tabs;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(360, 600));
        panel.setBackground(new Color(227, 242, 253));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonsPanel.setOpaque(false);

        playButton = createModernButton("START", new Color(33, 150, 243), new Color(25, 118, 210));
        playButton.addActionListener(e -> startGame());

        rollDiceButton = createModernButton("ROLL DICE", new Color(100, 181, 246), new Color(30, 136, 229));
        rollDiceButton.setEnabled(false);
        rollDiceButton.addActionListener(e -> rollDice());

        buttonsPanel.add(playButton);
        buttonsPanel.add(rollDiceButton);

        JPanel middlePanel = new JPanel();
        middlePanel.setOpaque(false);
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));

        JPanel currentPanel = new JPanel(new BorderLayout());
        currentPanel.setOpaque(false);
        currentPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(144, 202, 249)),
                "Current Player"
        ));

        currentPlayerLabel = new JLabel("Waiting...");
        currentPlayerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        currentPlayerLabel.setForeground(new Color(21, 101, 192));
        currentPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentPanel.add(currentPlayerLabel, BorderLayout.CENTER);

        dicePanel = new JPanel(new BorderLayout());
        dicePanel.setBackground(new Color(215, 227, 252));
        dicePanel.setBorder(new LineBorder(new Color(25, 118, 210), 2, true));
        dicePanel.setPreferredSize(new Dimension(160, 120));

        JLabel diceTitle = new JLabel("Dice", SwingConstants.CENTER);
        diceTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        diceTitle.setForeground(new Color(25, 118, 210));

        diceResultLabel = new JLabel("-", SwingConstants.CENTER);
        diceResultLabel.setFont(new Font("Consolas", Font.BOLD, 48));
        diceResultLabel.setForeground(new Color(120, 144, 156));

        dicePanel.add(diceTitle, BorderLayout.NORTH);
        dicePanel.add(diceResultLabel, BorderLayout.CENTER);

        JPanel diceWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        diceWrapper.setOpaque(false);
        diceWrapper.add(dicePanel);

        middlePanel.add(currentPanel);
        middlePanel.add(Box.createVerticalStrut(8));
        middlePanel.add(diceWrapper);

        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));
        bottomPanel.setOpaque(false);

        playersInfoPanel = new JPanel();
        playersInfoPanel.setBackground(new Color(225, 245, 254));
        playersInfoPanel.setLayout(new BoxLayout(playersInfoPanel, BoxLayout.Y_AXIS));

        JScrollPane playersScroll = new JScrollPane(playersInfoPanel);
        playersScroll.setPreferredSize(new Dimension(320, 130));
        playersScroll.getViewport().setBackground(new Color(225, 245, 254));
        playersScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(187, 222, 251)), "Players"));

        bottomPanel.add(playersScroll, BorderLayout.CENTER);

        scoreboardArea = new JTextArea();
        scoreboardArea.setEditable(false);
        scoreboardArea.setFont(new Font("Segoe UI", Font.BOLD, 13));
        scoreboardArea.setForeground(new Color(25, 118, 210));
        scoreboardArea.setBackground(new Color(227, 242, 253));

        JScrollPane scoreboardScroll = new JScrollPane(scoreboardArea);
        scoreboardScroll.setPreferredSize(new Dimension(320, 80));
        scoreboardScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(187, 222, 251)), "Live Scoreboard"));

        bottomPanel.add(scoreboardScroll, BorderLayout.SOUTH);

        panel.add(buttonsPanel, BorderLayout.NORTH);
        panel.add(middlePanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(227, 242, 253));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        gameLogArea = new JTextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setLineWrap(true);
        gameLogArea.setWrapStyleWord(true);
        gameLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        gameLogArea.setBackground(new Color(232, 244, 253));

        JScrollPane logScroll = new JScrollPane(gameLogArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(187, 222, 251)), "Game Log"));
        panel.add(logScroll, BorderLayout.CENTER);

        JList<String> list = new JList<>(linksModel);
        list.setFont(new Font("Consolas", Font.PLAIN, 12));
        list.setBackground(new Color(232, 244, 253));

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(187, 222, 251)), "Snakes & Ladders"));
        listScroll.setPreferredSize(new Dimension(0, 160));
        panel.add(listScroll, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createModernButton(String text, Color color1, Color color2) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isEnabled()) {
                    GradientPaint gradient = new GradientPaint(0, 0, color1, 0, getHeight(), color2);
                    g2d.setPaint(gradient);
                } else {
                    g2d.setColor(new Color(189, 189, 189));
                }

                g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 45));
        return button;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGameItem = new JMenuItem("New Board / Reset");
        newGameItem.addActionListener(e -> resetBoard());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e2 -> System.exit(0));

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        bar.add(gameMenu);
        return bar;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(4, 8, 4, 8));
        status.setBackground(new Color(227, 242, 253));

        statusLabel = new JLabel("Ready – click START to begin");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(25, 118, 210));

        status.add(statusLabel, BorderLayout.WEST);
        return status;
    }

    // =========================================================
    // GAME LOGIC
    // =========================================================

    private void resetBoard() {
        if (isAnimating) return;

        generateRandomLinks();
        gameBoard.setRandomLinks(randomLinks);
        gameBoard.setHighlightPath(null);
        gameBoard.setTeleportEffect(null);
        gameBoard.repaint();

        for (Player p : players) p.setPosition(1);

        updatePlayersInfoPanel();
        updateScoreboard();

        if (gameLogArea != null) gameLogArea.setText("");

        gameStarted = false;
        playButton.setEnabled(true);
        rollDiceButton.setEnabled(false);
        currentPlayerLabel.setText("Waiting...");
        statusLabel.setText("Board reset – click START to begin new game");

        diceResultLabel.setText("-");
        diceResultLabel.setForeground(new Color(120, 144, 156));
        dicePanel.setBackground(new Color(215, 227, 252));
    }

    private void updatePlayersInfoPanel() {
        playersInfoPanel.removeAll();

        for (Player player : players) {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setMaximumSize(new Dimension(290, 40));
            card.setBackground(new Color(225, 245, 254));
            card.setBorder(new EmptyBorder(5, 10, 5, 10));

            JPanel colorBox = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(player.getColor());
                    g2d.fillOval(5, 5, 20, 20);
                    g2d.setColor(player.getColor().darker());
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(5, 5, 20, 20);
                }
            };
            colorBox.setPreferredSize(new Dimension(30, 30));
            colorBox.setOpaque(false);

            JLabel info = new JLabel(player.getName() + " - Node " + player.getPosition());
            info.setFont(new Font("Segoe UI", Font.BOLD, 13));

            card.add(colorBox, BorderLayout.WEST);
            card.add(info, BorderLayout.CENTER);

            playersInfoPanel.add(card);
            playersInfoPanel.add(Box.createVerticalStrut(5));
        }

        playersInfoPanel.revalidate();
        playersInfoPanel.repaint();
    }

    private void updateScoreboard() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            sb.append(p.getName()).append(" : Node ").append(p.getPosition()).append("\n");
        }
        scoreboardArea.setText(sb.toString());
    }

    private void startGame() {
        String numPlayersStr = JOptionPane.showInputDialog(this, "How many players? (2-6)", "Number of Players", JOptionPane.QUESTION_MESSAGE);
        if (numPlayersStr == null) return;

        int numPlayers;
        try {
            numPlayers = Integer.parseInt(numPlayersStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (numPlayers < 2 || numPlayers > 6) {
            JOptionPane.showMessageDialog(this, "Please enter a number between 2 and 6!", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        generateRandomLinks();
        gameBoard.setRandomLinks(randomLinks);
        gameBoard.setHighlightPath(null);
        gameBoard.setTeleportEffect(null);
        gameBoard.repaint();

        players.clear();

        Color[] playerColors = {
                new Color(244, 67, 54),
                new Color(30, 136, 229),
                new Color(67, 160, 71),
                new Color(142, 36, 170),
                new Color(251, 192, 45),
                new Color(255, 112, 67)
        };

        for (int i = 0; i < numPlayers; i++) {
            String playerName = JOptionPane.showInputDialog(this, "Enter name for Player " + (i + 1) + ":", "Player Name", JOptionPane.QUESTION_MESSAGE);
            if (playerName == null || playerName.trim().isEmpty()) playerName = "Player " + (i + 1);
            players.add(new Player(playerName.trim(), playerColors[i]));
        }

        playerQueue.clear();
        playerQueue.addAll(players);

        gameStarted = true;
        playButton.setEnabled(false);
        rollDiceButton.setEnabled(true);

        currentPlayer = playerQueue.poll();
        currentPlayerLabel.setText(currentPlayer.getName());

        gameBoard.setPlayers(players);
        gameBoard.repaint();

        updatePlayersInfoPanel();
        updateScoreboard();

        gameLogArea.setText("");
        addLog("=== GAME STARTED ===");
        addLog("First turn: " + currentPlayer.getName());
        statusLabel.setText("Game started – " + currentPlayer.getName() + "'s turn");

        diceResultLabel.setText("-");
        diceResultLabel.setForeground(new Color(120, 144, 156));
        dicePanel.setBackground(new Color(215, 227, 252));
    }

    private void rollDice() {
        if (!gameStarted || currentPlayer == null || isAnimating) return;

        isAnimating = true;
        rollDiceButton.setEnabled(false);

        int diceValue = random.nextInt(6) + 1;
        diceResultLabel.setText(String.valueOf(diceValue));

        boolean usePrimePath = isPrime(currentPlayer.getPosition());
        if (usePrimePath) {
            List<Integer> path = findShortestPath(currentPlayer.getPosition(), 64);
            gameBoard.setHighlightPath(path);
            gameBoard.repaint();
            addLog("Prime node! Shortest path highlighted: " + path);
        } else {
            gameBoard.setHighlightPath(null);
        }

        int oldPos = currentPlayer.getPosition();
        int newPos = Math.min(64, oldPos + diceValue);

        // check snake/ladder (simple: apply if landing on from)
        currentPlayer.setPosition(newPos);
        RandomLink hit = null;
        for (RandomLink link : randomLinks) {
            if (link.getFrom() == newPos) {
                hit = link;
                break;
            }
        }

        if (hit != null) {
            gameBoard.setTeleportEffect(hit);
            addLog("Hit " + (hit.isLadder() ? "Ladder" : "Snake") + ": " + hit.getFrom() + " -> " + hit.getTo());
            currentPlayer.setPosition(hit.getTo());
        } else {
            gameBoard.setTeleportEffect(null);
        }

        gameBoard.repaint();
        updatePlayersInfoPanel();
        updateScoreboard();

        if (currentPlayer.getPosition() == 64) {
            addLog(">>> " + currentPlayer.getName() + " wins! <<<");
            statusLabel.setText(currentPlayer.getName() + " wins!");
            JOptionPane.showMessageDialog(this, "CONGRATS! " + currentPlayer.getName() + " wins!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            gameStarted = false;
            playButton.setEnabled(true);
            rollDiceButton.setEnabled(false);
            isAnimating = false;
            return;
        }

        // next turn
        playerQueue.add(currentPlayer);
        currentPlayer = playerQueue.poll();
        currentPlayerLabel.setText(currentPlayer.getName());
        statusLabel.setText("Next: " + currentPlayer.getName());

        isAnimating = false;
        rollDiceButton.setEnabled(true);
    }

    private void addLog(String msg) {
        gameLogArea.append(msg + "\n");
        gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new org.example.GameUI().setVisible(true));
    }
}
