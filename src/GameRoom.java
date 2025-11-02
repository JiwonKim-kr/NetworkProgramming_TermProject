import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameRoom {

    private String title;
    private ClientHandler host;
    private ClientHandler guest;
    private ClientHandler player1; // P1
    private ClientHandler player2; // P2
    private List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameLogic gameLogic;
    private boolean hostReady = false;
    private boolean guestReady = false;
    private ClientHandler undoRequester = null;
    private Piece.Player kingInZonePlayer = null;

    public GameRoom(String title, ClientHandler host) {
        this.title = title;
        this.host = host;
        this.gameLogic = new GameLogic();
    }

    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) {
            if (command.equals("READY") && (player == host || player == guest)) {
                if (player == host) hostReady = !hostReady;
                else guestReady = !guestReady;
                broadcastSystem("PLAYER_READY " + (player == host ? "HOST" : "GUEST") + " " + (player == host ? hostReady : guestReady));
                checkAndStartGame();
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
                int fromR = Integer.parseInt(parts[1]);
                int fromC = Integer.parseInt(parts[2]);
                int toR = Integer.parseInt(parts[3]);
                int toC = Integer.parseInt(parts[4]);
                if (gameLogic.handleMove(playerRole, fromR, fromC, toR, toC)) {
                    if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
                        endGame(player, player.getNickname() + "님이 상대 왕을 잡아 승리했습니다!");
                    } else {
                        checkKingInOpponentZone();
                        broadcastState();
                    }
                } else {
                    player.sendMessage("ERROR: 유효하지 않은 움직임입니다.");
                }
                break;
            case "PLACE":
                try {
                    Piece originalPiece = Piece.valueOf(parts[1]);
                    Piece pieceToPlace = Piece.flipOwner(originalPiece);
                    
                    int placeR = Integer.parseInt(parts[2]);
                    int placeC = Integer.parseInt(parts[3]);

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
                break;
            case "UNDO_REQUEST":
                ClientHandler opponent = (player == player1) ? player2 : player1;
                if (opponent != null) {
                    opponent.sendMessage("UNDO_REQUESTED " + player.getNickname());
                    undoRequester = player;
                }
                break;
            case "UNDO_RESPONSE":
                if (undoRequester != null) {
                    boolean accepted = Boolean.parseBoolean(parts[1]);
                    if (accepted) {
                        gameLogic.undoLastMove();
                        broadcastSystem("SYSTEM: 수 무르기가 수락되었습니다.");
                        broadcastState();
                    } else {
                        undoRequester.sendMessage("SYSTEM: 상대방이 수 무르기를 거절했습니다.");
                    }
                    undoRequester = null;
                }
                break;
            case "GET_VALID_MOVES":
                int r = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                List<int[]> moves = gameLogic.getBoard().getValidMoves(r, c);
                String movesStr = moves.stream()
                                       .map(move -> move[0] + "," + move[1])
                                       .collect(Collectors.joining(";"));
                player.sendMessage("VALID_MOVES " + movesStr);
                break;
        }
    }

    private void checkAndStartGame() {
        if (host != null && guest != null && hostReady && guestReady) {
            if (new Random().nextBoolean()) { player1 = host; player2 = guest; } 
            else { player1 = guest; player2 = host; }
            
            player1.sendMessage("ASSIGN_ROLE P1");
            player2.sendMessage("ASSIGN_ROLE P2");

            gameLogic.startGame();
            broadcastSystem("GAME_START");
            broadcastState();
            Server.broadcastRoomList();
        }
    }

    private void checkKingInOpponentZone() {
        if (kingInZonePlayer != null && kingInZonePlayer == gameLogic.getCurrentPlayer()) {
            endGame(getClient(kingInZonePlayer), kingInZonePlayer.name() + "님이 왕을 상대 진영에서 한 턴 생존시켜 승리했습니다!");
            return;
        }

        GameBoard board = gameLogic.getBoard();
        int[] p1KingPos = board.findPiece(Piece.P1_KING);
        int[] p2KingPos = board.findPiece(Piece.P2_KING);

        if (p1KingPos != null && p1KingPos[0] == 0) {
            kingInZonePlayer = Piece.Player.P1;
        } else if (p2KingPos != null && p2KingPos[0] == 3) {
            kingInZonePlayer = Piece.Player.P2;
        } else {
            kingInZonePlayer = null;
        }
    }

    private void endGame(ClientHandler winner, String reason) {
        broadcastSystem("GAME_OVER " + reason);
        saveReplay();

        ClientHandler loser = (winner == player1) ? player2 : player1;
        this.host = winner;
        this.guest = loser;
        this.player1 = null;
        this.player2 = null;

        gameLogic = new GameLogic();
        hostReady = false;
        guestReady = false;
        kingInZonePlayer = null;

        broadcastSystem("SYSTEM: " + winner.getNickname() + "님이 새로운 호스트입니다.");
        Server.broadcastRoomList();
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
                boardStr.toString(),
                p1Captured,
                p2Captured,
                gameLogic.getCurrentPlayer().name());

        broadcastSystem("UPDATE_STATE " + statePayload);
    }

    public synchronized void addPlayer(ClientHandler player) {
        if (this.guest == null) {
            this.guest = player;
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 GUEST로 입장했습니다.");
        } else {
            spectators.add(player);
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 관전자로 입장했습니다.");
        }
        player.sendMessage("ROOM_INFO " + this.title);
        if (isGameInProgress()) {
            broadcastState();
        }
        Server.broadcastRoomList();
    }

    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        if (player == host || player == guest) {
            if (isGameInProgress()) {
                ClientHandler winner = (player == host) ? guest : host;
                if (winner != null) {
                    endGame(winner, "상대방이 퇴장하여 게임에서 승리했습니다.");
                }
            }
        }

        if (player == host) {
            host = guest;
            guest = spectators.isEmpty() ? null : spectators.remove(0);
            if (host != null) broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        } else if (player == guest) {
            guest = spectators.isEmpty() ? null : spectators.remove(0);
        } else {
            spectators.remove(player);
        }

        if (host == null) {
            Server.removeGameRoom(this.title);
        } else {
            broadcastSystem("SYSTEM: " + leavingNickname + "님이 퇴장했습니다.");
            Server.broadcastRoomList();
        }
    }

    private void saveReplay() {
        File replayDir = new File("replays");
        if (!replayDir.exists()) {
            replayDir.mkdirs();
        }

        String fileName = String.format("replays/replay_%s_%d.txt", title, System.currentTimeMillis());
        try (FileWriter writer = new FileWriter(fileName)) {
            for (String move : gameLogic.getMoveHistory()) {
                writer.write(move + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastSystem(String message) {
        getAllUsers().forEach(user -> user.sendMessage(message));
    }

    public void broadcastChat(String message) {
        getAllUsers().forEach(user -> user.sendMessage(message));
    }

    public synchronized List<ClientHandler> getAllUsers() {
        List<ClientHandler> allUsers = new ArrayList<>();
        if (host != null) allUsers.add(host);
        if (guest != null) allUsers.add(guest);
        allUsers.addAll(spectators);
        return allUsers;
    }

    public boolean isGameInProgress() {
        return gameLogic.getGameState() == GameLogic.GameState.IN_PROGRESS;
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

    public String getTitle() { return title; }
    public int getPlayerCount() { return getAllUsers().size(); }
}
