import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * 클라이언트 애플리케이션의 핵심 컨트롤러 클래스입니다.
 * UI(GameUI)와 네트워크(GameClient) 사이의 중재자 역할을 합니다.
 * UI로부터 사용자 입력을 받아 서버로 전송하고, 서버로부터 받은 메시지를 파싱하여 UI를 업데이트합니다.
 */
public class GameController {

    private GameUI ui;
    private GameClient client;
    
    // 게임 내 선택 상태를 관리하는 변수들
    private int selectedRow = -1, selectedCol = -1; // 보드 위에서 선택된 기물의 좌표
    private Piece selectedCapturedPiece = null;     // 잡은 말 목록에서 선택된 기물

    private boolean isFirstTurnHighlightNeeded = false; // 게임 시작 후 첫 턴에 내 기물을 하이라이트할지 여부
    private boolean isInRoom = false; // 현재 게임방에 들어가 있는지 여부

    /**
     * 게임 컨트롤러를 시작합니다.
     * GameClient와 GameUI를 생성하고 초기화합니다.
     */
    public void start() {
        // 서버 메시지 처리를 위한 콜백으로 handleServerMessage 메서드를 등록
        this.client = new GameClient(this::handleServerMessage);
        // UI 생성 및 표시는 Swing Event Dispatch Thread에서 수행
        SwingUtilities.invokeLater(() -> {
            this.ui = new GameUI(this);
            this.ui.createAndShow();
            this.ui.showNicknamePrompt(); // 최초 실행 시 닉네임 입력 요청
        });
    }

    // --- UI로부터 발생하는 이벤트를 처리하는 메서드들 ---

    /**
     * 사용자가 닉네임을 입력했을 때 호출됩니다.
     * @param nickname 사용자가 입력한 닉네임
     */
    public void onNicknameEntered(String nickname) {
        if (nickname != null && !nickname.trim().isEmpty()) {
            client.start(nickname); // 클라이언트를 시작하고 서버에 연결
        } else {
            System.exit(0); // 닉네임 입력 취소 시 프로그램 종료
        }
    }

    /**
     * 닉네임 변경을 요청합니다.
     */
    public void requestNicknameChange() {
        String newNickname = JOptionPane.showInputDialog(null, "새 닉네임을 입력하세요:", "닉네임 변경", JOptionPane.PLAIN_MESSAGE);
        if (newNickname != null && !newNickname.trim().isEmpty()) {
            client.sendMessage(Protocol.CHANGE_NICKNAME + " " + newNickname);
        }
    }

