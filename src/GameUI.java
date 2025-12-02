import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

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

        // 초기 메뉴바를 로비 메뉴바로 설정합니다.
        frame.setJMenuBar(createLobbyMenuBar());

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

    /**
     * 로비 화면에 표시될 메뉴바를 생성합니다.
     * @return 로비용 JMenuBar
     */
    private JMenuBar createLobbyMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("환경설정");
        JMenuItem changeNicknameItem = new JMenuItem("닉네임 변경");
        changeNicknameItem.addActionListener(e -> controller.requestNicknameChange());

        JMenuItem rulebookItem = new JMenuItem("십이장기 룰 북");
        rulebookItem.addActionListener(e -> showRulebook());

        settingsMenu.add(changeNicknameItem);
        settingsMenu.add(rulebookItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }

    /**
     * 게임방 화면에 표시될 메뉴바를 생성합니다.
     * @return 게임방용 JMenuBar
     */
    private JMenuBar createRoomMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("환경설정");
        
        JMenuItem gameSettingsItem = new JMenuItem("게임 설정 (구현 예정)");
        gameSettingsItem.setEnabled(false);
        
        settingsMenu.add(gameSettingsItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }

    /**
     * 십이장기 룰 북을 JTextArea를 포함하는 다이얼로그로 표시합니다.
     */
    private void showRulebook() {
        JTextArea ruleText = new JTextArea(20, 50);
        ruleText.setEditable(false);
        ruleText.setLineWrap(true);
        ruleText.setWrapStyleWord(true);
        ruleText.setText(getRulebookText());

        JScrollPane scrollPane = new JScrollPane(ruleText);
        JOptionPane.showMessageDialog(frame, scrollPane, "십이장기 룰 북", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 클래스패스에서 RuleBook.txt 파일을 읽어 그 내용을 문자열로 반환합니다.
     * @return 파일 내용 또는 오류 메시지
     */
    private String getRulebookText() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("RuleBook.txt")) {
            if (is == null) {
                return "오류: RuleBook.txt 파일을 찾을 수 없습니다.";
            }
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "오류: 룰 북 파일을 읽는 중 문제가 발생했습니다.";
        }
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
        frame.setJMenuBar(createLobbyMenuBar());
        
        roomPanel.clearChat(); // 게임방 채팅창 클리어
        roomPanel.resetRoomUI(); // 방에서 로비로 나올 때 방 UI 초기화

        frame.revalidate();
        frame.repaint();
    }

    public void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        frame.setTitle("십이장기 - " + roomTitle);
        frame.setJMenuBar(createRoomMenuBar());

        lobbyPanel.clearChat(); // 로비 채팅창 클리어

        frame.revalidate();
        frame.repaint();
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

    public void appendLobbyChatMessage(String message) {
        lobbyPanel.appendChatMessage(message);
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
