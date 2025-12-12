import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 게임의 전체적인 사용자 인터페이스(UI)를 관리하는 메인 프레임 클래스입니다.
 * 로비, 게임방, 리플레이 화면 간의 전환을 담당하며, 메뉴바와 같은 공통 UI 요소를 생성합니다.
 * GameController로부터 UI 업데이트 요청을 받아 각 하위 패널에 전달하는 역할을 합니다.
 */
public class GameUI {

    private final GameController controller;
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // 화면 전환에 사용될 패널들
    private LobbyPanel lobbyPanel;
    private RoomPanel roomPanel;
    private ReplayPanel replayPanel;

    /**
     * GameUI 생성자입니다.
     * @param controller UI 이벤트를 처리할 게임 컨트롤러
     */
    public GameUI(GameController controller) {
        this.controller = controller;
    }

    /**
     * UI를 생성하고 화면에 표시합니다.
     * 메인 프레임, 카드 레이아웃, 각 패널들을 초기화합니다.
     */
    public void createAndShow() {
        frame = new JFrame("십이장기");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setJMenuBar(createLobbyMenuBar());

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 각 화면에 해당하는 패널들 생성
        lobbyPanel = new LobbyPanel(controller);
        roomPanel = new RoomPanel(controller);
        replayPanel = new ReplayPanel(controller, this);

        // 카드 레이아웃에 패널들 추가
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(roomPanel, "ROOM");
        mainPanel.add(replayPanel, "REPLAY");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null); // 화면 중앙에 위치
        frame.setVisible(true);
    }

    /**
     * 로비 화면에서 사용될 메뉴바를 생성합니다.
     * @return 생성된 JMenuBar 객체
     */
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

    /**
     * 게임방 화면에서 사용될 메뉴바를 생성합니다.
     * @return 생성된 JMenuBar 객체
     */
    private JMenuBar createRoomMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("환경설정");
        JMenuItem gameSettingsItem = new JMenuItem("게임 설정 (구현 예정)");
        gameSettingsItem.setEnabled(false); // 아직 구현되지 않은 기능
        settingsMenu.add(gameSettingsItem);
        menuBar.add(settingsMenu);
        return menuBar;
    }
    
    /**
     * 로비 채팅 패널에 메시지를 추가합니다.
     * @param message 추가할 채팅 메시지
     */
    public void appendLobbyChat(String message) {
        lobbyPanel.appendLobbyChatMessage(message);
    }

    /**
     * 십이장기 규칙이 담긴 다이얼로그를 표시합니다.
     */
    private void showRulebook() {
        JTextArea ruleText = new JTextArea(20, 50);
        ruleText.setEditable(false);
        ruleText.setLineWrap(true);
        ruleText.setWrapStyleWord(true);
        ruleText.setText(getRulebookText()); // 리소스 파일에서 규칙 텍스트를 읽어옴
        JScrollPane scrollPane = new JScrollPane(ruleText);
        JOptionPane.showMessageDialog(frame, scrollPane, "십이장기 룰 북", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 리소스 폴더의 RuleBook.txt 파일 내용을 읽어 문자열로 반환합니다.
     * @return 규칙이 담긴 문자열
     */
    private String getRulebookText() {
        // try-with-resources 구문을 사용하여 스트림 자동 해제
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

    // --- 화면 전환(Panel Switching) 메서드들 ---

    /**
     * 로비 화면으로 전환합니다.
     * 방에서 나왔을 때 UI 상태를 초기화하는 역할도 합니다.
     */
    public void showLobby() {
        cardLayout.show(mainPanel, "LOBBY");
        frame.setJMenuBar(createLobbyMenuBar());
        roomPanel.clearChat(); // 방 채팅 내용 초기화
        roomPanel.resetRoomUI(); // 방 UI 상태 초기화
        frame.revalidate();
        frame.repaint();
    }

    /**
     * 게임방 화면으로 전환합니다.
     * @param roomTitle 창 제목에 표시될 방의 이름
     */
    public void enterRoom(String roomTitle) {
        cardLayout.show(mainPanel, "ROOM");
        frame.setTitle("십이장기 - " + roomTitle);
        frame.setJMenuBar(createRoomMenuBar());
        lobbyPanel.clearChat(); // 로비 채팅 내용 초기화
        frame.revalidate();
        frame.repaint();
    }
    
    /**
     * 리플레이 화면으로 전환합니다.
     * @param replayFile 재생할 리플레이 파일
     */
    public void showReplay(File replayFile) {
        replayPanel.loadReplay(replayFile);
        cardLayout.show(mainPanel, "REPLAY");
        frame.setTitle("리플레이 - " + replayFile.getName());
        frame.setJMenuBar(null); // 리플레이 중에는 메뉴바를 숨김
        frame.revalidate();
        frame.repaint();
    }

    // --- 컨트롤러를 위한 공개 API ---

    /**
     * 사용자에게 닉네임 입력을 요청하는 다이얼로그를 표시합니다.
     */
    public void showNicknamePrompt() {
        String nickname = JOptionPane.showInputDialog(frame, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
        controller.onNicknameEntered(nickname);
    }

    /**
     * 메인 프레임의 제목을 설정합니다.
     * @param title 새로운 제목
     */
    public void setTitle(String title) {
        frame.setTitle(title);
    }
    
    /**
     * 사용자에게 오류 메시지를 다이얼로그로 표시합니다.
     * @param message 표시할 오류 메시지
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "오류", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 상대방의 수 무르기 요청을 다이얼로그로 표시하고 사용자의 응답을 컨트롤러에 전달합니다.
     * @param payload 요청한 사용자의 닉네임이 포함된 메시지
     */
    public void showUndoRequest(String payload) {
        int response = JOptionPane.showConfirmDialog(frame, payload + "님이 수 무르기를 요청했습니다. 수락하시겠습니까?", "수 무르기 요청", JOptionPane.YES_NO_OPTION);
        controller.respondUndo(response == JOptionPane.YES_OPTION);
    }

    // --- UI 업데이트 위임(Delegation) 메서드들 ---
    // GameController로부터 받은 UI 업데이트 요청을 실제 작업을 수행할 하위 패널로 전달합니다.
    
    public void highlightSummonRange(String myRole) {roomPanel.highlightSummonRange(myRole);}
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
