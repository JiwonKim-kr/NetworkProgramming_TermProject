import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class GameSession {

    private final GameRoom gameRoom;
    private final GameLogic gameLogic;
    private final PlayerConnection host;
    private final PlayerConnection guest;
    private PlayerConnection player1;
    private PlayerConnection player2;

    private boolean hostReady = false;
    private boolean guestReady = false;
    private PlayerConnection undoRequester = null;
    private Piece.Player kingInZonePlayer = null;

    // --- Command Pattern ---
    private final Map<String, BiFunction<PlayerConnection, String[], Command>> commandFactories = new HashMap<>();

    public GameSession(GameRoom gameRoom, PlayerConnection host, PlayerConnection guest) {
        this.gameRoom = gameRoom;
        this.host = host;
        this.guest = guest;
        this.gameLogic = new GameLogic();
        initializeCommands();
    }

    private void initializeCommands() {
        // 팩토리를 사용하여 각 명령어에 대한 Command 객체 생성을 위임
        commandFactories.put(Protocol.READY, ReadyCommand::new);
        commandFactories.put(Protocol.MOVE, MoveCommand::new);
        commandFactories.put(Protocol.PLACE, PlaceCommand::new);
        commandFactories.put(Protocol.GET_VALID_MOVES, GetValidMovesCommand::new);
        commandFactories.put(Protocol.UNDO_REQUEST, UndoRequestCommand::new);
        commandFactories.put(Protocol.UNDO_RESPONSE, UndoResponseCommand::new);
    }

    public synchronized void processCommand(PlayerConnection player, String message) {
        String[] parts = message.split(" ");
        String commandKey = parts[0];

        BiFunction<PlayerConnection, String[], Command> factory = commandFactories.get(commandKey);
        if (factory != null) {
            Command command = factory.apply(player, parts);
            command.execute();
        }
    }

    // --- Command Inner Classes ---

    private class ReadyCommand implements Command {
        private final PlayerConnection player;
        public ReadyCommand(PlayerConnection player, String[] parts) { this.player = player; }
        @Override
        public void execute() {
            if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) {
                if (player == host) hostReady = !hostReady;
                else if (player == guest) guestReady = !guestReady;

                gameRoom.broadcastSystem(Protocol.PLAYER_READY + " " + (player == host ? Protocol.HOST : Protocol.GUEST) + " " + (player == host ? hostReady : guestReady));
                
                if (host != null && guest != null && hostReady && guestReady) {
                    startGame();
                }
            }
        }
    }

    private class MoveCommand implements Command {
        private final PlayerConnection player;
        private final String[] parts;
        public MoveCommand(PlayerConnection player, String[] parts) { this.player = player; this.parts = parts; }
        @Override
        public void execute() {
            if (!isPlayerTurn(player)) return;
            try {
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
                    player.sendMessage(Protocol.ERROR + " 유효하지 않은 움직임입니다.");
                }
            } catch (Exception e) {
                player.sendMessage(Protocol.ERROR + " 잘못된 이동 명령입니다.");
            }
        }
    }

    private class PlaceCommand implements Command {
        private final PlayerConnection player;
        private final String[] parts;
        public PlaceCommand(PlayerConnection player, String[] parts) { this.player = player; this.parts = parts; }
        @Override
        public void execute() {
            if (!isPlayerTurn(player)) return;
            try {
                Piece pieceToPlace = Piece.valueOf(parts[1]);
                int placeR = Integer.parseInt(parts[2]);
                int placeC = Integer.parseInt(parts[3]);
                Piece.Player playerRole = getPlayerRole(player);

                List<Piece> capturedList = (playerRole == Piece.Player.P1) ? gameLogic.getBoard().getP1Captured() : gameLogic.getBoard().getP2Captured();
                if (capturedList.contains(pieceToPlace)) {
                    if (gameLogic.handlePlace(playerRole, pieceToPlace, placeR, placeC)) {
                        broadcastState();
                    } else {
                        player.sendMessage(Protocol.ERROR + " 해당 위치에 말을 놓을 수 없습니다.");
                    }
                } else {
                    player.sendMessage(Protocol.ERROR + " 가지고 있지 않은 말입니다.");
                }
            } catch (Exception e) {
                player.sendMessage(Protocol.ERROR + " 잘못된 명령입니다.");
            }
        }
    }

    private class GetValidMovesCommand implements Command {
        private final PlayerConnection player;
        private final String[] parts;
        public GetValidMovesCommand(PlayerConnection player, String[] parts) { this.player = player; this.parts = parts; }
        @Override
        public void execute() {
            if (!isPlayerTurn(player)) return;
            try {
                int r = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                List<int[]> moves = gameLogic.getBoard().getValidMoves(r, c);
                String movesStr = moves.stream()
                                       .map(move -> move[0] + "," + move[1])
                                       .collect(Collectors.joining(";"));
                player.sendMessage(Protocol.VALID_MOVES + " " + movesStr);
            } catch (Exception e) {
                player.sendMessage(Protocol.ERROR + " 잘못된 좌표입니다.");
            }
        }
    }

    private class UndoRequestCommand implements Command {
        private final PlayerConnection player;
        public UndoRequestCommand(PlayerConnection player, String[] parts) { this.player = player; }
        @Override
        public void execute() {
            if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) return;
            if (getPlayerRole(player) == gameLogic.getCurrentPlayer()) {
                player.sendMessage(Protocol.ERROR + " 상대방의 턴에만 수 무르기를 요청할 수 있습니다.");
                return;
            }
            PlayerConnection opponent = (player == player1) ? player2 : player1;
            if (opponent != null) {
                opponent.sendMessage(Protocol.UNDO_REQUESTED + " " + player.getNickname());
                undoRequester = player;
            }
        }
    }

    private class UndoResponseCommand implements Command {
        private final PlayerConnection player;
        private final String[] parts;
        public UndoResponseCommand(PlayerConnection player, String[] parts) { this.player = player; this.parts = parts; }
        @Override
        public void execute() {
            if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) return;
            if (getPlayerRole(player) != gameLogic.getCurrentPlayer()) {
                 player.sendMessage(Protocol.ERROR + " 수 무르기 요청에 응답할 수 없습니다.");
                return;
            }
            if (undoRequester != null) {
                boolean accepted = Boolean.parseBoolean(parts[1]);
                if (accepted) {
                    gameLogic.undoLastMove();
                    gameRoom.broadcastSystem(Protocol.SYSTEM + " 수 무르기가 수락되었습니다.");
                    broadcastState();
                } else {
                    undoRequester.sendMessage(Protocol.SYSTEM + " 상대방이 수 무르기를 거절했습니다.");
                }
                undoRequester = null;
            }
        }
    }

    // --- Helper & Core Logic Methods ---

    private boolean isPlayerTurn(PlayerConnection player) {
        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) return false;
        Piece.Player playerRole = getPlayerRole(player);
        if (playerRole == null || playerRole != gameLogic.getCurrentPlayer()) {
            player.sendMessage(Protocol.ERROR + " 지금은 당신의 턴이 아닙니다.");
            return false;
        }
        return true;
    }

    private void startGame() {
    	if (new Random().nextBoolean()) { player1 = host; player2 = guest; } 
        else { player1 = guest; player2 = host; }
        
    	player1.sendMessage(Protocol.ASSIGN_ROLE + " " + Protocol.P1);
        player2.sendMessage(Protocol.ASSIGN_ROLE + " " + Protocol.P2);

        gameLogic.startGame();
        gameRoom.broadcastSystem(Protocol.GAME_START);
        broadcastState();
        Server.broadcastRoomList();
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

    public void endGame(PlayerConnection winner, String reason) {
        gameRoom.broadcastSystem(Protocol.GAME_OVER + " " + reason);
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

        gameRoom.broadcastSystem(Protocol.UPDATE_STATE + " " + statePayload);
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

    private Piece.Player getPlayerRole(PlayerConnection player) {
        if (player == player1) return Piece.Player.P1;
        if (player == player2) return Piece.Player.P2;
        return null;
    }

    private PlayerConnection getClient(Piece.Player playerRole) {
        if (playerRole == Piece.Player.P1) return player1;
        if (playerRole == Piece.Player.P2) return player2;
        return null;
    }
}
