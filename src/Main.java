import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static Client client;
    private static String nickname;
    private static JFrame frame;
    private static CardLayout cardLayout;
    private static JPanel mainPanel;

    // Lobby UI
    private static JList<String> roomList;
    private static DefaultListModel<String> roomListModel;

    // Room UI
    private static JTextArea chatArea;
    private static JTextField chatField;
    private static final JButton[][] boardButtons = new JButton[4][3];
    private static final Piece[][] boardState = new Piece[4][3];
    private static JLabel hostStatusLabel, guestStatusLabel, turnLabel;
    private static JPanel p1CapturedPanel, p2CapturedPanel;
    private static int selectedRow = -1, selectedCol = -1;
    private static Piece selectedCapturedPiece = null;
    private static final List<int[]> validMoveCells = new ArrayList<>();
    private static boolean myTurn = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            client = new Client(Main::handleServerMessage);
            showNicknamePrompt();
        });
    }
    // push체크용
    private static void showNicknamePrompt() {
        String input = JOptionPane.showInputDialog(frame, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
        if (input != null && !input.trim().isEmpty()) {
            nickname = input.trim();
            client.start(nickname);
        } else {
            System.exit(0);
        }
    }

    private static void handleServerMessage(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        SwingUtilities.invokeLater(() -> {
            switch (command) {
                case "NICKNAME_OK":
                    frame.setTitle("십이장기 - " + nickname);
                    cardLayout.show(mainPanel, "LOBBY");
                    break;
                case "NICKNAME_TAKEN":
                    JOptionPane.showMessageDialog(frame, "해당 닉네임은 이미 존재합니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    showNicknamePrompt();
                    break;
                case "UPDATE_ROOMLIST":
                    updateRoomList(payload.isEmpty() ? new String[0] : payload.split(","));
                    break;
                case "JOIN_SUCCESS":
                    enterRoom(payload);
                    break;
                case "GOTO_LOBBY":
                    cardLayout.show(mainPanel, "LOBBY");
                    frame.setTitle("십이장기 - " + nickname);
                    resetRoomUI();
                    break;
                case "CHAT":
                case "SYSTEM":
                    chatArea.append(payload + "\n");
                    break;
                case "PLAYER_READY":
                    String[] readyInfo = payload.split(" ");
                    String playerRole = readyInfo[0];
                    boolean isReady = Boolean.parseBoolean(readyInfo[1]);
                    if (playerRole.equals("HOST")) {
                        hostStatusLabel.setText("HOST: " + (isReady ? "READY" : "NOT READY"));
                    } else {
                        guestStatusLabel.setText("GUEST: " + (isReady ? "READY" : "NOT READY"));
                    }
                    break;
                case "GAME_START":
                    chatArea.append("SYSTEM: 게임이 시작되었습니다!\n");
                    hostStatusLabel.setText("HOST: PLAYING");
                    guestStatusLabel.setText("GUEST: PLAYING");
                    break;
                case "UPDATE_STATE":
                    String[] stateParts = payload.split("\\|", 4);
                    updateBoard(stateParts[0]);
                    if (stateParts.length > 2) updateCapturedPieces(stateParts[1], stateParts[2]);
                    if (stateParts.length > 3) {
                        String currentPlayerRole = stateParts[3];
                        myTurn = currentPlayerRole.equals(client.getPlayerRole());
                        turnLabel.setText(myTurn ? "당신의 턴입니다." : "상대방의 턴입니다.");
                    }
                    break;
                case "VALID_MOVES":
                    clearHighlights(false);
                    if (!payload.isEmpty()) {
                        for (String move : payload.split(";")) {
                            String[] coords = move.split(",");
                            int r = Integer.parseInt(coords[0]);
                            int c = Integer.parseInt(coords[1]);
                            boardButtons[r][c].setBackground(Color.YELLOW);
                            validMoveCells.add(new int[]{r, c});
                        }
                    }
                    break;
                case "GAME_OVER":
                    myTurn = false;
                    JOptionPane.showMessageDialog(frame, payload, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
                    resetRoomUI();
                    break;
                case "UNDO_REQUESTED":
                    int response = JOptionPane.showConfirmDialog(frame, payload + "님이 수 무르기를 요청했습니다. 수락하시겠습니까?", "수 무르기 요청", JOptionPane.YES_NO_OPTION);
                    client.sendMessage("UNDO_RESPONSE " + (response == JOptionPane.YES_OPTION));
                    break;
                case "ERROR":
                    JOptionPane.showMessageDialog(frame, payload, "오류", JOptionPane.ERROR_MESSAGE);
                    break;
                default:
                    chatArea.append(message + "\n");
                    break;
            }
        });
    }

    private static void createAndShowGUI() {
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

    private static JPanel createLobbyPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedRoom = roomList.getSelectedValue();
                    if (selectedRoom != null) {
                        client.sendMessage("JOIN_ROOM " + selectedRoom.split(" ")[0]);
                    }
                }
            }
        });
        panel.add(new JScrollPane(roomList), BorderLayout.CENTER);

        JButton createRoomButton = new JButton("방 만들기");
        createRoomButton.addActionListener(e -> {
            String roomTitle = JOptionPane.showInputDialog(frame, "방 제목을 입력하세요:");
            if (roomTitle != null && !roomTitle.trim().isEmpty()) {
                client.sendMessage("CREATE_ROOM " + roomTitle);
            }
        });
        panel.add(createRoomButton, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel northContainer = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton leaveButton = new JButton("나가기");
        leaveButton.addActionListener(e -> client.sendMessage("LEAVE_ROOM"));
        JButton readyButton = new JButton("준비");
        readyButton.addActionListener(e -> client.sendMessage("READY"));
        JButton undoButton = new JButton("수 무르기");
        undoButton.addActionListener(e -> client.sendMessage("UNDO_REQUEST"));
        buttonPanel.add(leaveButton);
        buttonPanel.add(readyButton);
        buttonPanel.add(undoButton);
        topPanel.add(buttonPanel, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        hostStatusLabel = new JLabel("HOST: NOT READY", SwingConstants.CENTER);
        guestStatusLabel = new JLabel("GUEST: NOT READY", SwingConstants.CENTER);
        statusPanel.add(hostStatusLabel);
        statusPanel.add(guestStatusLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        turnLabel = new JLabel("대기 중...", SwingConstants.CENTER);
        topPanel.add(turnLabel, BorderLayout.SOUTH);
        northContainer.add(topPanel, BorderLayout.NORTH);

        p2CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2CapturedPanel.setBorder(BorderFactory.createTitledBorder("상대방이 잡은 말"));
        p2CapturedPanel.setPreferredSize(new Dimension(0, 60));
        northContainer.add(p2CapturedPanel, BorderLayout.SOUTH);

        panel.add(northContainer, BorderLayout.NORTH);

        p1CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1CapturedPanel.setBorder(BorderFactory.createTitledBorder("내가 잡은 말"));
        p1CapturedPanel.setPreferredSize(new Dimension(0, 60));
        panel.add(p1CapturedPanel, BorderLayout.SOUTH);

        JPanel boardPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c] = new JButton();
                boardButtons[r][c].setFont(new Font("맑은 고딕", Font.BOLD, 24));
                boardButtons[r][c].setFocusable(false);
                final int finalR = r;
                final int finalC = c;
                boardButtons[r][c].addActionListener(e -> onBoardButtonClick(finalR, finalC));
                boardPanel.add(boardButtons[r][c]);
            }
        }
        panel.add(boardPanel, BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(250, 0));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        chatField = new JTextField();
        chatField.addActionListener(e -> {
            String msg = chatField.getText();
            if (!msg.isEmpty()) {
                client.sendMessage("CHAT " + msg);
                chatField.setText("");
            }
        });
        chatPanel.add(chatField, BorderLayout.SOUTH);
        panel.add(chatPanel, BorderLayout.EAST);

        return panel;
    }

    private static void onBoardButtonClick(int r, int c) {
        if (!myTurn) return;

        if (selectedCapturedPiece != null) {
            client.sendMessage(String.format("PLACE %s %d %d", selectedCapturedPiece.name(), r, c));
            clearHighlights(true);
            return;
        }

        boolean isClickOnValidMove = validMoveCells.stream().anyMatch(m -> m[0] == r && m[1] == c);

        if (selectedRow != -1) {
            if (isClickOnValidMove) {
                client.sendMessage(String.format("MOVE %d %d %d %d", selectedRow, selectedCol, r, c));
                clearHighlights(true);
            } else {
                clearHighlights(true);
                String pieceOwnerRole = getPieceOwnerRole(r, c);
                if (pieceOwnerRole != null && pieceOwnerRole.equals(client.getPlayerRole())) {
                    selectedRow = r;
                    selectedCol = c;
                    boardButtons[r][c].setBorder(new LineBorder(Color.RED, 2));
                    client.sendMessage("GET_VALID_MOVES " + r + " " + c);
                }
            }
        } else {
            String pieceOwnerRole = getPieceOwnerRole(r, c);
            if (pieceOwnerRole != null && pieceOwnerRole.equals(client.getPlayerRole())) {
                selectedRow = r;
                selectedCol = c;
                boardButtons[r][c].setBorder(new LineBorder(Color.RED, 2));
                client.sendMessage("GET_VALID_MOVES " + r + " " + c);
            }
        }
    }

    private static void onCapturedPieceClick(Piece piece, JButton button) {
        if (!myTurn) return;
        clearHighlights(true);
        selectedCapturedPiece = piece;
        button.setBorder(new LineBorder(Color.GREEN, 2));
    }

    private static void updateCapturedPieces(String p1CapturedStr, String p2CapturedStr) {
        p1CapturedPanel.removeAll();
        p2CapturedPanel.removeAll();

        String myCapturedStr = (client.getPlayerRole() != null && client.getPlayerRole().equals("P1")) ? p1CapturedStr : p2CapturedStr;
        String opponentCapturedStr = (client.getPlayerRole() != null && client.getPlayerRole().equals("P1")) ? p2CapturedStr : p1CapturedStr;

        if (myCapturedStr != null && !myCapturedStr.isEmpty()) {
            for (String pieceName : myCapturedStr.split(",")) {
                Piece piece = Piece.valueOf(pieceName);
                JButton pieceButton = new JButton(piece.getDisplayName());
                pieceButton.addActionListener(e -> onCapturedPieceClick(piece, (JButton)e.getSource()));
                p1CapturedPanel.add(pieceButton);
            }
        }

        if (opponentCapturedStr != null && !opponentCapturedStr.isEmpty()) {
            for (String pieceName : opponentCapturedStr.split(",")) {
                Piece piece = Piece.valueOf(pieceName);
                JButton pieceButton = new JButton(piece.getDisplayName());
                pieceButton.setEnabled(false);
                p2CapturedPanel.add(pieceButton);
            }
        }

        p1CapturedPanel.revalidate();
        p1CapturedPanel.repaint();
        p2CapturedPanel.revalidate();
        p2CapturedPanel.repaint();
    }

    private static void updateBoard(String boardStateStr) {
        clearHighlights(true);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c].setText("");
                boardButtons[r][c].setForeground(Color.BLACK);
                boardState[r][c] = null;
            }
        }
        if (boardStateStr.isEmpty()) return;

        for (String pieceInfo : boardStateStr.split(";")) {
            String[] info = pieceInfo.split(",");
            Piece piece = Piece.valueOf(info[0]);
            int r = Integer.parseInt(info[1]);
            int c = Integer.parseInt(info[2]);
            boardState[r][c] = piece;
            boardButtons[r][c].setText(piece.getDisplayName());
            if (piece.getOwner() == Piece.Player.P2) {
                boardButtons[r][c].setForeground(Color.BLUE);
            }
        }
    }

    private static void clearHighlights(boolean clearSelection) {
        if (clearSelection) {
            if (selectedRow != -1) {
                boardButtons[selectedRow][selectedCol].setBorder(UIManager.getBorder("Button.border"));
                selectedRow = -1;
                selectedCol = -1;
            }
            if (selectedCapturedPiece != null) {
                for (Component comp : p1CapturedPanel.getComponents()) {
                    if (comp instanceof JButton) {
                        ((JButton) comp).setBorder(UIManager.getBorder("Button.border"));
                    }
                }
                selectedCapturedPiece = null;
            }
        }
        for (int[] cell : validMoveCells) {
            boardButtons[cell[0]][cell[1]].setBackground(UIManager.getColor("Button.background"));
        }
        validMoveCells.clear();
    }

    private static String getPieceOwnerRole(int r, int c) {
        Piece piece = boardState[r][c];
        if (piece == null) return null;
        return piece.getOwner().name();
    }

    private static void updateRoomList(String[] rooms) {
        roomListModel.clear();
        for (String roomInfo : rooms) {
            roomListModel.addElement(roomInfo);
        }
    }

    private static void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        chatArea.setText("");
        frame.setTitle("십이장기 - " + roomTitle);
        resetRoomUI();
    }

    private static void resetRoomUI() {
        hostStatusLabel.setText("HOST: NOT READY");
        guestStatusLabel.setText("GUEST: NOT READY");
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
