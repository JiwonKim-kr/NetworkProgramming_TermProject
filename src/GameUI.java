import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GameUI {

    private final GameController controller;
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Panels
    private LobbyPanel lobbyPanel;
    private RoomPanel roomPanel;
    private ReplayPanel replayPanel;

    public GameUI(GameController controller) {
        this.controller = controller;
    }

    public void createAndShow() {
        frame = new JFrame("십이장기");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setJMenuBar(createLobbyMenuBar());

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        lobbyPanel = new LobbyPanel(controller);
        roomPanel = new RoomPanel(controller);
        replayPanel = new ReplayPanel(controller, this);

        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(roomPanel, "ROOM");
        mainPanel.add(replayPanel, "REPLAY");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JMenuBar createLobbyMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("환경설정");
        
        JMenuItem changeNicknameItem = new JMenuItem("닉네임 변경");
        changeNicknameItem.addActionListener(e -> controller.requestNicknameChange());

        JMenuItem rulebookItem = new JMenuItem("십이장기 룰 북");
        rulebookItem.addActionListener(e -> showRulebook());

        JMenuItem replayItem = new JMenuItem("리플레이 보기");
        replayItem.addActionListener(e -> controller.startReplay());

        settingsMenu.add(changeNicknameItem);
        settingsMenu.add(rulebookItem);
        settingsMenu.add(replayItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }

    private JMenuBar createRoomMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("환경설정");
        JMenuItem gameSettingsItem = new JMenuItem("게임 설정 (구현 예정)");
        gameSettingsItem.setEnabled(false);
        settingsMenu.add(gameSettingsItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }
    
    public void appendLobbyChat(String message) {
        lobbyPanel.appendLobbyChatMessage(message);
    }


    private void showRulebook() {
        JTextArea ruleText = new JTextArea(20, 50);
        ruleText.setEditable(false);
        ruleText.setLineWrap(true);
        ruleText.setWrapStyleWord(true);
        ruleText.setText(getRulebookText());
        JScrollPane scrollPane = new JScrollPane(ruleText);
        JOptionPane.showMessageDialog(frame, scrollPane, "십이장기 룰 북", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getRulebookText() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("RuleBook.txt")) {
            if (is == null) return "오류: RuleBook.txt 파일을 찾을 수 없습니다.";
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "오류: 룰 북 파일을 읽는 중 문제가 발생했습니다.";
        }
    }

    public void updateGameState(String payload) {
        roomPanel.updateGameState(payload);
    }
    public void showPrivateRoomPasswordDialog(String title) {
        String pw = JOptionPane.showInputDialog(null, "비밀번호 입력:");
        if (pw == null) return;
        controller.joinRoom(title + "|" + pw);
    }
    public void highlightValidMoves(String payload) {
        roomPanel.highlightValidMoves(payload);
    // --- Panel Switching ---

    public void showLobby() {
        cardLayout.show(mainPanel, "LOBBY");
        frame.setJMenuBar(createLobbyMenuBar());
        roomPanel.clearChat();
        roomPanel.resetRoomUI();
        frame.revalidate();
        frame.repaint();
    }

    public void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        frame.setTitle("십이장기 - " + roomTitle);
        frame.setJMenuBar(createRoomMenuBar());
        lobbyPanel.clearChat();
        frame.revalidate();
        frame.repaint();
    }
    public void highlightSummonRange(String myRole) {
        roomPanel.highlightSummonRange(myRole);
    }
    public void highlightSelectedCapturedPiece(Object sourceButton) {
        roomPanel.highlightSelectedCapturedPiece(sourceButton);

    public void showReplay(File replayFile) {
        replayPanel.loadReplay(replayFile);
        cardLayout.show(mainPanel, "REPLAY");
        frame.setTitle("리플레이 - " + replayFile.getName());
        frame.setJMenuBar(null); // 리플레이 중에는 메뉴바 숨김
        frame.revalidate();
        frame.repaint();
    }

    // --- Public API for Controller ---

    public void showNicknamePrompt() {
        String nickname = JOptionPane.showInputDialog(frame, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
        controller.onNicknameEntered(nickname);
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }
    

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    public void showUndoRequest(String payload) {
        int response = JOptionPane.showConfirmDialog(frame, payload + "님이 수 무르기를 요청했습니다. 수락하시겠습니까?", "수 무르기 요청", JOptionPane.YES_NO_OPTION);
        controller.respondUndo(response == JOptionPane.YES_OPTION);
    }

    // --- UI Update Delegation ---

    public void updateRoomList(String payload) { lobbyPanel.updateRoomList(payload); }
    public void appendChatMessage(String message) { roomPanel.appendChatMessage(message); }
    public void appendLobbyChatMessage(String message) { lobbyPanel.appendChatMessage(message); }
    public void updatePlayerStatus(String[] readyInfo) { roomPanel.updatePlayerStatus(readyInfo); }
    public void handleGameStart() { roomPanel.handleGameStart(); }
    public void updateGameState(String payload) { roomPanel.updateGameState(payload); }
    public void highlightValidMoves(String payload) { roomPanel.highlightValidMoves(payload); }
    public void handleGameOver(String payload) { roomPanel.handleGameOver(payload); }
    public void highlightSelectedBoardPiece(int r, int c) { roomPanel.highlightSelectedBoardPiece(r, c); }
    public void highlightSelectedCapturedPiece(Object sourceButton) { roomPanel.highlightSelectedCapturedPiece(sourceButton); }
    public void highlightPlayerPieces(String playerRole) { roomPanel.highlightPlayerPieces(playerRole); }
    public void clearHighlights(boolean clearSelection) { roomPanel.clearHighlights(clearSelection); }
    public boolean isMyTurn() { return roomPanel.isMyTurn(); }
    public String getPieceOwnerRole(int r, int c) { return roomPanel.getPieceOwnerRole(r, c); }
    public boolean isValidMove(int r, int c) { return roomPanel.isValidMove(r, c); }
    public void resetRoomUI() { roomPanel.resetRoomUI(); }
}
