import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class GameController {

    private GameUI ui;
    private GameClient client;
    private int selectedRow = -1, selectedCol = -1;
    private Piece selectedCapturedPiece = null;
    private boolean isFirstTurnHighlightNeeded = false;
    private boolean isInRoom = false;

    public void start() {
        this.client = new GameClient(this::handleServerMessage);
        SwingUtilities.invokeLater(() -> {
            this.ui = new GameUI(this);
            this.ui.createAndShow();
            this.ui.showNicknamePrompt();
        });
    }

    // --- UI Events ---
    public void onNicknameEntered(String nickname) {
        if (nickname != null && !nickname.trim().isEmpty()) {
            client.start(nickname);
        } else {
            System.exit(0);
        }
    }

    public void requestNicknameChange() {
        String newNickname = JOptionPane.showInputDialog(null, "새 닉네임을 입력하세요:", "닉네임 변경", JOptionPane.PLAIN_MESSAGE);
        if (newNickname != null && !newNickname.trim().isEmpty()) {
            client.sendMessage(Protocol.CHANGE_NICKNAME + " " + newNickname);
        }
    }

    public void startReplay() {
        JFileChooser fileChooser = new JFileChooser("./replays");
        fileChooser.setDialogTitle("리플레이 파일 선택");
        fileChooser.setFileFilter(new FileNameExtensionFilter("텍스트 파일", "txt"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            ui.showReplay(selectedFile);
        }
    }

    public void createRoom(String title, String password, int maxPlayers) {
        String payload = String.join("#", title, password, String.valueOf(maxPlayers));
        client.sendMessage(Protocol.CREATE_ROOM + " " + payload);
    }

    public void joinRoom(String roomName, String password) {
        String payload = roomName + "#" + password;
        client.sendMessage(Protocol.JOIN_ROOM + " " + payload);
    }

    public void sendChatMessage(String message) { client.sendMessage(Protocol.CHAT + " " + message); }
    public void sendReady() { client.sendMessage(Protocol.READY); }
    public void leaveRoom() { client.sendMessage(Protocol.LEAVE_ROOM); }
    public void requestUndo() { client.sendMessage(Protocol.UNDO_REQUEST); }
    public void respondUndo(boolean accepted) { client.sendMessage(Protocol.UNDO_RESPONSE + " " + accepted); }
    public void requestRoomInfo(String title) {
        client.sendMessage(Protocol.REQUEST_ROOMINFO + " " + title);
    }
    public void onBoardClicked(int r, int c) {
        if (!ui.isMyTurn()) return;
        
        if (selectedCapturedPiece != null) {
            client.sendMessage(String.format("%s %s %d %d", Protocol.PLACE, selectedCapturedPiece.name(), r, c));
            clearSelections();
            
            return;
        }

        boolean isClickOnValidMove = ui.isValidMove(r, c);

        if (selectedRow != -1) {
            if (isClickOnValidMove) {
                client.sendMessage(String.format("%s %d %d %d %d", Protocol.MOVE, selectedRow, selectedCol, r, c));
                clearSelections();
            } else {
                clearSelections();
                selectBoardPiece(r, c);
            }
        } else {
            selectBoardPiece(r, c);
        }
    }

    public void onCapturedPieceClicked(Piece piece, Object sourceButton) {
        if (!ui.isMyTurn()) return;
        clearSelections();
        selectedCapturedPiece = piece;
        ui.highlightSelectedCapturedPiece(sourceButton);
    }

    private void selectBoardPiece(int r, int c) {
        String pieceOwner = ui.getPieceOwnerRole(r, c);
        if (pieceOwner != null && pieceOwner.equals(client.getPlayerRole())) {
            selectedRow = r;
            selectedCol = c;
            ui.highlightSelectedBoardPiece(r, c);
            client.sendMessage(Protocol.GET_VALID_MOVES + " " + r + " " + c);
        }
    }

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
    public void sendLobbyChat(String text) {
        client.sendMessage(Protocol.LOBBY_CHAT + " " + text);
    }

    // --- Server Messages ---
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
                    if (!isInRoom) {
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
                    if (isInRoom) ui.appendChatMessage(message);
                    else ui.appendLobbyChatMessage(message);
                    break;
            }
        });
    }
}
