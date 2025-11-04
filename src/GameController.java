import javax.swing.SwingUtilities;

public class GameController {

    private GameUI ui;
    private GameClient client;
    private int selectedRow = -1, selectedCol = -1;
    private Piece selectedCapturedPiece = null;

    public void start() {
        this.client = new GameClient(this::handleServerMessage);
        SwingUtilities.invokeLater(() -> {
            this.ui = new GameUI(this);
            // 1. GUI 컴포넌트를 먼저 생성하고 화면에 표시합니다.
            this.ui.createAndShow(); 
            // 2. 그 후에 닉네임 입력을 요청합니다.
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

    public void createRoom(String roomTitle) { client.sendMessage("CREATE_ROOM " + roomTitle); }
    public void joinRoom(String roomName) { client.sendMessage("JOIN_ROOM " + roomName); }
    public void sendChatMessage(String message) { client.sendMessage("CHAT " + message); }
    public void sendReady() { client.sendMessage("READY"); }
    public void leaveRoom() { client.sendMessage("LEAVE_ROOM"); }
    public void requestUndo() { client.sendMessage("UNDO_REQUEST"); }
    public void respondUndo(boolean accepted) { client.sendMessage("UNDO_RESPONSE " + accepted); }

    public void onBoardClicked(int r, int c) {
        if (!ui.isMyTurn()) return;

        if (selectedCapturedPiece != null) {
            client.sendMessage(String.format("PLACE %s %d %d", selectedCapturedPiece.name(), r, c));
            clearSelections();
            return;
        }

        boolean isClickOnValidMove = ui.isValidMove(r, c);

        if (selectedRow != -1) {
            if (isClickOnValidMove) {
                client.sendMessage(String.format("MOVE %d %d %d %d", selectedRow, selectedCol, r, c));
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
            client.sendMessage("GET_VALID_MOVES " + r + " " + c);
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

    // --- Server Messages ---
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(" ", 2);
            String command = parts[0];
            String payload = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "NICKNAME_OK":
                    ui.setTitle("십이장기 - " + client.getNickname());
                    ui.showLobby();
                    break;
                case "NICKNAME_TAKEN":
                    ui.showError("해당 닉네임은 이미 존재합니다.");
                    ui.showNicknamePrompt();
                    break;
                case "UPDATE_ROOMLIST":
                    ui.updateRoomList(payload.isEmpty() ? new String[0] : payload.split(","));
                    break;
                case "JOIN_SUCCESS":
                    ui.enterRoom(payload);
                    break;
                case "GOTO_LOBBY":
                    ui.showLobby();
                    ui.setTitle("십이장기 - " + client.getNickname());
                    ui.resetRoomUI();
                    break;
                case "CHAT":
                case "SYSTEM":
                    ui.appendChatMessage(payload);
                    break;
                case "PLAYER_READY":
                    ui.updatePlayerStatus(payload.split(" "));
                    break;
                case "GAME_START":
                    ui.handleGameStart();
                    break;
                case "UPDATE_STATE":
                    ui.updateGameState(payload);
                    break;
                case "VALID_MOVES":
                    ui.highlightValidMoves(payload);
                    break;
                case "GAME_OVER":
                    ui.handleGameOver(payload);
                    break;
                case "UNDO_REQUESTED":
                    ui.showUndoRequest(payload);
                    break;
                case "ERROR":
                    ui.showError(payload);
                    break;
                default:
                    ui.appendChatMessage(message);
                    break;
            }
        });
    }
}
