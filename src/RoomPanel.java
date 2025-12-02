import javax.swing.*;
import java.awt.*;

/**
 * 게임방의 전체 UI를 구성하는 패널입니다.
 * 게임 보드, 채팅, 플레이어 상태 등 다양한 하위 컴포넌트를 포함합니다.
 */
public class RoomPanel extends JPanel {
    private final GameController controller;

    // 하위 패널 및 컴포넌트
    private final BoardPanel boardPanel;
    private final ChatPanel chatPanel;
    private JLabel hostStatusLabel, guestStatusLabel, turnLabel;
    private JPanel p1CapturedPanel, p2CapturedPanel;
    private final Color DEFAULT_BG = UIManager.getColor("Panel.background");

    public RoomPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 하위 패널들 생성
        this.boardPanel = new BoardPanel(controller);
        this.chatPanel = new ChatPanel(controller);
        
        // UI 구성
        this.add(createTopPanel(), BorderLayout.NORTH);
        this.add(createBottomPanel(), BorderLayout.SOUTH);
        this.add(boardPanel, BorderLayout.CENTER);
        this.add(chatPanel, BorderLayout.EAST);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        
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

        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        hostStatusLabel = new JLabel(Protocol.HOST + ": NOT READY", SwingConstants.CENTER);
        guestStatusLabel = new JLabel(Protocol.GUEST + ": NOT READY", SwingConstants.CENTER);
        statusPanel.add(hostStatusLabel);
        statusPanel.add(guestStatusLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        turnLabel = new JLabel("대기 중...", SwingConstants.CENTER);
        turnLabel.setOpaque(true);
        turnLabel.setBackground(DEFAULT_BG);
        topPanel.add(turnLabel, BorderLayout.SOUTH);
        
        p2CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2CapturedPanel.setBorder(BorderFactory.createTitledBorder("상대방이 잡은 말"));
        p2CapturedPanel.setPreferredSize(new Dimension(0, 60));
        
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(p2CapturedPanel, BorderLayout.SOUTH);
        
        return northContainer;
    }

    private JPanel createBottomPanel() {
        p1CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1CapturedPanel.setBorder(BorderFactory.createTitledBorder("내가 잡은 말"));
        p1CapturedPanel.setPreferredSize(new Dimension(0, 60));
        return p1CapturedPanel;
    }

    // --- Public API for UI Updates (GameUI로부터 위임받음) ---

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

    public void updateGameState(String payload) {
        // 게임 상태가 업데이트 될 때마다 BoardPanel의 시점을 현재 플레이어 역할에 맞게 설정
        boardPanel.setPlayerRoleForView(controller.getPlayerRole());

        String[] stateParts = payload.split("\\|", 4);
        boardPanel.updateBoard(stateParts[0]);
        if (stateParts.length > 2) {
            boardPanel.updateCapturedPieces(p1CapturedPanel, p2CapturedPanel, stateParts[1], stateParts[2]);
        }
        if (stateParts.length > 3) {
            String currentPlayerRole = stateParts[3];
            boolean isMyTurn = controller.isMyTurn(currentPlayerRole);
            boardPanel.setMyTurn(isMyTurn);
            turnLabel.setText(isMyTurn ? "당신의 턴입니다." : "상대방의 턴입니다.");
            applyTurnBackground(isMyTurn);
        }
    }
    
    public void highlightValidMoves(String payload) {
        boardPanel.highlightValidMoves(payload);
    }

    public void highlightSelectedBoardPiece(int r, int c) {
        boardPanel.highlightSelectedBoardPiece(r,c);
    }

    public void highlightSelectedCapturedPiece(Object sourceButton) {
        boardPanel.highlightSelectedCapturedPiece(sourceButton);
    }

    public void highlightPlayerPieces(String playerRole) {
        boardPanel.highlightPlayerPieces(playerRole);
    }

    public void clearHighlights(boolean clearSelection) {
        boardPanel.clearHighlights(clearSelection);
    }

    public boolean isMyTurn() {
        return boardPanel.isMyTurn();
    }

    public String getPieceOwnerRole(int r, int c) {
        return boardPanel.getPieceOwnerRole(r, c);
    }

    public boolean isValidMove(int r, int c) {
        return boardPanel.isValidMove(r, c);
    }



    public void handleGameOver(String payload) {
        boardPanel.setMyTurn(false);
        JOptionPane.showMessageDialog(this, payload, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
        resetRoomUI();
    }

    public void appendChatMessage(String message) {
        chatPanel.appendChatMessage(message);
    }

    public void clearChat() {
        chatPanel.clearChat();
    }

    public void resetRoomUI() { 
        turnLabel.setBackground(DEFAULT_BG);
        hostStatusLabel.setText(Protocol.HOST + ": NOT READY"); 
        guestStatusLabel.setText(Protocol.GUEST + ": NOT READY"); 
        turnLabel.setText("대기 중..."); 
        boardPanel.resetBoard();
        p1CapturedPanel.removeAll();
        p1CapturedPanel.revalidate();
        p1CapturedPanel.repaint();
        p2CapturedPanel.removeAll();
        p2CapturedPanel.revalidate();
        p2CapturedPanel.repaint();
    }

    private void applyTurnBackground(boolean isMyTurn) {
        Color bg = isMyTurn ? Color.ORANGE : DEFAULT_BG;
        turnLabel.setBackground(bg);
    }
}
