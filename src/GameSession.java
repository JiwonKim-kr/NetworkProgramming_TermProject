import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 한 판의 게임 진행을 담당하는 클래스입니다.
 * 플레이어의 준비 상태 관리, 게임 시작, 플레이어의 게임 관련 명령어 처리,
 * 게임 상태 브로드캐스팅, 게임 종료 및 리플레이 저장 등의 역할을 수행합니다.
 * 실제 게임 규칙의 적용은 GameLogic 클래스에 위임합니다.
 */
public class GameSession {

    private final GameRoom gameRoom; // 이 세션이 속한 게임방
    private final GameLogic gameLogic; // 게임의 순수 로직 담당
    private final ClientHandler host;
    private final ClientHandler guest;
    private ClientHandler player1; // 게임 내 P1 역할을 맡은 클라이언트
    private ClientHandler player2; // 게임 내 P2 역할을 맡은 클라이언트

    private boolean hostReady = false;
    private boolean guestReady = false;
    private ClientHandler undoRequester = null; // 수 무르기를 요청한 플레이어
    private Piece.Player kingInZonePlayer = null; // 상대 진영에 왕을 진입시킨 플레이어

    /**
     * GameSession 생성자입니다.
     * @param gameRoom 이 세션이 속한 게임방
     * @param host 방의 호스트
     * @param guest 방의 게스트
     */
    public GameSession(GameRoom gameRoom, ClientHandler host, ClientHandler guest) {
        this.gameRoom = gameRoom;
        this.host = host;
        this.guest = guest;
        this.gameLogic = new GameLogic();
    }

    /**
     * 플레이어로부터 받은 명령어를 파싱하고 적절한 핸들러 메서드를 호출합니다.
     * @param player 명령어를 보낸 플레이어
     * @param message 플레이어가 보낸 전체 메시지
     */
    public synchronized void processCommand(ClientHandler player, String message) {
        String[] parts = message.split(" ");
        String command = parts[0];

        // 1. 턴에 독립적인 명령어 (준비, 수 무르기)를 우선 처리합니다.
        switch (command) {
            case "READY":
                if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) {
                    handleReadyCommand(player);
                }
                return;
            case "UNDO_REQUEST":
                handleUndoRequest(player);
                return;
            case "UNDO_RESPONSE":
                handleUndoResponse(player, parts);
                return;
        }

