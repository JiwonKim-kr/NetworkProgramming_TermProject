import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameUI {

    private final GameController controller;
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // --- UI Constants ---
    private static final Font DEFAULT_FONT = new Font("맑은 고딕", Font.BOLD, 24);
    private static final Color HIGHLIGHT_VALID_MOVE = Color.YELLOW;
    private static final Color HIGHLIGHT_SELECTED_PIECE = Color.RED;
    private static final Color HIGHLIGHT_CAPTURED_PIECE = Color.GREEN;
    private static final Color HIGHLIGHT_FIRST_TURN = Color.CYAN;

    // Room UI & Colors
    private final Color DEFAULT_BG = UIManager.getColor("Panel.background");

    // --- Lobby UI ---
    private final JButton[][] lobbyButtons = new JButton[4][3];

    // --- Room UI ---
    private JTextArea chatArea;
    private final JButton[][] boardButtons = new JButton[4][3];
    private final Piece[][] boardState = new Piece[4][3];
    private final java.util.Map<Piece, ImageIcon> pieceIcons = new java.util.EnumMap<>(Piece.class);
    private JLabel hostStatusLabel, guestStatusLabel, turnLabel;
    private JPanel p1CapturedPanel, p2CapturedPanel;
    private final List<int[]> validMoveCells = new ArrayList<>();
    private final List<int[]> initialHighlightCells = new ArrayList<>();
    private boolean myTurn = false;

    public GameUI(GameController controller) {
        this.controller = controller;
    }

    public void createAndShow() {
        frame = new JFrame("십이장기");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLobbyPanel(), "LOBBY");
        mainPanel.add(createRoomPanel(), "ROOM");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel lobbyGrid = new JPanel(new GridLayout(4, 3, 8, 8));
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                JButton b = new JButton("빈 방");
                b.setFocusable(false);
                b.setEnabled(false);
                b.addActionListener(e -> {
                    JButton src = (JButton) e.getSource();
                    String title = (String) src.getClientProperty("roomTitle");
                    if (title != null && !title.isBlank()) {
                        controller.joinRoom(title);
                    }
                });
                lobbyButtons[r][c] = b;
                lobbyGrid.add(b);
            }
        }
        panel.add(new JScrollPane(lobbyGrid), BorderLayout.CENTER);

        JButton createRoomButton = new JButton("방 만들기");
        createRoomButton.addActionListener(e -> {
            String roomTitle = JOptionPane.showInputDialog(frame, "방 제목을 입력하세요:");
            if (roomTitle != null && !roomTitle.trim().isEmpty()) {
                controller.createRoom(roomTitle);
            }
        });
        panel.add(createRoomButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 상단 패널 (버튼, 상태)
        JPanel topPanel = createTopPanel();
        panel.add(topPanel, BorderLayout.NORTH);

        // 하단 패널 (내가 잡은 말)
        p1CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1CapturedPanel.setBorder(BorderFactory.createTitledBorder("내가 잡은 말"));
        p1CapturedPanel.setPreferredSize(new Dimension(0, 60));
        panel.add(p1CapturedPanel, BorderLayout.SOUTH);

        // 중앙 게임 보드
        JPanel boardPanel = createBoardPanel();
        panel.add(boardPanel, BorderLayout.CENTER);

        // 우측 채팅 패널
        JPanel chatPanel = createChatPanel();
        panel.add(chatPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // 버튼 영역
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton leaveButton = new JButton("나가기");
        leaveButton.addActionListener(e -> controller.leaveRoom());
        JButton readyButton = new JButton("준비");
        readyButton.addActionListener(e -> controller.sendReady());
        JButton undoButton = new JButton("수 무르기");
        undoButton.addActionListener(e -> controller.requestUndo());
        buttonPanel.add(leaveButton);
        buttonPanel.add(readyButton);
        buttonPanel.add(undoButton);
        topPanel.add(buttonPanel, BorderLayout.WEST);

        // 상태 표시 영역 (master의 Protocol 상수 사용)
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        hostStatusLabel = new JLabel(Protocol.HOST + ": NOT READY", SwingConstants.CENTER);
        guestStatusLabel = new JLabel(Protocol.GUEST + ": NOT READY", SwingConstants.CENTER);
        statusPanel.add(hostStatusLabel);
        statusPanel.add(guestStatusLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // 턴 표시 레이블
        turnLabel = new JLabel("대기 중...", SwingConstants.CENTER);
        turnLabel.setOpaque(true);                 // ← 배경색 먹게
        turnLabel.setBackground(DEFAULT_BG);       // ← 초기 배경
        topPanel.add(turnLabel, BorderLayout.SOUTH);

        // 상대가 잡은 말
        p2CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2CapturedPanel.setBorder(BorderFactory.createTitledBorder("상대방이 잡은 말"));
        p2CapturedPanel.setPreferredSize(new Dimension(0, 60));

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(p2CapturedPanel, BorderLayout.SOUTH);

        return northContainer;
    }

    private JPanel createBoardPanel() {
        JPanel boardPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c] = new JButton();
                boardButtons[r][c].setFont(DEFAULT_FONT);
                boardButtons[r][c].setFocusable(false);
                final int vr = r;
                final int vc = c;
                // DHBranch의 회전 로직 적용
                boardButtons[r][c].addActionListener(e -> {
                    int[] rc = Rotate180(vr, vc);
                    controller.onBoardClicked(rc[0], rc[1]);
                });
                boardPanel.add(boardButtons[r][c]);
            }
        }
        return boardPanel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, 0));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JTextField chatField = new JTextField();
        chatField.addActionListener(e -> {
            String msg = chatField.getText();
            if (!msg.isEmpty()) {
                controller.sendChatMessage(msg);
                chatField.setText("");
            }
        });
        chatPanel.add(chatField, BorderLayout.SOUTH);
        return chatPanel;
    }

    // --- Helper Methods (DHBranch의 이미지/회전 로직 유지) ---
    private int[] Rotate180(int r, int c) {
        return iAmP1() ? new int[]{r, c} : new int[]{3 - r, 2 - c};
    }

    private ImageIcon toFit(ImageIcon src, JButton btn, boolean rotate180) {
        if (src == null) return null;
        Image img = src.getImage();
        if (rotate180) {
            int w = img.getWidth(null), h = img.getHeight(null);
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.rotate(Math.PI, w / 2.0, h / 2.0);
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = out;
        }
        // 버튼 현재 크기에 맞춰 스케일 (약간 여백)
        int w = Math.max(1, btn.getWidth() - 8);
        int h = Math.max(1, btn.getHeight() - 8);
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public void showNicknamePrompt() {
        controller.onNicknameEntered(JOptionPane.showInputDialog(frame, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE));
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void showLobby() {
        cardLayout.show(mainPanel, "LOBBY");
        chatArea.setText("");
    }

    public void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        frame.setTitle("십이장기 - " + roomTitle);
        resetRoomUI();
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void appendChatMessage(String message) {
        chatArea.append(message + "\n");
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public String getPieceOwnerRole(int r, int c) {
        Piece piece = boardState[r][c];
        if (piece == null) return null;
        return piece.getOwner().name();
    }

    public boolean isValidMove(int r, int c) {
        return validMoveCells.stream().anyMatch(m -> m[0] == r && m[1] == c);
    }

    private boolean iAmP1() {
        return controller.isMyTurn("P1");
    }

    // master 브랜치의 Protocol 사용 및 개선된 로직 적용
    public void updatePlayerStatus(String[] readyInfo) {
        String playerRole = readyInfo[0];
        boolean isReady = Boolean.parseBoolean(readyInfo[1]);
        JLabel targetLabel = playerRole.equals(Protocol.HOST) ? hostStatusLabel : guestStatusLabel;
        targetLabel.setText(playerRole + ": " + (isReady ? "READY" : "NOT READY"));
    }

    public void handleGameStart() {
        appendChatMessage(Protocol.SYSTEM + ": 게임이 시작되었습니다!\n");
        hostStatusLabel.setText(Protocol.HOST + ": PLAYING");
        guestStatusLabel.setText(Protocol.GUEST + ": PLAYING");
    }

    public void updateRoomList(String payload) {
        SwingUtilities.invokeLater(() -> {
            String[] items = (payload == null || payload.isBlank()) ? new String[0] : payload.split("\\s*,\\s*");
            int n = Math.min(items.length, 12);
            for (int i = 0; i < 12; i++) {
                JButton b = lobbyButtons[i / 3][i % 3];
                if (i < n) {
                    String item = items[i];
                    String title = item.replaceFirst("\\s*\\(.*$", "").trim();
                    b.setText(item);
                    b.setEnabled(true);
                    b.setToolTipText("입장: " + title);
                    b.putClientProperty("roomTitle", title);
                } else {
                    b.setText("빈 방");
                    b.setEnabled(false);
                    b.setToolTipText(null);
                    b.putClientProperty("roomTitle", null);
                }
            }
        });
    }

    private void applyTurnBackground() {
        Color bg = myTurn ? Color.RED : DEFAULT_BG;
        frame.getContentPane().setBackground(bg);
        turnLabel.setBackground(bg);
    }

    public void updateGameState(String payload) {
        String[] stateParts = payload.split("\\|", 4);
        updateBoard(stateParts[0]);
        if (stateParts.length > 2) updateCapturedPieces(stateParts[1], stateParts[2]);
        if (stateParts.length > 3) {
            String currentPlayerRole = stateParts[3];
            this.myTurn = controller.isMyTurn(currentPlayerRole);
            turnLabel.setText(myTurn ? "당신의 턴입니다." : "상대방의 턴입니다.");
            applyTurnBackground();
        }
    }

    // DHBranch의 회전 로직 + master의 상수 사용 혼합
    public void highlightValidMoves(String payload) {
        clearHighlights(false);
        if (payload == null || payload.isBlank()) return;
        for (String move : payload.split(";")) {
            move = move.trim();
            if (move.isEmpty()) continue;
            String[] coords = move.split(",");
            int r = Integer.parseInt(coords[0].trim()); // 모델 좌표
            int c = Integer.parseInt(coords[1].trim());
            int[] v = Rotate180(r, c);                  // ★ 모델→뷰 변환 (DH 기능)
            boardButtons[v[0]][v[1]].setBackground(HIGHLIGHT_VALID_MOVE); // 상수 사용
            validMoveCells.add(new int[]{r, c});
        }
    }

    public void handleGameOver(String payload) {
        this.myTurn = false;
        JOptionPane.showMessageDialog(frame, payload, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
        resetRoomUI();
    }

    public void showUndoRequest(String payload) {
        int response = JOptionPane.showConfirmDialog(frame, payload + "님이 수 무르기를 요청했습니다. 수락하시겠습니까?", "수 무르기 요청", JOptionPane.YES_NO_OPTION);
        controller.respondUndo(response == JOptionPane.YES_OPTION);
    }

    // DHBranch의 회전 로직 적용
    public void highlightSelectedBoardPiece(int r, int c) {
        int[] v = Rotate180(r, c);
        boardButtons[v[0]][v[1]].setBorder(new LineBorder(HIGHLIGHT_SELECTED_PIECE, 2));
    }

    public void highlightSelectedCapturedPiece(Object sourceButton) {
        ((JButton) sourceButton).setBorder(new LineBorder(HIGHLIGHT_CAPTURED_PIECE, 2));
    }

    // master에서 추가된 기능 (필요 시 DH의 회전 로직을 추가로 적용할 수 있음)
    public void highlightPlayerPieces(String playerRole) {
        clearHighlights(true);
        Piece.Player player = Piece.Player.valueOf(playerRole);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (boardState[r][c] != null && boardState[r][c].getOwner() == player) {
                    // 필요하다면 여기서 Rotate180 적용 가능: int[] v = Rotate180(r, c);
                    boardButtons[r][c].setBackground(HIGHLIGHT_FIRST_TURN);
                    initialHighlightCells.add(new int[]{r, c});
                }
            }
        }
    }

    public void clearHighlights(boolean clearSelection) {
        if (clearSelection) {
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 3; c++)
                    boardButtons[r][c].setBorder(UIManager.getBorder("Button.border"));

            for (Component comp : p1CapturedPanel.getComponents())
                if (comp instanceof JButton)
                    ((JButton) comp).setBorder(UIManager.getBorder("Button.border"));
        }

        // ★ 모델→뷰 변환해서 배경 원복
        for (int[] cell : validMoveCells) {
            int[] v = Rotate180(cell[0], cell[1]);
            boardButtons[v[0]][v[1]].setBackground(UIManager.getColor("Button.background"));
        }
        validMoveCells.clear();

        for (int[] cell : initialHighlightCells)
            boardButtons[cell[0]][cell[1]].setBackground(UIManager.getColor("Button.background"));
        initialHighlightCells.clear();
    }

    // DHBranch의 이미지 로드 및 보드 업데이트 로직 사용
    private ImageIcon iconFor(Piece p) {
        return pieceIcons.computeIfAbsent(p, k -> {
            String file = k.name() + ".png";
            java.net.URL u = GameUI.class.getResource("/images/" + file);
            if (u == null) {
                System.err.println("리소스 없음: " + file);
                return null;
            }
            return new ImageIcon(u);
        });
    }

    private void updateBoard(String boardStateStr) {
        SwingUtilities.invokeLater(() -> {
            clearHighlights(true);

            // 1) 전칸 초기화
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 3; c++) {
                    boardButtons[r][c].setText("");      // 혹시 남아있을 수 있는 텍스트 제거
                    boardButtons[r][c].setIcon(null);    // 아이콘 초기화
                    boardButtons[r][c].setBorder(UIManager.getBorder("Button.border"));
                    boardState[r][c] = null;
                }
            }

            // 2) 빈 보드 처리
            if (boardStateStr == null || boardStateStr.isBlank()) return;

            // 3) 말 배치 (텍스트→아이콘)
            for (String pieceInfo : boardStateStr.split(";")) {
                if (pieceInfo.isBlank()) continue;
                String[] info = pieceInfo.split(",");
                Piece piece = Piece.valueOf(info[0].trim());
                int r = Integer.parseInt(info[1].trim());
                int c = Integer.parseInt(info[2].trim());
                boardState[r][c] = piece;
                int[] v = Rotate180(r, c);                         // ★ 모델→뷰 좌표로 배치
                JButton btn = boardButtons[v[0]][v[1]];
                boolean isOpponent = (piece.getOwner() == Piece.Player.P1) != iAmP1(); // ★ 내 말이 아니면 true
                ImageIcon base = iconFor(piece);
                btn.setIcon(toFit(base, btn, isOpponent));      // ★ 회전 + 스케일 후 세팅
            }
        });
    }

    private void updateCapturedPieces(String p1CapturedStr, String p2CapturedStr) {
        p1CapturedPanel.removeAll();
        p2CapturedPanel.removeAll();

        String playerRole = controller.getPlayerRole();
        boolean isP1 = Protocol.P1.equals(playerRole);

        String myCapturedStr = (isP1) ? p1CapturedStr : p2CapturedStr;
        String opponentCapturedStr = (isP1) ? p2CapturedStr : p1CapturedStr;

        populateCapturedPanel(p1CapturedPanel, myCapturedStr, true);
        populateCapturedPanel(p2CapturedPanel, opponentCapturedStr, false);

        p1CapturedPanel.revalidate();
        p1CapturedPanel.repaint();
        p2CapturedPanel.revalidate();
        p2CapturedPanel.repaint();
    }

    // master의 헬퍼 메서드 사용
    private void populateCapturedPanel(JPanel panel, String capturedStr, boolean isMyPanel) {
        if (capturedStr == null || capturedStr.isEmpty()) return;

        Arrays.stream(capturedStr.split(","))
                .map(Piece::valueOf)
                .forEach(piece -> {
                    JButton pieceButton = new JButton(piece.getDisplayName());
                    if (isMyPanel) {
                        pieceButton.addActionListener(e -> controller.onCapturedPieceClicked(piece, e.getSource()));
                    } else {
                        pieceButton.setEnabled(false);
                    }
                    panel.add(pieceButton);
                });
    }

    // DHBranch의 UI 초기화 로직 (배경색 등) + master의 텍스트 초기화 혼합
    public void resetRoomUI() {
        turnLabel.setBackground(DEFAULT_BG);
        hostStatusLabel.setText(Protocol.HOST + ": NOT READY");
        guestStatusLabel.setText(Protocol.GUEST + ": NOT READY");
        turnLabel.setText("대기 중...");
        updateBoard("");
        p1CapturedPanel.removeAll();
        p1CapturedPanel.revalidate();
        p1CapturedPanel.repaint();
        p2CapturedPanel.removeAll();
        p2CapturedPanel.revalidate();
        p2CapturedPanel.repaint();
    }
}
