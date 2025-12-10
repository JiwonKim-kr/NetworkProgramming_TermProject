import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 순수한 게임 규칙과 상태를 관리하는 클래스입니다.
 * 네트워크나 UI에 대한 의존성 없이, 게임의 핵심 로직(턴 관리, 승패 판정, 수 기록 등)을 담당합니다.
 * GameSession에 의해 사용됩니다.
 */
public class GameLogic {

    private GameBoard board;
    private Piece.Player currentPlayer;
    private GameState gameState;
    private Piece.Player winner = null;
    private final List<String> moveHistory = new ArrayList<>(); // 기보 저장을 위한 리스트
    private final Stack<GameBoard> boardHistory = new Stack<>(); // '수 무르기'를 위한 보드 상태 스택

    // 기보(notation) 파싱을 위한 정규식
    // 예: "Ka1xb2" (왕이 a1에서 b2로 이동하며 잡음)
    private static final Pattern MOVE_PATTERN = Pattern.compile("([KGERP])([a-c][1-4])x?([a-c][1-4])");
    // 예: "P@b3" (잡은 '자'를 b3에 놓음)
    private static final Pattern PLACE_PATTERN = Pattern.compile("([KGERP])@([a-c][1-4])");

    /**
     * 게임의 현재 상태를 나타내는 열거형입니다.
     */
    public enum GameState {
        WAITING_FOR_PLAYERS, // 플레이어 대기 중
        IN_PROGRESS,         // 게임 진행 중
        GAME_OVER            // 게임 종료
    }

    /**
     * GameLogic 생성자입니다.
     * 새로운 게임 보드를 생성하고 초기 상태를 설정합니다.
     */
    public GameLogic() {
        this.board = new GameBoard();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
    }

    /**
     * 새로운 게임을 시작합니다.
     * 보드를 초기화하고, 첫 턴을 P1으로 설정하며, 게임 상태를 '진행 중'으로 변경합니다.
     */
    public void startGame() {
        board.setupInitialBoard();
        currentPlayer = Piece.Player.P1;
        gameState = GameState.IN_PROGRESS;
        winner = null;
        moveHistory.clear();
        boardHistory.clear();
    }

    /**
     * 기물 이동을 처리합니다.
     * @param player 이동을 시도하는 플레이어
     * @param fromR  시작 행
     * @param fromC  시작 열
     * @param toR    목표 행
     * @param toC    목표 열
     * @return 이동 성공 여부
     */
    public boolean handleMove(Piece.Player player, int fromR, int fromC, int toR, int toC) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        Piece movingPiece = board.getPieceAt(fromR, fromC);
        if (movingPiece == null || movingPiece.getOwner() != player) {
            return false;
        }

        // 기보 기록을 위해 이동 정보를 변환
        char pieceChar = getPieceChar(movingPiece);
        String fromAlg = toAlgebraic(fromR, fromC);
        String toAlg = toAlgebraic(toR, toC);
        boolean isCapture = board.getPieceAt(toR, toC) != null;
        String moveNotation = String.format("%c%s%s%s", pieceChar, fromAlg, isCapture ? "x" : "", toAlg);

        boardHistory.push(this.board.clone()); // 수 무르기를 위해 현재 보드 상태 저장

        if (board.movePiece(fromR, fromC, toR, toC)) {
            moveHistory.add(moveNotation);
            if (checkGameOver()) {
                gameState = GameState.GAME_OVER;
                this.winner = player;
            } else {
                switchTurn();
            }
            return true;
        }
        