        // 2. 게임 진행 중이 아닐 경우, 아래의 게임 관련 명령어들은 처리하지 않습니다.
        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) {
            return;
        }

        // 3. 턴에 종속적인 명령어 처리 전, 현재 턴의 플레이어가 맞는지 검사합니다.
        Piece.Player playerRole = getPlayerRole(player);
        if (playerRole == null || playerRole != gameLogic.getCurrentPlayer()) {
            player.sendMessage("ERROR: 지금은 당신의 턴이 아닙니다.");
            return;
        }

        // 4. 현재 턴의 플레이어가 보낸 게임 관련 명령어를 처리합니다.
        switch (command) {
            case "MOVE":
                handleMoveCommand(player, parts);
                break;
            case "PLACE":
                handlePlaceCommand(player, playerRole, parts);
                break;
            case "GET_VALID_MOVES":
                handleGetValidMoves(player, parts);
                break;
        }
    }

    /**
     * 플레이어의 '준비' 상태를 처리합니다.
     * 양쪽 모두 준비가 되면 게임을 시작합니다.
     * @param player 준비/준비해제한 플레이어
     */
    private void handleReadyCommand(ClientHandler player) {
        if (player == host) hostReady = !hostReady;
        else if (player == guest) guestReady = !guestReady;

        gameRoom.broadcastSystem("PLAYER_READY " + (player == host ? "HOST" : "GUEST") + " " + (player == host ? hostReady : guestReady));
        
        if (host != null && guest != null && hostReady && guestReady) {
            startGame();
        }
    }

    /**
     * 게임을 시작합니다. P1, P2 역할을 랜덤으로 배정하고,
     * 모든 클라이언트에게 게임 시작을 알립니다.
     */
    private void startGame() {
        // 랜덤하게 P1, P2 역할 배정
        if (new Random().nextBoolean()) { player1 = host; player2 = guest; } 
        else { player1 = guest; player2 = host; }
        
        player1.sendMessage("ASSIGN_ROLE P1");
        player2.sendMessage("ASSIGN_ROLE P2");

        gameLogic.startGame();
        gameRoom.broadcastSystem("GAME_START");
        broadcastState(); // 초기 게임 상태 전송
        Server.broadcastRoomList(); // 로비에 '게임중' 상태 업데이트
    }

    /**
     * 기물 이동 명령을 처리합니다.
     * @param player 명령을 보낸 플레이어
     * @param parts 명령어와 파라미터 배열
     */
    private void handleMoveCommand(ClientHandler player, String[] parts) {
        try {
            int fromR = Integer.parseInt(parts[1]);
            int fromC = Integer.parseInt(parts[2]);
            int toR = Integer.parseInt(parts[3]);
            int toC = Integer.parseInt(parts[4]);

            if (gameLogic.handleMove(getPlayerRole(player), fromR, fromC, toR, toC)) {
                if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
                    // 왕을 잡아 게임이 끝난 경우
                    naturalEndGame(player, player.getNickname() + "님이 상대 왕을 잡아 승리했습니다!");
                } else {
                    checkKingInOpponentZone(); // 왕의 입궁(승리 조건) 확인
                    broadcastState();
                }
            } else {
                player.sendMessage("ERROR: 유효하지 않은 움직임입니다.");
            }
        } catch (Exception e) {
            player.sendMessage("ERROR: 잘못된 이동 명령입니다.");
        }
    }

    /**
     * 잡은 기물을 내려놓는 명령을 처리합니다.
     * @param player 명령을 보낸 플레이어
     * @param playerRole 명령을 보낸 플레이어의 역할 (P1/P2)
     * @param parts 명령어와 파라미터 배열
     */
    private void handlePlaceCommand(ClientHandler player, Piece.Player playerRole, String[] parts) {
        try {
            Piece pieceToPlace = Piece.valueOf(parts[1]);
            int placeR = Integer.parseInt(parts[2]);
            int placeC = Integer.parseInt(parts[3]);

            // 해당 기물을 실제로 가지고 있는지 확인
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

    /**
     * 수 무르기 요청을 처리합니다.
     * @param player 요청한 플레이어
     */
    private void handleUndoRequest(ClientHandler player) {
        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) return;

        // 자신의 턴에는 수 무르기 요청 불가
        if (getPlayerRole(player) == gameLogic.getCurrentPlayer()) {
            player.sendMessage("ERROR: 상대방의 턴에만 수 무르기를 요청할 수 있습니다.");
            return;
        }
        ClientHandler opponent = (player == player1) ? player2 : player1;
        if (opponent != null) {
            opponent.sendMessage("UNDO_REQUESTED " + player.getNickname());
            undoRequester = player;
        }
    }

    /**
     * 수 무르기 요청에 대한 응답을 처리합니다.
     * @param player 응답한 플레이어
     * @param parts 명령어와 파라미터 배열
     */
    private void handleUndoResponse(ClientHandler player, String[] parts) {
        if (gameLogic.getGameState() != GameLogic.GameState.IN_PROGRESS) return;
        
        // 현재 턴인 플레이어만 응답 가능
        if (getPlayerRole(player) != gameLogic.getCurrentPlayer()) {
             player.sendMessage("ERROR: 수 무르기 요청에 응답할 수 없습니다.");
            return;
        }

        if (undoRequester != null) {
            boolean accepted = Boolean.parseBoolean(parts[1]);
            if (accepted) {
                gameLogic.undoLastMove();
                gameRoom.broadcastSystem("SYSTEM: 수 무르기가 수락되었습니다.");
                broadcastState();
            } else {
                undoRequester.sendMessage("SYSTEM: 상대방이 수 무르기를 거절했습니다.");
            }
            undoRequester = null; // 요청 처리 완료
        }
    }

    /**
     * 특정 기물의 유효 이동 경로를 클라이언트에 전송합니다.
     * @param player 요청한 플레이어
     * @param parts 명령어와 파라미터 배열
     */
    private void handleGetValidMoves(ClientHandler player, String[] parts) {
        try {
            int r = Integer.parseInt(parts[1]);
            int c = Integer.parseInt(parts[2]);
            List<int[]> moves = gameLogic.getBoard().getValidMoves(r, c);
            String movesStr = moves.stream()
                                   .map(move -> move[0] + "," + move[1])
                                   .collect(Collectors.joining(";"));
            player.sendMessage("VALID_MOVES " + movesStr);
        } catch (Exception e) {
            player.sendMessage("ERROR: 잘못된 좌표입니다.");
        }
    }

    /**
     * 왕이 상대 진영에 들어가 한 턴을 버텼는지 확인하는 승리 조건을 체크합니다.
     */
    private void checkKingInOpponentZone() {
        // 이전 턴에 왕이 상대 진영에 들어갔고, 현재 턴이 다시 그 플레이어에게 돌아왔다면 승리
        if (kingInZonePlayer != null && kingInZonePlayer == gameLogic.getCurrentPlayer()) {
            naturalEndGame(getClient(kingInZonePlayer), kingInZonePlayer.name() + "님이 왕을 상대 진영에서 한 턴 생존시켜 승리했습니다!");
            return;
        }

        GameBoard board = gameLogic.getBoard();
        int[] p1KingPos = board.findPiece(Piece.P1_KING);
        int[] p2KingPos = board.findPiece(Piece.P2_KING);

        // 현재 턴에 왕이 상대 진영에 들어갔는지 기록
        if (p1KingPos != null && p1KingPos[0] == 0) kingInZonePlayer = Piece.Player.P1;
        else if (p2KingPos != null && p2KingPos[0] == 3) kingInZonePlayer = Piece.Player.P2;
        else kingInZonePlayer = null;
    }

    /**
     * 플레이어의 퇴장 등으로 게임이 비정상적으로 종료될 때 호출됩니다.
     * @param reason 종료 사유
     */
    public void abortGame(String reason) {
        gameRoom.broadcastSystem("GAME_OVER " + reason);
        saveReplay();
    }
    public void abortGame(String reason, ClientHandler leaver) {
        gameRoom.broadcastSystemExcept(leaver, "GAME_OVER " + reason);
        saveReplay();
    }

    /**
     * 게임이 정상적으로(승패가 결정되어) 종료될 때 호출됩니다.
     * @param winner 승리한 플레이어
     * @param reason 종료 사유
     */
    private void naturalEndGame(ClientHandler winner, String reason) {
        gameRoom.broadcastSystem("GAME_OVER " + reason);
        saveReplay();
        gameRoom.onSessionFinished(winner); // GameRoom에 게임 종료 알림
    }

    /**
     * 현재 게임 상태(보드, 잡은 말, 턴 등)를 모든 플레이어에게 브로드캐스트합니다.
     */
    public void broadcastState() {
        GameBoard board = gameLogic.getBoard();
        // 보드 상태 직렬화
        StringBuilder boardStr = new StringBuilder();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                Piece piece = board.getPieceAt(r, c);
                if (piece != null) {
                    boardStr.append(String.format("%s,%d,%d;", piece.name(), r, c));
                }
            }
        }

        // 잡은 말 목록 직렬화
        String p1Captured = gameLogic.getBoard().getP1Captured().stream().map(Enum::name).collect(Collectors.joining(","));
        String p2Captured = gameLogic.getBoard().getP2Captured().stream().map(Enum::name).collect(Collectors.joining(","));
        // 기보 직렬화
        String moveHistoryStr = String.join(" ", gameLogic.getMoveHistory());

        // 모든 정보를 하나의 문자열 페이로드로 합침
        String statePayload = String.format("%s|%s|%s|%s#%s",
                boardStr.toString(), p1Captured, p2Captured, gameLogic.getCurrentPlayer().name(), moveHistoryStr);

        gameRoom.broadcastSystem("UPDATE_STATE " + statePayload);
    }

    /**
     * 게임이 종료되면 현재까지의 기보를 파일로 저장합니다.
     */
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

    /**
     * ClientHandler 객체로부터 게임 내 역할(P1/P2)을 반환합니다.
     * @param player 확인할 클라이언트 핸들러
     * @return P1 또는 P2, 해당 없으면 null
     */
    private Piece.Player getPlayerRole(ClientHandler player) {
        if (player == player1) return Piece.Player.P1;
        if (player == player2) return Piece.Player.P2;
        return null;
    }

    /**
     * 게임 내 역할(P1/P2)로부터 해당하는 ClientHandler 객체를 반환합니다.
     * @param playerRole P1 또는 P2
     * @return 해당 역할을 맡은 클라이언트 핸들러
     */
    private ClientHandler getClient(Piece.Player playerRole) {
        if (playerRole == Piece.Player.P1) return player1;
        if (playerRole == Piece.Player.P2) return player2;
        return null;
    }
}
