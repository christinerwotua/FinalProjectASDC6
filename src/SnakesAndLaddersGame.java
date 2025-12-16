package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

public class SnakesAndLaddersGame extends JFrame {

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
    private Map<Integer, List<Integer>> shortestPaths;
    private List<RandomLink> randomLinks;
    private DefaultListModel<String> linksModel;

    private JLabel statusLabel;

    public SnakesAndLaddersGame() {
        random = new Random();
        players = new ArrayList<>();
        playerQueue = new LinkedList<>();
        shortestPaths = new HashMap<>();
        randomLinks = new ArrayList<>();
        linksModel = new DefaultListModel<>();

        // inisialisasi graph + ular/tangga random
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

        // koneksi sekuensial 1-64 (bolak-balik)
        for (int i = 1; i < 64; i++) {
            adjacencyMatrix[i][i + 1] = 1;
            adjacencyMatrix[i + 1][i] = 1;
        }

        // edge ekstra untuk bilangan prima
        int[] primes = {
                2, 3, 5, 7, 11, 13, 17, 19,
                23, 29, 31, 37, 41, 43, 47, 53, 59, 61
        };

        for (int prime : primes) {
            for (int step = 2; step <= 6; step++) {
                if (prime + step <= 64) {
                    adjacencyMatrix[prime][prime + step] = step;
                }
                if (prime - step >= 1) {
                    adjacencyMatrix[prime][prime - step] = step;
                }
            }
        }
    }

