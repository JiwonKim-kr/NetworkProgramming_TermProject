import javax.swing.*;
import java.awt.*;

/**
 * 게임방의 전체적인 UI를 구성하는 패널입니다.
 * 이 패널은 게임 보드(BoardPanel), 채팅(ChatPanel), 플레이어 상태, 잡은 말 목록 등
 * 게임 진행에 필요한 모든 시각적 요소를 포함하고 배치합니다.
 */
public class RoomPanel extends JPanel {
    private final GameController controller;

    // 하위 패널 및 컴포넌트
    private final BoardPanel boardPanel;
    private final ChatPanel chatPanel;
    private JLabel hostStatusLabel, guestStatusLabel, turnLabel;
    private JPanel p1CapturedPanel, p2CapturedPanel; // 각 플레이어가 잡은 말을 표시하는 패널
    private JTextArea gameMoveHistoryArea; // 실시간 기보 표시 영역
    private final Color DEFAULT_BG = UIManager.getColor("Panel.background"); // 기본 배경색 저장

    /**
     * RoomPanel 생성자입니다.
     * @param controller UI 이벤트를 처리하고 게임 로직과 통신할 컨트롤러
     */
    public RoomPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 하위 컴포넌트들 생성
        this.boardPanel = new BoardPanel(controller);
        this.chatPanel = new ChatPanel(controller);
        