        boardHistory.pop(); // 이동 실패 시 저장했던 보드 상태 제거
        return false;
    }

    /**
     * 잡은 기물을 보드에 내려놓는 것을 처리합니다.
     * @param player       기물을 놓는 플레이어
     * @param pieceToPlace 놓을 기물
     * @param row          놓을 위치의 행
     * @param col          놓을 위치의 열
     * @return 배치 성공 여부
     */
    public boolean handlePlace(Piece.Player player, Piece pieceToPlace, int row, int col) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        char pieceChar = getPieceChar(pieceToPlace);
        String toAlg = toAlgebraic(row, col);
        String placeNotation = String.format("%c@%s", pieceChar, toAlg);

        boardHistory.push(this.board.clone());

        if (board.placeCapturedPiece(player, pieceToPlace, row, col)) {
            moveHistory.add(placeNotation);
            switchTurn();
            return true;
        }
        
        boardHistory.pop();
        return false;
    }

    /**
     * 기보 문자열을 해석하여 해당하는 수를 실행합니다. (리플레이 기능에서 사용)
     * @param notation 기보 문자열 (예: "Ka1b1", "P@c2")
     * @return 실행 성공 여부
     */
    public boolean executeMove(String notation) {
        Matcher moveMatcher = MOVE_PATTERN.matcher(notation);
        if (moveMatcher.matches()) {
            int[] from = fromAlgebraic(moveMatcher.group(2));
            int[] to = fromAlgebraic(moveMatcher.group(3));
            return handleMove(currentPlayer, from[0], from[1], to[0], to[1]);
        }

        Matcher placeMatcher = PLACE_PATTERN.matcher(notation);
        if (placeMatcher.matches()) {
            Piece pieceToPlace = getPieceFromChar(placeMatcher.group(1).charAt(0), currentPlayer);
            int[] to = fromAlgebraic(placeMatcher.group(2));
            return handlePlace(currentPlayer, pieceToPlace, to[0], to[1]);
        }
        return false;
    }

    // --- 기보 표기법(Algebraic Notation) 변환 헬퍼 ---
    private int[] fromAlgebraic(String alg) {
        int c = alg.charAt(0) - 'a'; // 'a' -> 0, 'b' -> 1, 'c' -> 2
        int r = 4 - (alg.charAt(1) - '0'); // '1' -> 3, '2' -> 2, ...
        return new int[]{r, c};
    }

    private String toAlgebraic(int r, int c) {
        char file = (char) ('a' + c);
        int rank = 4 - r;
        return "" + file + rank;
    }

    private char getPieceChar(Piece piece) {
        String name = piece.name();
        if (name.contains("KING")) return 'K';
        if (name.contains("GENERAL")) return 'G';
        if (name.contains("ELEPHANT")) return 'E';
        if (name.contains("PAWN")) return 'P';
        if (name.contains("PRINCE")) return 'R'; // '후'는 Prince로 표현
        return '?';
    }

    private Piece getPieceFromChar(char pieceChar, Piece.Player owner) {
        return switch (pieceChar) {
            case 'K' -> owner == Piece.Player.P1 ? Piece.P1_KING : Piece.P2_KING;
            case 'G' -> owner == Piece.Player.P1 ? Piece.P1_GENERAL : Piece.P2_GENERAL;
            case 'E' -> owner == Piece.Player.P1 ? Piece.P1_ELEPHANT : Piece.P2_ELEPHANT;
            case 'P' -> owner == Piece.Player.P1 ? Piece.P1_PAWN : Piece.P2_PAWN;
            case 'R' -> owner == Piece.Player.P1 ? Piece.P1_PRINCE : Piece.P2_PRINCE;
            default -> null;
        };
    }
    // ---

    /**
     * 게임 종료 조건을 확인합니다. (어느 한쪽의 왕이 잡혔는지)
     * @return 게임이 종료되었으면 true
     */
    private boolean checkGameOver() {
        boolean p1KingExists = board.findPiece(Piece.P1_KING) != null;
        boolean p2KingExists = board.findPiece(Piece.P2_KING) != null;
        return !p1KingExists || !p2KingExists;
    }

    /**
     * 마지막으로 둔 수를 무릅니다.
     * 스택에 저장된 이전 보드 상태를 복원합니다.
     */
    public void undoLastMove() {
        if (!boardHistory.isEmpty()) {
            this.board = boardHistory.pop();
            if (!moveHistory.isEmpty()) {
                moveHistory.remove(moveHistory.size() - 1);
            }
            switchTurn(); // 턴도 이전 상태로 되돌림
        }
    }

    /**
     * 턴을 상대방에게 넘깁니다.
     */
    private void switchTurn() {
        currentPlayer = (currentPlayer == Piece.Player.P1) ? Piece.Player.P2 : Piece.Player.P1;
    }

    // --- Getter 메서드 ---
    public GameBoard getBoard() { return board; }
    public Piece.Player getCurrentPlayer() { return currentPlayer; }
    public GameState getGameState() { return gameState; }
    public Piece.Player getWinner() { return winner; }
    public List<String> getMoveHistory() { return moveHistory; }
}