    /**
     * 리플레이 파일을 선택하고 리플레이 화면을 시작합니다.
     */
    public void startReplay() {
        JFileChooser fileChooser = new JFileChooser("./replays"); // 기본 경로 설정
        fileChooser.setDialogTitle("리플레이 파일 선택");
        fileChooser.setFileFilter(new FileNameExtensionFilter("텍스트 파일", "txt"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ui.showReplay(selectedFile);
        }
    }

    // --- 서버로 메시지를 전송하는 메서드들 ---

    public void createRoom(String title, String password, int maxPlayers) {
        String payload = String.join("#", title, password, String.valueOf(maxPlayers));
        client.sendMessage(Protocol.CREATE_ROOM + " " + payload);
    }

    public void joinRoom(String roomName, String password) {
        String payload = roomName + "#" + password;
        client.sendMessage(Protocol.JOIN_ROOM + " " + payload);
    }

    public void sendChatMessage(String message) { client.sendMessage(Protocol.CHAT + " " + message); }
    public void sendLobbyChat(String text) { client.sendMessage(Protocol.LOBBY_CHAT + " " + text); }
    public void sendReady() { client.sendMessage(Protocol.READY); }
    public void leaveRoom() { client.sendMessage(Protocol.LEAVE_ROOM); }
    public void requestUndo() { client.sendMessage(Protocol.UNDO_REQUEST); }
    public void respondUndo(boolean accepted) { client.sendMessage(Protocol.UNDO_RESPONSE + " " + accepted); }
    public void requestRoomInfo(String title) { client.sendMessage(Protocol.REQUEST_ROOMINFO + " " + title); }

    /**
     * 게임 보드의 특정 칸이 클릭되었을 때 호출됩니다.
     * 현재 선택 상태에 따라 기물 선택, 이동, 또는 놓기 명령을 서버로 전송합니다.
     * @param r 클릭된 칸의 행 (모델 좌표)
     * @param c 클릭된 칸의 열 (모델 좌표)
     */
    public void onBoardClicked(int r, int c) {
        if (!ui.isMyTurn()) return; // 내 턴이 아니면 아무것도 하지 않음
        
        // 잡은 말을 선택한 상태에서 보드를 클릭한 경우 -> '놓기' 시도
        if (selectedCapturedPiece != null) {
            client.sendMessage(String.format("%s %s %d %d", Protocol.PLACE, selectedCapturedPiece.name(), r, c));
            clearSelections();
            return;
        }

        boolean isClickOnValidMove = ui.isValidMove(r, c);

        // 이미 보드 위의 기물을 선택한 상태에서 클릭한 경우
        if (selectedRow != -1) {
            if (isClickOnValidMove) { // 유효한 이동 범위를 클릭했다면 -> '이동'
                client.sendMessage(String.format("%s %d %d %d %d", Protocol.MOVE, selectedRow, selectedCol, r, c));
                clearSelections();
            } else { // 그 외의 칸을 클릭했다면 -> 기존 선택 취소 후 새로 선택
                clearSelections();
                selectBoardPiece(r, c);
            }
        } else { // 아무것도 선택하지 않은 상태에서 클릭한 경우 -> '선택'
            selectBoardPiece(r, c);
        }
    }

    /**
     * 잡은 말 목록의 기물이 클릭되었을 때 호출됩니다.
     * @param piece 클릭된 기물
     * @param sourceButton 클릭된 JButton 객체
     */
    public void onCapturedPieceClicked(Piece piece, Object sourceButton) {
        if (!ui.isMyTurn()) return;
        clearSelections();
        selectedCapturedPiece = piece;
        ui.highlightSelectedCapturedPiece(sourceButton);
    }

    /**
     * 보드 위의 기물을 선택하고 유효 이동 범위를 서버에 요청합니다.
     * @param r 선택할 기물의 행
     * @param c 선택할 기물의 열
     */
    private void selectBoardPiece(int r, int c) {
        String pieceOwner = ui.getPieceOwnerRole(r, c);
        // 자신의 기물일 경우에만 선택 가능
        if (pieceOwner != null && pieceOwner.equals(client.getPlayerRole())) {
            selectedRow = r;
            selectedCol = c;
            ui.highlightSelectedBoardPiece(r, c);
            client.sendMessage(Protocol.GET_VALID_MOVES + " " + r + " " + c);
        }
    }

    /**
     * 모든 선택 상태(선택된 기물, 하이라이트 등)를 초기화합니다.
     */
    private void clearSelections() {
        selectedRow = -1;
        selectedCol = -1;
        selectedCapturedPiece = null;
        ui.clearHighlights(true);
    }

    public boolean isMyTurn(String currentPlayerRole) {
        if (client == null) return false;
        return currentPlayerRole.equals(client.getPlayerRole());
    }

    public String getPlayerRole() {
        return client.getPlayerRole();
    }

    // --- 서버로부터 받은 메시지를 처리하는 메서드 ---

    /**
     * GameClient로부터 서버 메시지를 받아 파싱하고, UI 업데이트를 요청합니다.
     * 모든 UI 관련 작업은 SwingUtilities.invokeLater를 통해 EDT에서 실행되도록 보장합니다.
     * @param message 서버로부터 받은 원본 메시지
     */
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String payload = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case Protocol.NICKNAME_OK:
                    ui.setTitle("십이장기 - " + client.getNickname());
                    ui.showLobby();
                    break;
                case Protocol.NICKNAME_TAKEN:
                    ui.showError("해당 닉네임은 이미 존재합니다.");
                    ui.showNicknamePrompt();
                    break;
                case Protocol.NICKNAME_CHANGED_OK:
                    client.setNickname(payload);
                    ui.setTitle("십이장기 - " + payload);
                    JOptionPane.showMessageDialog(null, "닉네임이 성공적으로 변경되었습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case Protocol.NICKNAME_CHANGE_FAILED:
                    ui.showError("닉네임 변경에 실패했습니다: " + payload);
                    break;
                case Protocol.UPDATE_ROOMLIST:
                    if (!isInRoom) { // 방에 들어가 있을 때는 로비 정보를 업데이트하지 않음
                        ui.updateRoomList(payload);
                    }
                    break;
                case Protocol.LOBBY_CHAT:                
                    ui.appendLobbyChat(payload);
                    break;
                case Protocol.JOIN_SUCCESS:
                    isInRoom = true;
                    ui.enterRoom(payload);
                    break;
                case Protocol.GOTO_LOBBY:
                    isInRoom = false;
                    ui.showLobby();
                    ui.setTitle("십이장기 - " + client.getNickname());
                    ui.resetRoomUI();
                    break;
                case Protocol.CHAT:
                case Protocol.SYSTEM:
                    // 현재 위치(방/로비)에 따라 적절한 채팅창에 메시지 표시
                    if (isInRoom) {
                        ui.appendChatMessage(payload);
                    } else {
                        ui.appendLobbyChatMessage(payload);
                    }
                    break;
                case Protocol.PLAYER_READY:
                    ui.updatePlayerStatus(payload.split(" "));
                    break;
                case Protocol.GAME_START:
                    isFirstTurnHighlightNeeded = true;
                    ui.handleGameStart();
                    break;
                case Protocol.UPDATE_STATE:
                    ui.updateGameState(payload);
                    // 게임 시작 후 첫 턴일 때만 내 기물들을 하이라이트
                    if (isFirstTurnHighlightNeeded && ui.isMyTurn()) {
                        ui.highlightPlayerPieces(client.getPlayerRole());
                        isFirstTurnHighlightNeeded = false;
                    }
                    break;
                case Protocol.VALID_MOVES:
                    ui.highlightValidMoves(payload);
                    break;
                case Protocol.GAME_OVER:
                    isInRoom = false;
                    ui.handleGameOver(payload);
                    break;
                case Protocol.UNDO_REQUESTED:
                    ui.showUndoRequest(payload);
                    break;
                case Protocol.ERROR:
                    ui.showError(payload);
                    break;
                default:
                    // 알 수 없는 메시지는 현재 위치의 채팅창에 그대로 출력
                    if (isInRoom) ui.appendChatMessage(message);
                    else ui.appendLobbyChatMessage(message);
                    break;
            }
        });
    }
}
