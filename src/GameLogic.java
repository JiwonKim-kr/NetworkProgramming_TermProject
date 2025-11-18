import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class GameLogic {

    private GameBoard board;
    private Piece.Player currentPlayer;
    private GameState gameState;
    private Piece.Player winner = null;
    // 수정: final 키워드 추가
    private final List<String> moveHistory = new ArrayList<>();
    // 수정: final 키워드 추가
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
        currentPlayer = new Random().nextBoolean() ? Piece.Player.P1 : Piece.Player.P2;
        gameState = GameState.IN_PROGRESS;
        winner = null;
        moveHistory.clear();
        boardHistory.clear();
    }

    public boolean handleMove(Piece.Player player, int fromR, int fromC, int toR, int toC) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        boardHistory.push(this.board.clone());

        Piece movingPiece = board.getPieceAt(fromR, fromC);
        if (movingPiece == null || movingPiece.getOwner() != player) {
            // 히스토리에 추가된 잘못된 상태를 다시 제거
            boardHistory.pop();
            return false;
        }

        if (board.movePiece(fromR, fromC, toR, toC)) {
            moveHistory.add(String.format("MOVE %s %d,%d %d,%d", player.name(), fromR, fromC, toR, toC));
            if (checkGameOver()) {
                gameState = GameState.GAME_OVER;
                this.winner = player;
            } else {
                switchTurn();
            }
            return true;
        }
        
        // 이동에 실패한 경우에도 히스토리에서 제거
        boardHistory.pop();
        return false;
    }

    public boolean handlePlace(Piece.Player player, Piece pieceToPlace, int row, int col) {
        if (player != currentPlayer || gameState != GameState.IN_PROGRESS) return false;

        boardHistory.push(this.board.clone());

        if (board.placeCapturedPiece(player, pieceToPlace, row, col)) {
            moveHistory.add(String.format("PLACE %s %s %d,%d", player.name(), pieceToPlace.name(), row, col));
            switchTurn();
            return true;
        }
        
        // 배치에 실패한 경우 히스토리에서 제거
        boardHistory.pop();
        return false;
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