        // 패널에 컴포넌트들 배치
        this.add(createTopPanel(), BorderLayout.NORTH);
        this.add(createBottomPanel(), BorderLayout.SOUTH);
        this.add(createCenterPanel(), BorderLayout.CENTER);
        this.add(chatPanel, BorderLayout.EAST);
    }

    /**
     * 중앙 영역 패널(게임 보드, 기보 표시 영역)을 생성합니다.
     * @return 생성된 중앙 패널
     */
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        gameMoveHistoryArea = new JTextArea(1, 30);
        gameMoveHistoryArea.setEditable(false);
        gameMoveHistoryArea.setLineWrap(false); // 기보가 길어지면 가로 스크롤이 생기도록 설정
        JScrollPane historyScrollPane = new JScrollPane(gameMoveHistoryArea);
        historyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        centerPanel.add(boardPanel, BorderLayout.CENTER);
        centerPanel.add(historyScrollPane, BorderLayout.SOUTH);
        
        return centerPanel;
    }

    /**
     * 상단 영역 패널(버튼, 플레이어 상태, 상대가 잡은 말)을 생성합니다.
     * @return 생성된 상단 패널
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // 나가기, 준비, 수 무르기 버튼
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

        // 호스트, 게스트 준비 상태 표시
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        hostStatusLabel = new JLabel(Protocol.HOST + ": NOT READY", SwingConstants.CENTER);
        guestStatusLabel = new JLabel(Protocol.GUEST + ": NOT READY", SwingConstants.CENTER);
        statusPanel.add(hostStatusLabel);
        statusPanel.add(guestStatusLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // 현재 턴 표시
        turnLabel = new JLabel("대기 중...", SwingConstants.CENTER);
        turnLabel.setOpaque(true);
        turnLabel.setBackground(DEFAULT_BG);
        topPanel.add(turnLabel, BorderLayout.SOUTH);
        
        // 상대방(P2)이 잡은 말 표시 패널
        p2CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2CapturedPanel.setBorder(BorderFactory.createTitledBorder("상대방이 잡은 말"));
        p2CapturedPanel.setPreferredSize(new Dimension(0, 60));
        
        // 상단 컴포넌트들을 하나로 묶는 컨테이너
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(p2CapturedPanel, BorderLayout.SOUTH);
        
        return northContainer;
    }

    /**
     * 하단 영역 패널(내가 잡은 말)을 생성합니다.
     * @return 생성된 하단 패널
     */
    private JPanel createBottomPanel() {
        // 나(P1)이 잡은 말 표시 패널
        p1CapturedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1CapturedPanel.setBorder(BorderFactory.createTitledBorder("내가 잡은 말"));
        p1CapturedPanel.setPreferredSize(new Dimension(0, 60));
        return p1CapturedPanel;
    }
    
    /**
     * 잡은 말을 내려놓을 수 있는 범위(소환 가능 범위)를 하이라이트합니다.
     * @param myRole 현재 플레이어의 역할 (P1 또는 P2)
     */
    public void highlightSummonRange(String myRole) {
        // 십이장기 규칙상 잡은 말은 2~4행의 빈칸에만 놓을 수 있습니다.
        // (P1 기준. P2는 1~3행)
        // 이 구현은 P1 기준으로 2~4행(인덱스 1~3)을 하이라이트합니다.
        int frontRow = 1;

        if (frontRow == -1)
            return;

        for (int r = frontRow; r <= 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (isEmptyCell(r, c)) {
                    highlightSelectedBoardPiece(r, c);
                }
            }
        }
    }

    /**
     * 해당 칸이 비어있는지 확인합니다.
     * @param r 행
     * @param c 열
     * @return 비어있으면 true
     */
    private boolean isEmptyCell(int r, int c) {
        return getPieceOwnerRole(r, c) == null;
    }

    /**
     * 플레이어의 준비 상태를 UI에 반영합니다.
     * @param readyInfo [0]: 역할(HOST/GUEST), [1]: 준비상태(true/false)
     */
    public void updatePlayerStatus(String[] readyInfo) { 
    	boardPanel.clearHighlights(false);
        String playerRole = readyInfo[0]; 
        boolean isReady = Boolean.parseBoolean(readyInfo[1]); 
        JLabel targetLabel = playerRole.equals(Protocol.HOST) ? hostStatusLabel : guestStatusLabel;
        targetLabel.setText(playerRole + ": " + (isReady ? "READY" : "NOT READY"));
    }

    /**
     * 게임 시작 시 UI를 처리합니다.
     */
    public void handleGameStart() { 
        appendChatMessage(Protocol.SYSTEM + ": 게임이 시작되었습니다!\n");
        hostStatusLabel.setText(Protocol.HOST + ": PLAYING"); 
        guestStatusLabel.setText(Protocol.GUEST + ": PLAYING"); 
    }

    /**
     * 서버로부터 받은 게임 상태 정보로 UI를 업데이트합니다.
     * @param payload 보드 상태, 잡은 말 목록, 현재 턴, 기보 등의 정보가 담긴 문자열
     */
    public void updateGameState(String payload) {
    	boardPanel.clearHighlights(false);
        boardPanel.setPlayerRoleForView(controller.getPlayerRole());

        String[] parts = payload.split("#", 2);
        String gameStatePayload = parts[0];
        String moveHistoryPayload = (parts.length > 1) ? parts[1] : "";

        String[] stateParts = gameStatePayload.split("\\|", 4);
        boardPanel.updateBoard(stateParts[0]); // 보드 판 업데이트
        if (stateParts.length > 2) {
            // 잡은 말 목록 업데이트
            boardPanel.updateCapturedPieces(p1CapturedPanel, p2CapturedPanel, stateParts[1], stateParts[2]);
        }
        if (stateParts.length > 3) {
            // 현재 턴 정보 업데이트
            String currentPlayerRole = stateParts[3];
            boolean isMyTurn = controller.isMyTurn(currentPlayerRole);
            boardPanel.setMyTurn(isMyTurn);
            turnLabel.setText(isMyTurn ? "당신의 턴입니다." : "상대방의 턴입니다.");
            applyTurnBackground(isMyTurn);
        }

        gameMoveHistoryArea.setText(moveHistoryPayload); // 기보 표시 영역 업데이트
        // 스크롤을 항상 오른쪽 끝으로 이동
        gameMoveHistoryArea.setCaretPosition(gameMoveHistoryArea.getDocument().getLength());
    }
    
    // --- BoardPanel에 대한 위임 메서드들 ---
    public void highlightValidMoves(String payload) { boardPanel.highlightValidMoves(payload); }
    public void highlightSelectedBoardPiece(int r, int c) { boardPanel.highlightSelectedBoardPiece(r,c); }
    public void highlightSelectedCapturedPiece(Object sourceButton) { boardPanel.highlightSelectedCapturedPiece(sourceButton); }
    public void highlightPlayerPieces(String playerRole) { boardPanel.highlightPlayerPieces(playerRole); }
    public void clearHighlights(boolean clearSelection) { boardPanel.clearHighlights(clearSelection); }
    public boolean isMyTurn() { return boardPanel.isMyTurn(); }
    public String getPieceOwnerRole(int r, int c) { return boardPanel.getPieceOwnerRole(r, c); }
    public boolean isValidMove(int r, int c) { return boardPanel.isValidMove(r, c); }

    /**
     * 게임 종료 메시지를 표시하고 UI를 초기 상태로 되돌립니다.
     * @param payload 게임 종료 이유가 담긴 메시지
     */
    public void handleGameOver(String payload) {
        boardPanel.setMyTurn(false);
        JOptionPane.showMessageDialog(this, payload, "게임 종료", JOptionPane.INFORMATION_MESSAGE);
        resetRoomUI();
    }

    public void appendChatMessage(String message) { chatPanel.appendChatMessage(message); }
    public void clearChat() { chatPanel.clearChat(); }

    /**
     * 방의 모든 UI 요소를 초기 상태(게임 대기 상태)로 되돌립니다.
     */
    public void resetRoomUI() { 
        turnLabel.setBackground(DEFAULT_BG);
        hostStatusLabel.setText(Protocol.HOST + ": NOT READY"); 
        guestStatusLabel.setText(Protocol.GUEST + ": NOT READY"); 
        turnLabel.setText("대기 중..."); 
        boardPanel.resetBoard();
        gameMoveHistoryArea.setText("");
        // 잡은 말 패널 비우기
        p1CapturedPanel.removeAll();
        p1CapturedPanel.revalidate();
        p1CapturedPanel.repaint();
        p2CapturedPanel.removeAll();
        p2CapturedPanel.revalidate();
        p2CapturedPanel.repaint();
        chatPanel.resetChat();
    }

    /**
     * 현재 턴에 따라 턴 라벨의 배경색을 변경합니다.
     * @param isMyTurn 내 턴이면 true
     */
    private void applyTurnBackground(boolean isMyTurn) {
        Color bg = isMyTurn ? Color.ORANGE : DEFAULT_BG;
        turnLabel.setBackground(bg);
    }
}
