import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class GameLogic {

    private GameBoard board;
    private Piece.Player currentPlayer;
    private GameState gameState;
    private Piece.Player winner = null;
    private final List<String> moveHistory = new ArrayList<>();
    private final Stack<GameBoard> boardHistory = new Stack<>();

    public enum GameState {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        GAME_OVER
    }

    public GameLogic() {
        this.board = new GameBoard();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
    }

    public void startGame() {
        board.setupInitialBoard();
        currentPlayer = Piece.Player.P1;
        gameState = GameState.IN_PROGRESS;
        winner = null;
        moveHistory.clear();
        boardHistory.clear();
    }

    public boolean handleMove(Piece.Player player, int fromR, int fromC, int toR, int toC) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        Piece movingPiece = board.getPieceAt(fromR, fromC);
        if (movingPiece == null || movingPiece.getOwner() != player) {
            return false;
        }

        // --- 기보 기록 로직 ---
        char pieceChar = getPieceChar(movingPiece);
        String fromAlg = toAlgebraic(fromR, fromC);
        String toAlg = toAlgebraic(toR, toC);
        boolean isCapture = board.getPieceAt(toR, toC) != null;
        String moveNotation = String.format("%c%s%s%s", pieceChar, fromAlg, isCapture ? "x" : "", toAlg);
        // --------------------

        boardHistory.push(this.board.clone());

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
        
        boardHistory.pop();
        return false;
    }

    public boolean handlePlace(Piece.Player player, Piece pieceToPlace, int row, int col) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        // --- 기보 기록 로직 ---
        char pieceChar = getPieceChar(pieceToPlace);
        String toAlg = toAlgebraic(row, col);
        String placeNotation = String.format("%c@%s", pieceChar, toAlg);
        // --------------------

        boardHistory.push(this.board.clone());

        if (board.placeCapturedPiece(player, pieceToPlace, row, col)) {
            moveHistory.add(placeNotation);
            switchTurn();
            return true;
        }
        
        boardHistory.pop();
        return false;
    }

    private String toAlgebraic(int r, int c) {
        char file = (char) ('a' + c);
        int rank = 4 - r;
        return "" + file + rank;
    }

    private char getPieceChar(Piece piece) {
        // 사용자의 요청 "왕: K, 장: G, 상: E, 자: P, 후: R"에 따라 매핑합니다.
        String name = piece.name();
        if (name.contains("KING")) return 'K';
        if (name.contains("GENERAL")) return 'G';
        if (name.contains("ELEPHANT")) return 'E';
        if (name.contains("PAWN")) return 'P';
        if (name.contains("PRINCE")) return 'R'; // "후"는 PRINCE로 구현되어 있음
        return '?';
    }

    private boolean checkGameOver() {
        boolean p1KingExists = board.findPiece(Piece.P1_KING) != null;
        boolean p2KingExists = board.findPiece(Piece.P2_KING) != null;
        
        return !p1KingExists || !p2KingExists;
    }

    public void undoLastMove() {
        if (!boardHistory.isEmpty()) {
            this.board = boardHistory.pop();
            if (!moveHistory.isEmpty()) {
                moveHistory.remove(moveHistory.size() - 1);
            }
            switchTurn();
        }
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == Piece.Player.P1) ? Piece.Player.P2 : Piece.Player.P1;
    }

    public GameBoard getBoard() {
        return board;
    }

    public Piece.Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Piece.Player getWinner() {
        return winner;
    }
    
    public List<String> getMoveHistory() {
        return moveHistory;
    }
}
