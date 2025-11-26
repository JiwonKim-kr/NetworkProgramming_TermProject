import javax.swing.*;
import java.awt.*;

/**
 * 게임의 메인 프레임과 화면 전환을 관리하는 최상위 UI 컨테이너입니다.
 * 로비(LobbyPanel)와 게임방(RoomPanel)을 CardLayout으로 전환하는 역할을 합니다.
 */
public class GameUI {

    private final GameController controller;
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // 하위 패널
    private LobbyPanel lobbyPanel;
    private RoomPanel roomPanel;

    public GameUI(GameController controller) {
        this.controller = controller;
    }

    /**
     * 메인 프레임과 하위 패널들을 생성하고 화면에 표시합니다.
     */
    public void createAndShow() {
        frame = new JFrame("십이장기");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        lobbyPanel = new LobbyPanel(controller);
        roomPanel = new RoomPanel(controller);

        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(roomPanel, "ROOM");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- Public API for Controller ---

    public void showNicknamePrompt() {
        String nickname = JOptionPane.showInputDialog(frame, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
        controller.onNicknameEntered(nickname);
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void showLobby() {
        cardLayout.show(mainPanel, "LOBBY");
        roomPanel.resetRoomUI(); // 방에서 로비로 나올 때 방 UI 초기화
    }

    public void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        frame.setTitle("십이장기 - " + roomTitle);
        
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void showUndoRequest(String payload) {
        int response = JOptionPane.showConfirmDialog(frame, payload + "님이 수 무르기를 요청했습니다. 수락하시겠습니까?", "수 무르기 요청", JOptionPane.YES_NO_OPTION);
        controller.respondUndo(response == JOptionPane.YES_OPTION);
    }

    // --- UI Update Delegation ---

    public void updateRoomList(String payload) {
        lobbyPanel.updateRoomList(payload);
    }

    public void appendChatMessage(String message) {
        // RoomPanel이 ChatPanel을 관리하므로 RoomPanel에 위임
        roomPanel.appendChatMessage(message);
    }

    public void updatePlayerStatus(String[] readyInfo) {
        roomPanel.updatePlayerStatus(readyInfo);
    }

    public void handleGameStart() {
        roomPanel.handleGameStart();
    }

    public void updateGameState(String payload) {
        roomPanel.updateGameState(payload);
    }

    public void highlightValidMoves(String payload) {
        roomPanel.highlightValidMoves(payload);
    }

    public void handleGameOver(String payload) {
        roomPanel.handleGameOver(payload);
    }
    
    public void highlightSelectedBoardPiece(int r, int c) {
        roomPanel.highlightSelectedBoardPiece(r, c);
    }

    public void highlightSelectedCapturedPiece(Object sourceButton) {
        roomPanel.highlightSelectedCapturedPiece(sourceButton);
    }

    public void highlightPlayerPieces(String playerRole) {
        roomPanel.highlightPlayerPieces(playerRole);
    }

    public void clearHighlights(boolean clearSelection) {
        roomPanel.clearHighlights(clearSelection);
    }

    public boolean isMyTurn() {
        return roomPanel.isMyTurn();
    }

    public String getPieceOwnerRole(int r, int c) {
        return roomPanel.getPieceOwnerRole(r, c);
    }

    public boolean isValidMove(int r, int c) {
        return roomPanel.isValidMove(r, c);
    }

    public void resetRoomUI() {
        roomPanel.resetRoomUI();
    }
}
