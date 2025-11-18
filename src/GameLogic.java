import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameLogic {

    private GameBoard board;
    private Piece.Player currentPlayer;
    private GameState gameState;
    private Piece.Player winner = null;
    private final List<String> moveHistory = new ArrayList<>();
    private GameBoard lastTurnBoardState;

    public enum GameState {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        GAME_OVER
    }

    public GameLogic() {
        this.board = new GameBoard();
        this.gameState = GameState.WAITING_FOR_PLAYERS;
    }

    public boolean handlePlace(Piece.Player player, Piece pieceToPlace, int row, int col) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        this.lastTurnBoardState = this.board.clone();

        // GameBoard에 말을 놓는 로직을 직접 호출
        if (board.placeCapturedPiece(player, pieceToPlace, row, col)) {
            moveHistory.add(String.format("PLACE %s %s %d,%d", player.name(), pieceToPlace.name(), row, col));
            switchTurn();
            return true;
        }
        return false;
    }

    public void startGame() { board.setupInitialBoard(); currentPlayer = Piece.Player.P1; gameState = GameState.IN_PROGRESS; winner = null; moveHistory.clear(); }
    public boolean handleMove(Piece.Player player, int fromR, int fromC, int toR, int toC) { if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false; this.lastTurnBoardState = this.board.clone(); Piece movingPiece = board.getPieceAt(fromR, fromC); if (movingPiece == null || movingPiece.getOwner() != player) return false; if (board.movePiece(fromR, fromC, toR, toC)) { moveHistory.add(String.format("MOVE %s %d,%d %d,%d", player.name(), fromR, fromC, toR, toC)); if (checkGameOver()) { gameState = GameState.GAME_OVER; this.winner = player; } else { switchTurn(); } return true; } return false; }
    private boolean checkGameOver() { boolean p1KingExists = board.findPiece(Piece.P1_KING) != null; boolean p2KingExists = board.findPiece(Piece.P2_KING) != null; return !p1KingExists || !p2KingExists; }
    public void undoLastMove() { if (lastTurnBoardState != null) { this.board = lastTurnBoardState; if (!moveHistory.isEmpty()) { moveHistory.remove(moveHistory.size() - 1); } switchTurn(); } }
    private void switchTurn() { currentPlayer = (currentPlayer == Piece.Player.P1) ? Piece.Player.P2 : Piece.Player.P1; }
    public GameBoard getBoard() { return board; }
    public Piece.Player getCurrentPlayer() { return currentPlayer; }
    public GameState getGameState() { return gameState; }
    // public Piece.Player getWinner() { return winner; } // wtf
    public List<String> getMoveHistory() { return moveHistory; }
}
