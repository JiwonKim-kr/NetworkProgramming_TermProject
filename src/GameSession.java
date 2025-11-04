import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameSession {

    private final GameRoom gameRoom;
    private final GameLogic gameLogic;
    private final ClientHandler host;
    private final ClientHandler guest;
    private ClientHandler player1;
    private ClientHandler player2;

    private boolean hostReady = false;
    private boolean guestReady = false;
    private ClientHandler undoRequester = null;
    private Piece.Player kingInZonePlayer = null;

    public GameSession(GameRoom gameRoom, ClientHandler host, ClientHandler guest) {
        this.gameRoom = gameRoom;
        this.host = host;
        this.guest = guest;
        this.gameLogic = new GameLogic();
    }

    public synchronized void processCommand(ClientHandler player, String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) {
            if (command.equals("READY")) {
                handleReadyCommand(player);
            }
            return;
        }

        Piece.Player playerRole = getPlayerRole(player);
        if (playerRole == null || playerRole != gameLogic.getCurrentPlayer()) {
            player.sendMessage("ERROR: 지금은 당신의 턴이 아닙니다.");
            return;
        }

        switch (command) {
            case "MOVE":
                handleMoveCommand(player, parts);
                break;
            case "PLACE":
                handlePlaceCommand(player, playerRole, parts);
                break;
            case "UNDO_REQUEST":
                handleUndoRequest(player);
                break;
            case "UNDO_RESPONSE":
                handleUndoResponse(parts);
                break;
            case "GET_VALID_MOVES":
                handleGetValidMoves(player, parts);
                break;
        }
    }

    private void handleReadyCommand(ClientHandler player) {
        if (player == host) hostReady = !hostReady;
        else if (player == guest) guestReady = !guestReady;

        gameRoom.broadcastSystem("PLAYER_READY " + (player == host ? "HOST" : "GUEST") + " " + (player == host ? hostReady : guestReady));
        
        if (host != null && guest != null && hostReady && guestReady) {
            startGame();
        }
    }

    private void startGame() {
        if (new Random().nextBoolean()) { player1 = host; player2 = guest; } 
        else { player1 = guest; player2 = host; }
        
        player1.sendMessage("ASSIGN_ROLE P1");
        player2.sendMessage("ASSIGN_ROLE P2");

        gameLogic.startGame();
        gameRoom.broadcastSystem("GAME_START");
        broadcastState();
        Server.broadcastRoomList();
    }

    private void handleMoveCommand(ClientHandler player, String[] parts) {
        int fromR = Integer.parseInt(parts[1]);
        int fromC = Integer.parseInt(parts[2]);
        int toR = Integer.parseInt(parts[3]);
        int toC = Integer.parseInt(parts[4]);

        if (gameLogic.handleMove(getPlayerRole(player), fromR, fromC, toR, toC)) {
            if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
                endGame(player, player.getNickname() + "님이 상대 왕을 잡아 승리했습니다!");
            } else {
                checkKingInOpponentZone();
                broadcastState();
            }
        } else {
            player.sendMessage("ERROR: 유효하지 않은 움직임입니다.");
        }
    }

    private void handlePlaceCommand(ClientHandler player, Piece.Player playerRole, String[] parts) {
        try {
            // 클라이언트가 보낸 말 이름은 이미 "내 소유의 말" (예: P1_PAWN)
            Piece pieceToPlace = Piece.valueOf(parts[1]);
            
            int placeR = Integer.parseInt(parts[2]);
            int placeC = Integer.parseInt(parts[3]);

            // 서버에서 해당 말이 플레이어의 포로 목록에 있는지 직접 확인
            List<Piece> capturedList = (playerRole == Piece.Player.P1) ? gameLogic.getBoard().getP1Captured() : gameLogic.getBoard().getP2Captured();
            if (capturedList.contains(pieceToPlace)) {
                if (gameLogic.handlePlace(playerRole, pieceToPlace, placeR, placeC)) {
                    broadcastState();
                } else {
                    player.sendMessage("ERROR: 해당 위치에 말을 놓을 수 없습니다.");
                }
            } else {
                player.sendMessage("ERROR: 가지고 있지 않은 말입니다.");
            }
        } catch (Exception e) {
            player.sendMessage("ERROR: 잘못된 명령입니다.");
        }
    }

    private void handleUndoRequest(ClientHandler player) {
        ClientHandler opponent = (player == player1) ? player2 : player1;
        if (opponent != null) {
            opponent.sendMessage("UNDO_REQUESTED " + player.getNickname());
            undoRequester = player;
        }
    }

    private void handleUndoResponse(String[] parts) {
        if (undoRequester != null) {
            boolean accepted = Boolean.parseBoolean(parts[1]);
            if (accepted) {
                gameLogic.undoLastMove();
                gameRoom.broadcastSystem("SYSTEM: 수 무르기가 수락되었습니다.");
                broadcastState();
            } else {
                undoRequester.sendMessage("SYSTEM: 상대방이 수 무르기를 거절했습니다.");
            }
            undoRequester = null;
        }
    }

    private void handleGetValidMoves(ClientHandler player, String[] parts) {
        int r = Integer.parseInt(parts[1]);
        int c = Integer.parseInt(parts[2]);
        List<int[]> moves = gameLogic.getBoard().getValidMoves(r, c);
        String movesStr = moves.stream()
                               .map(move -> move[0] + "," + move[1])
                               .collect(Collectors.joining(";"));
        player.sendMessage("VALID_MOVES " + movesStr);
    }

    private void checkKingInOpponentZone() {
        if (kingInZonePlayer != null && kingInZonePlayer == gameLogic.getCurrentPlayer()) {
            endGame(getClient(kingInZonePlayer), kingInZonePlayer.name() + "님이 왕을 상대 진영에서 한 턴 생존시켜 승리했습니다!");
            return;
        }

        GameBoard board = gameLogic.getBoard();
        int[] p1KingPos = board.findPiece(Piece.P1_KING);
        int[] p2KingPos = board.findPiece(Piece.P2_KING);

        if (p1KingPos != null && p1KingPos[0] == 0) kingInZonePlayer = Piece.Player.P1;
        else if (p2KingPos != null && p2KingPos[0] == 3) kingInZonePlayer = Piece.Player.P2;
        else kingInZonePlayer = null;
    }

    public void endGame(ClientHandler winner, String reason) {
        gameRoom.broadcastSystem("GAME_OVER " + reason);
        saveReplay();
        gameRoom.onSessionFinished(winner);
    }

    public void broadcastState() {
        GameBoard board = gameLogic.getBoard();
        StringBuilder boardStr = new StringBuilder();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                Piece piece = board.getPieceAt(r, c);
                if (piece != null) {
                    boardStr.append(String.format("%s,%d,%d;", piece.name(), r, c));
                }
            }
        }

        String p1Captured = gameLogic.getBoard().getP1Captured().stream().map(Enum::name).collect(Collectors.joining(","));
        String p2Captured = gameLogic.getBoard().getP2Captured().stream().map(Enum::name).collect(Collectors.joining(","));

        String statePayload = String.format("%s|%s|%s|%s",
                boardStr.toString(), p1Captured, p2Captured, gameLogic.getCurrentPlayer().name());

        gameRoom.broadcastSystem("UPDATE_STATE " + statePayload);
    }

    private void saveReplay() {
        File replayDir = new File("replays");
        if (!replayDir.exists()) replayDir.mkdirs();

        String fileName = String.format("replays/replay_%s_%d.txt", gameRoom.getTitle(), System.currentTimeMillis());
        try (FileWriter writer = new FileWriter(fileName)) {
            for (String move : gameLogic.getMoveHistory()) {
                writer.write(move + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GameLogic.GameState getGameState() {
        return gameLogic.getGameState();
    }

    private Piece.Player getPlayerRole(ClientHandler player) {
        if (player == player1) return Piece.Player.P1;
        if (player == player2) return Piece.Player.P2;
        return null;
    }

    private ClientHandler getClient(Piece.Player playerRole) {
        if (playerRole == Piece.Player.P1) return player1;
        if (playerRole == Piece.Player.P2) return player2;
        return null;
    }
}
