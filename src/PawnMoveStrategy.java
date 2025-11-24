import java.util.ArrayList;
import java.util.List;

/**
 * '자'(Pawn) 기물의 이동 전략을 구현합니다. '자'는 앞으로만 한 칸 이동할 수 있습니다.
 */
public class PawnMoveStrategy implements MoveStrategy {

    @Override
    public List<int[]> getValidMoves(GameBoard board, Piece piece, int r, int c) {
        List<int[]> moves = new ArrayList<>();
        // '자'는 소유자에 따라 전진 방향이 다름
        int direction = (piece.getOwner() == Piece.Player.P1) ? -1 : 1;
        int newRow = r + direction;
        int newCol = c;

        if (board.isValid(newRow, newCol)) {
            Piece target = board.getPieceAt(newRow, newCol);
            if (target == null || target.getOwner() != piece.getOwner()) {
                moves.add(new int[]{newRow, newCol});
            }
        }
        return moves;
    }
}
