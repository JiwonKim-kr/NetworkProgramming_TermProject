import java.util.ArrayList;
import java.util.List;

public class GameBoard implements Cloneable {

    private Piece[][] board = new Piece[4][3];
    private List<Piece> p1Captured = new ArrayList<>();
    private List<Piece> p2Captured = new ArrayList<>();

    public GameBoard() {
        setupInitialBoard();
    }

    public void setupInitialBoard() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = null;
            }
        }
        p1Captured.clear();
        p2Captured.clear();

        board[3][0] = Piece.P1_ELEPHANT;
        board[3][1] = Piece.P1_KING;
        board[3][2] = Piece.P1_GENERAL;
        board[2][1] = Piece.P1_PAWN;

        board[0][0] = Piece.P2_GENERAL;
        board[0][1] = Piece.P2_KING;
        board[0][2] = Piece.P2_ELEPHANT;
        board[1][1] = Piece.P2_PAWN;
    }

    public Piece getPieceAt(int row, int col) {
        return isValid(row, col) ? board[row][col] : null;
    }

    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) return false;

        Piece movingPiece = board[fromRow][fromCol];
        Piece targetPiece = board[toRow][toCol];

        if (targetPiece != null) {
            capturePiece(targetPiece);
        }

        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        if (movingPiece == Piece.P1_PAWN && toRow == 0) board[toRow][toCol] = movingPiece.promote();
        if (movingPiece == Piece.P2_PAWN && toRow == 3) board[toRow][toCol] = movingPiece.promote();

        return true;
    }

    private void capturePiece(Piece piece) {
        Piece capturedAs = Piece.flipOwner(piece).demote();
        if (capturedAs.getOwner() == Piece.Player.P1) {
            p1Captured.add(capturedAs);
        } else {
            p2Captured.add(capturedAs);
        }
    }

    public boolean placeCapturedPiece(Piece.Player placingPlayer, Piece pieceToPlace, int row, int col) {
        if (board[row][col] != null) return false;
        if ((placingPlayer == Piece.Player.P1 && row == 0) || (placingPlayer == Piece.Player.P2 && row == 3)) return false;
        if (pieceToPlace.getOwner() != placingPlayer) return false;

        List<Piece> capturedList = (placingPlayer == Piece.Player.P1) ? p1Captured : p2Captured;
        if (capturedList.remove(pieceToPlace)) {
            board[row][col] = pieceToPlace;
            return true;
        }
        return false;
    }

    public List<int[]> getValidMoves(int row, int col) {
        List<int[]> moves = new ArrayList<>();
        Piece piece = getPieceAt(row, col);
        if (piece == null) return moves;

        int[][] directions = getDirections(piece);
        for (int[] d : directions) {
            int newRow = row + d[0];
            int newCol = col + d[1];
            if (isValid(newRow, newCol)) {
                Piece target = getPieceAt(newRow, newCol);
                if (target == null || target.getOwner() != piece.getOwner()) {
                    moves.add(new int[]{newRow, newCol});
                }
            }
        }
        return moves;
    }

    protected int[][] getDirections(Piece piece) {
        switch (piece.getDisplayName()) {
            case "왕": return new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
            case "장": return new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}};
            case "상": return new int[][]{{-1,-1}, {-1,1}, {1,-1}, {1,1}};
            case "자": return (piece.getOwner() == Piece.Player.P1) ? new int[][]{{-1,0}} : new int[][]{{1,0}};
            case "후": return new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}};
            default: return new int[0][0];
        }
    }

    public int[] findPiece(Piece pieceToFind) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[r][c] == pieceToFind) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < 4 && col >= 0 && col < 3;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        return getValidMoves(fromRow, fromCol).stream().anyMatch(m -> m[0] == toRow && m[1] == toCol);
    }

    public List<Piece> getP1Captured() {
        return p1Captured;
    }

    public List<Piece> getP2Captured() {
        return p2Captured;
    }

    @Override
    public GameBoard clone() {
        try {
            GameBoard cloned = (GameBoard) super.clone();
            cloned.board = new Piece[4][3];
            for (int r = 0; r < 4; r++) {
                System.arraycopy(this.board[r], 0, cloned.board[r], 0, 3);
            }
            cloned.p1Captured = new ArrayList<>(this.p1Captured);
            cloned.p2Captured = new ArrayList<>(this.p2Captured);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
