import java.util.ArrayList;
import java.util.List;

/**
 * 지정된 방향으로 한 칸씩 이동하는 기물들의 기본 이동 전략을 구현합니다.
 */
public class DefaultMoveStrategy implements MoveStrategy {

    private final int[][] directions;

    public DefaultMoveStrategy(int[][] directions) {
        this.directions = directions;
    }

    @Override
    public List<int[]> getValidMoves(GameBoard board, Piece piece, int r, int c) {
        List<int[]> moves = new ArrayList<>();
        for (int[] d : directions) {
            int newRow = r + d[0];
            int newCol = c + d[1];

            if (board.isValid(newRow, newCol)) {
                Piece target = board.getPieceAt(newRow, newCol);
                // 이동할 위치가 비어있거나, 상대방의 기물이 있는 경우
                if (target == null || target.getOwner() != piece.getOwner()) {
                    moves.add(new int[]{newRow, newCol});
                }
            }
        }
        return moves;
    }
}