    private void generateRandomLinks() {
        // reset graph dasar (sekuensial + prima)
        initializeAdjacencyMatrix();
        randomLinks.clear();
        if (linksModel != null) {
            linksModel.clear();
        }

        Set<String> usedPairs = new HashSet<>();

        while (randomLinks.size() < 5) {
            int node1 = random.nextInt(54) + 6;  // 6..59
            int node2 = random.nextInt(54) + 6;  // 6..59
            int diff = Math.abs(node1 - node2);

            // jarak dibatasi supaya tidak terlalu jauh
            if (node1 != node2 && diff > 3 && diff < 20) {
                String p1 = node1 + "-" + node2;
                String p2 = node2 + "-" + node1;

                if (!usedPairs.contains(p1) && !usedPairs.contains(p2)) {
                    boolean isLadder = node2 > node1;
                    int from = isLadder ? node1 : node2;
                    int to = isLadder ? node2 : node1;

                    RandomLink link = new RandomLink(from, to, isLadder);
                    randomLinks.add(link);

                    // edge ini ikut dipakai pada shortest path
                    adjacencyMatrix[from][to] = 1;

                    usedPairs.add(p1);
                    usedPairs.add(p2);

                    if (linksModel != null) {
                        linksModel.addElement((isLadder ? "Ladder" : "Snake")
                                + ": " + from + " → " + to);
                    }
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

        JLabel subtitle = new JLabel(
                "64 squares • Random snakes & ladders • Prime nodes use shortest path to 64"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(225, 245, 254));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setOpaque(false);
        legend.add(createLegendLabel("Prime Node", new Color(255, 193, 7)));
        legend.add(createLegendLabel("Ladder", new Color(56, 142, 60)));
        legend.add(createLegendLabel("Snake", new Color(211, 47, 47)));
        legend.add(createLegendLabel("Shortest Path", new Color(255, 235, 59)));

        panel.add(legend, BorderLayout.EAST);

        return panel;
    }

    private JLabel createLegendLabel(String text, Color dotColor) {
        String hex = String.format("#%02x%02x%02x",
                dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue());
        JLabel label = new JLabel(
                "<html><span style='color:" + hex + "'>●</span> " + text + "</html>"
        );
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(new Color(227, 242, 253));
        return label;
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

        // tombol START / ROLL DICE
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonsPanel.setOpaque(false);

        playButton = createModernButton(
                "START",
                new Color(33, 150, 243),
                new Color(25, 118, 210)
        );
        playButton.addActionListener(e -> startGame());

        rollDiceButton = createModernButton(
                "ROLL DICE",
                new Color(100, 181, 246),
                new Color(30, 136, 229)
        );
        rollDiceButton.setEnabled(false);
        rollDiceButton.addActionListener(e -> rollDice());

        buttonsPanel.add(playButton);
        buttonsPanel.add(rollDiceButton);

        // current player + dice
        JPanel middlePanel = new JPanel();
        middlePanel.setOpaque(false);
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));

        JPanel currentPanel = new JPanel(new BorderLayout());
        currentPanel.setOpaque(false);
        currentPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(144, 202, 249)),
                "Current Player",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(21, 101, 192)
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
        dicePanel.setMaximumSize(new Dimension(200, 140));

        JLabel diceTitle = new JLabel("Dice");
        diceTitle.setHorizontalAlignment(SwingConstants.CENTER);
        diceTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        diceTitle.setForeground(new Color(25, 118, 210));
        diceTitle.setBorder(new EmptyBorder(6, 0, 0, 0));

        diceResultLabel = new JLabel("-");
        diceResultLabel.setHorizontalAlignment(SwingConstants.CENTER);
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

        // players + live scoreboard
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 6));
        bottomPanel.setOpaque(false);

        playersInfoPanel = new JPanel();
        playersInfoPanel.setBackground(new Color(225, 245, 254));
        playersInfoPanel.setLayout(new BoxLayout(playersInfoPanel, BoxLayout.Y_AXIS));

        JScrollPane playersScroll = new JScrollPane(playersInfoPanel);
        playersScroll.setPreferredSize(new Dimension(320, 130));
        playersScroll.getViewport().setBackground(new Color(225, 245, 254));
        playersScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(187, 222, 251)),
                "Players",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(21, 101, 192)
        ));

        bottomPanel.add(playersScroll, BorderLayout.CENTER);

        scoreboardArea = new JTextArea();
        scoreboardArea.setEditable(false);
        scoreboardArea.setFont(new Font("Segoe UI", Font.BOLD, 13));
        scoreboardArea.setForeground(new Color(25, 118, 210));
        scoreboardArea.setBackground(new Color(227, 242, 253));

        JScrollPane scoreboardScroll = new JScrollPane(scoreboardArea);
        scoreboardScroll.setPreferredSize(new Dimension(320, 80));
        scoreboardScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(187, 222, 251)),
                "Live Scoreboard",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(25, 118, 210)
        ));

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

        JTextArea info = new JTextArea();
        info.setEditable(false);
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setBackground(new Color(227, 242, 253));
        info.setForeground(new Color(33, 33, 33));
        info.setText(
                "Game Rules (Prime Path Version):\n\n" +
                        "• Semua pemain mulai dari START (1) dan berlomba ke FINISH (64).\n" +
                        "• Klik START untuk memilih jumlah pemain dan nama.\n" +
                        "• Saat giliranmu, klik ROLL DICE.\n" +
                        "• Berhenti di bawah tangga = naik.\n" +
                        "• Berhenti di kepala ular = turun.\n" +
                        "• Jika berada di kotak prima saat mulai giliran,\n" +
                        "  pion akan mengikuti shortest path menuju 64.\n" +
                        "  Jalur ini akan di-highlight pada papan.\n"
        );
        info.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(187, 222, 251)),
                "How to Play",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(25, 118, 210)
        ));
        info.setPreferredSize(new Dimension(0, 170));
        panel.add(info, BorderLayout.NORTH);

        gameLogArea = new JTextArea();
        gameLogArea.setEditable(false);
        gameLogArea.setLineWrap(true);
        gameLogArea.setWrapStyleWord(true);
        gameLogArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        gameLogArea.setBackground(new Color(232, 244, 253));
        gameLogArea.setForeground(new Color(33, 33, 33));

        JScrollPane logScroll = new JScrollPane(gameLogArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(187, 222, 251)),
                "Game Log (History)",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(25, 118, 210)
        ));
        panel.add(logScroll, BorderLayout.CENTER);

        JList<String> list = new JList<>(linksModel);
        list.setFont(new Font("Consolas", Font.PLAIN, 12));
        list.setBackground(new Color(232, 244, 253));
        list.setForeground(new Color(33, 33, 33));

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(new Color(187, 222, 251)),
                "Snakes & Ladders (Current Board)",
                0, 0,
                new Font("Segoe UI", Font.PLAIN, 12),
                new Color(25, 118, 210)
        ));
        listScroll.setPreferredSize(new Dimension(0, 120));
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
                    GradientPaint gradient = new GradientPaint(
                            0, 0, color1,
                            0, getHeight(), color2
                    );
                    g2d.setPaint(gradient);
                } else {
                    g2d.setColor(new Color(189, 189, 189));
                }

                g2d.fill(new RoundRectangle2D.Double(
                        0, 0, getWidth(), getHeight(), 16, 16
                ));

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
        button.setMaximumSize(new Dimension(310, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGameItem = new JMenuItem("New Board / Reset");
        newGameItem.addActionListener(e -> {
            if (isAnimating) return;

            // generate ulang ular & tangga
            generateRandomLinks();
            if (gameBoard != null) {
                gameBoard.setRandomLinks(randomLinks);
                gameBoard.setHighlightPath(null);
                gameBoard.setTeleportEffect(null);
                gameBoard.repaint();
            }

            for (Player p : players) {
                p.setPosition(1);
            }

            updatePlayersInfoPanel();
            updateScoreboard();

            if (gameLogArea != null) {
                gameLogArea.setText("");
            }

            gameStarted = false;
            if (playButton != null) playButton.setEnabled(true);
            if (rollDiceButton != null) rollDiceButton.setEnabled(false);
            if (currentPlayerLabel != null) currentPlayerLabel.setText("Waiting...");
            if (statusLabel != null) {
                statusLabel.setText("Board reset – click START to begin new game");
            }

            if (diceResultLabel != null) {
                diceResultLabel.setText("-");
                diceResultLabel.setForeground(new Color(120, 144, 156));
            }
            if (dicePanel != null) {
                dicePanel.setBackground(new Color(215, 227, 252));
            }
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e2 -> System.exit(0));

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Snakes & Ladders - Prime Path Edition\n" +
                                "Prime nodes use a graph shortest path to 64.\n" +
                                "Built with Java Swing.",
                        "About",
                        JOptionPane.INFORMATION_MESSAGE)
        );
        helpMenu.add(aboutItem);

        bar.add(gameMenu);
        bar.add(helpMenu);

        return bar;
    }

    private JPanel createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(4, 8, 4, 8));
        status.setBackground(new Color(227, 242, 253));

        statusLabel = new JLabel("Ready – click START to begin");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(25, 118, 210));

        JLabel right = new JLabel("Snakes & Ladders • Java Swing");
        right.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        right.setForeground(new Color(120, 144, 156));
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        status.add(statusLabel, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);

        return status;
    }

    // =========================================================
    //  GAME FLOW
    // =========================================================

    private void updatePlayersInfoPanel() {
        if (playersInfoPanel == null) return;

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
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
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
            info.setForeground(new Color(33, 33, 33));

            card.add(colorBox, BorderLayout.WEST);
            card.add(info, BorderLayout.CENTER);

            playersInfoPanel.add(card);
            playersInfoPanel.add(Box.createVerticalStrut(5));
        }

        playersInfoPanel.revalidate();
        playersInfoPanel.repaint();
    }

    private void updateScoreboard() {
        if (scoreboardArea == null) return;

        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            sb.append(p.getName())
                    .append(" : Node ")
                    .append(p.getPosition())
                    .append("\n");
        }
        scoreboardArea.setText(sb.toString());
    }

    private void startGame() {
        String numPlayersStr = JOptionPane.showInputDialog(this,
                "How many players? (2-6)",
                "Number of Players",
                JOptionPane.QUESTION_MESSAGE);

        if (numPlayersStr == null) return;

        try {
            int numPlayers = Integer.parseInt(numPlayersStr);
            if (numPlayers < 2 || numPlayers > 6) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a number between 2 and 6!",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // setiap start game, board di-random ulang
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
                String playerName = JOptionPane.showInputDialog(this,
                        "Enter name for Player " + (i + 1) + ":",
                        "Player Name",
                        JOptionPane.QUESTION_MESSAGE);

                if (playerName == null || playerName.trim().isEmpty()) {
                    playerName = "Player " + (i + 1);
                }

                Player p = new Player(playerName.trim(), playerColors[i]);
                players.add(p);
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

            if (gameLogArea != null) {
                gameLogArea.setText("");
            }
            addLog("=== GAME STARTED ===");
            addLog("Players:");
            for (Player p : players) {
                addLog(" - " + p.getName());
            }
            addLog("");
            addLog("Snakes & Ladders:");
            for (RandomLink link : randomLinks) {
                addLog((link.isLadder() ? "Ladder" : "Snake") + ": "
                        + link.getFrom() + " → " + link.getTo());
            }
            addLog("");
            addLog("First turn: " + currentPlayer.getName());

            if (statusLabel != null) {
                statusLabel.setText("Game started – " + currentPlayer.getName() + "'s turn");
            }

            diceResultLabel.setText("-");
            diceResultLabel.setForeground(new Color(120, 144, 156));
            dicePanel.setBackground(new Color(215, 227, 252));

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid number!",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- SMOOTH DICE & MOVEMENT ----------

    private void rollDice() {
        if (!gameStarted || currentPlayer == null || isAnimating) return;

        isAnimating = true;
        rollDiceButton.setEnabled(false);

        int finalDiceValue = random.nextInt(6) + 1;

        // 70% maju, 30% mundur
        double probability = random.nextDouble();
        boolean isForward = probability < 0.7;
        Color diceColor = isForward ? new Color(33, 150, 243) : new Color(229, 57, 53);

        int startPos = currentPlayer.getPosition();

        if (statusLabel != null) {
            statusLabel.setText("Rolling dice for " + currentPlayer.getName() + "...");
        }

        dicePanel.setBackground(new Color(215, 227, 252));
        diceResultLabel.setForeground(new Color(120, 144, 156));

        final int[] tick = {0};
        final int maxTicks = 14;

        javax.swing.Timer diceTimer = new javax.swing.Timer(70, null);
        diceTimer.addActionListener(e -> {
            tick[0]++;

            int visual = random.nextInt(6) + 1;
            diceResultLabel.setText(String.valueOf(visual));

            if (tick[0] % 2 == 0) {
                dicePanel.setBackground(new Color(225, 235, 255));
            } else {
                dicePanel.setBackground(new Color(215, 227, 252));
            }

            if (tick[0] >= maxTicks) {
                diceTimer.stop();

                diceResultLabel.setText(String.valueOf(finalDiceValue));
                diceResultLabel.setForeground(diceColor.brighter());
                dicePanel.setBackground(diceColor.darker());

                startMovementFromRoll(startPos, finalDiceValue, isForward);
            }
        });
        diceTimer.setInitialDelay(0);
        diceTimer.start();
    }

    private void startMovementFromRoll(int oldPosition, int diceValue, boolean isForward) {
        int targetPosition;
        if (isForward) {
            targetPosition = oldPosition + diceValue;
            if (targetPosition > 64) targetPosition = 64;
        } else {
            targetPosition = oldPosition - diceValue;
            if (targetPosition < 1) targetPosition = 1;
        }

        boolean usePrimePath = isPrime(oldPosition);
        String pathType = usePrimePath ? "SHORTEST PATH (prime node)" : "NORMAL MOVE";

        addLog("--------------------------------");
        addLog("Player: " + currentPlayer.getName());
        addLog("Dice: " + diceValue + (isForward ? " (forward)" : " (backward)"));
        addLog("Move type: " + pathType);

        if (usePrimePath) {
            addLog("Prime node at " + oldPosition + ": using shortest path to 64.");
            addLog("Steps/visits this turn: " + diceValue);
            if (statusLabel != null) {
                statusLabel.setText("Following shortest path from prime node...");
            }
            animateShortestPath(oldPosition, targetPosition, diceValue, isForward);
        } else {
            addLog("Path: " + oldPosition + " → " + targetPosition);
            if (statusLabel != null) {
                statusLabel.setText("Moving " + diceValue + " step(s)...");
            }
            animateSequentialMovement(oldPosition, targetPosition, diceValue, isForward);
        }
    }

    private void animateSequentialMovement(int startPos, int endPos,
                                           int diceValue, boolean isForward) {
        final int[] currentStep = {0};
        final int totalSteps = Math.abs(endPos - startPos);
        final boolean[] linkedUsed = {false};

        javax.swing.Timer animationTimer = new javax.swing.Timer(230, null);
        animationTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentStep[0]++;

                int intermediatePos = isForward
                        ? startPos + currentStep[0]
                        : startPos - currentStep[0];

                if (intermediatePos > 64) intermediatePos = 64;
                if (intermediatePos < 1) intermediatePos = 1;

                currentPlayer.setPosition(intermediatePos);

                gameBoard.setHighlightPath(null);
                gameBoard.repaint();
                updatePlayersInfoPanel();
                updateScoreboard();

                addLog("Step " + currentStep[0] + ": node " + intermediatePos);

                if (!linkedUsed[0]) {
                    for (RandomLink link : randomLinks) {
                        if (link.getFrom() == intermediatePos) {
                            addLog("Hit " + (link.isLadder() ? "Ladder 🪜" : "Snake 🐍") + "!");
                            addLog("Jump: " + link.getFrom() + " → " + link.getTo());

                            animationTimer.stop();
                            linkedUsed[0] = true;

                            javax.swing.Timer teleportTimer = new javax.swing.Timer(650, evt -> {
                                currentPlayer.setPosition(link.getTo());
                                gameBoard.setTeleportEffect(link);
                                gameBoard.repaint();
                                updatePlayersInfoPanel();
                                updateScoreboard();

                                javax.swing.Timer clearTimer = new javax.swing.Timer(650, evt2 -> {
                                    gameBoard.setTeleportEffect(null);
                                    finishTurn(link.getTo());
                                });
                                clearTimer.setRepeats(false);
                                clearTimer.start();
                            });
                            teleportTimer.setRepeats(false);
                            teleportTimer.start();
                            return;
                        }
                    }
                }

                if (currentStep[0] >= totalSteps || intermediatePos == endPos) {
                    animationTimer.stop();
                    finishTurn(endPos);
                }
            }
        });
        animationTimer.start();
    }

    private void animateShortestPath(int startPos, int endPos,
                                     int diceValue, boolean isForward) {
        List<Integer> fullPath = findShortestPath(startPos, 64);

        if (fullPath.isEmpty() || fullPath.size() == 1) {
            animateSequentialMovement(startPos, endPos, diceValue, isForward);
            return;
        }

        addLog("Shortest path: " + fullPath);

        List<Integer> selectedPath = selectNodesForVisit(fullPath, startPos, diceValue);

        addLog("Visited nodes this turn: " + selectedPath);
        addLog("Total visits: " + (selectedPath.size() - 1));

        final int[] currentIndex = {0};
        final boolean[] usedLink = {false};

        javax.swing.Timer animationTimer = new javax.swing.Timer(320, null);
        animationTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentIndex[0]++;

                if (currentIndex[0] < selectedPath.size()) {
                    int prevPos = selectedPath.get(currentIndex[0] - 1);
                    int nextPos = selectedPath.get(currentIndex[0]);

                    RandomLink usedRandomLink = null;
                    for (RandomLink link : randomLinks) {
                        if (link.getFrom() == prevPos && link.getTo() == nextPos) {
                            usedRandomLink = link;
                            break;
                        }
                    }

                    currentPlayer.setPosition(nextPos);

                    gameBoard.setHighlightPath(selectedPath.subList(0, currentIndex[0] + 1));

                    if (usedRandomLink != null) {
                        gameBoard.setTeleportEffect(usedRandomLink);
                    }

                    gameBoard.repaint();
                    updatePlayersInfoPanel();
                    updateScoreboard();

                    String stepLog = "Path step " + currentIndex[0] + ": node " + nextPos;
                    if (usedRandomLink != null) {
                        stepLog += " (via " +
                                (usedRandomLink.isLadder() ? "Ladder" : "Snake") + ")";
                        usedLink[0] = true;
                    }
                    if (isPrime(nextPos)) {
                        stepLog += " (prime)";
                    }
                    addLog(stepLog);

                    if (usedRandomLink != null) {
                        javax.swing.Timer clearTimer = new javax.swing.Timer(280, evt -> {
                            if (currentIndex[0] < selectedPath.size() - 1) {
                                gameBoard.setTeleportEffect(null);
                            }
                        });
                        clearTimer.setRepeats(false);
                        clearTimer.start();
                    }

                } else {
                    animationTimer.stop();
                    gameBoard.setHighlightPath(null);
                    gameBoard.setTeleportEffect(null);

                    int finalPos = selectedPath.get(selectedPath.size() - 1);

                    if (usedLink[0]) {
                        addLog("Shortest path used some ladders/snakes as shortcuts.");
                    }

                    finishTurn(finalPos);
                }
            }
        });
        animationTimer.start();
    }

    private List<Integer> selectNodesForVisit(List<Integer> fullPath,
                                              int startPos, int diceValue) {
        List<Integer> selected = new ArrayList<>();
        selected.add(startPos);

        if (fullPath.size() <= 1) return selected;
        if (fullPath.size() - 1 <= diceValue) {
            return fullPath;
        }

        double stepSize = (double) (fullPath.size() - 1) / diceValue;

        for (int i = 1; i <= diceValue; i++) {
            int index = (int) Math.round(i * stepSize);
            if (index >= fullPath.size()) {
                index = fullPath.size() - 1;
            }
            int node = fullPath.get(index);
            if (!selected.contains(node)) {
                selected.add(node);
            }
        }

        // usahakan memanfaatkan link jika bisa
        for (int i = 0; i < selected.size() - 1; i++) {
            int currentNode = selected.get(i);
            for (RandomLink link : randomLinks) {
                if (link.getFrom() == currentNode) {
                    int linkTarget = link.getTo();
                    int targetIdx = fullPath.indexOf(linkTarget);

                    if (targetIdx > fullPath.indexOf(currentNode)) {
                        if (!selected.contains(linkTarget) && i + 1 < selected.size()) {
                            selected.add(i + 1, linkTarget);
                            if (selected.size() > diceValue + 1) {
                                selected.remove(selected.size() - 2);
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    private void finishTurn(int finalPosition) {
        addLog("Final position this turn: " + finalPosition);

        updatePlayersInfoPanel();
        updateScoreboard();

        if (finalPosition == 64) {
            addLog(">>> " + currentPlayer.getName() + " reached FINISH (64)! <<<");

            if (statusLabel != null) {
                statusLabel.setText(currentPlayer.getName() + " wins the game!");
            }

            javax.swing.Timer timer = new javax.swing.Timer(500, evt -> {
                JOptionPane.showMessageDialog(this,
                        "CONGRATULATIONS! 🎉\n\n" +
                                currentPlayer.getName() + " has reached FINISH (64).",
                        "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
                gameStarted = false;
                playButton.setEnabled(true);
                rollDiceButton.setEnabled(false);
                isAnimating = false;
            });
            timer.setRepeats(false);
            timer.start();

            return;
        }

        playerQueue.add(currentPlayer);
        currentPlayer = playerQueue.poll();
        currentPlayerLabel.setText(currentPlayer.getName());

        addLog("Next turn: " + currentPlayer.getName());
        addLog("");

        if (statusLabel != null) {
            statusLabel.setText("Next: " + currentPlayer.getName() + " – click ROLL DICE");
        }

        isAnimating = false;
        rollDiceButton.setEnabled(true);
    }

    private void addLog(String message) {
        if (gameLogArea == null) return;
        gameLogArea.append(message + "\n");
        gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
    }

    // =========================================================
    //  MAIN
    // =========================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SnakesAndLaddersGame game = new SnakesAndLaddersGame();
            game.setVisible(true);
        });
    }
}