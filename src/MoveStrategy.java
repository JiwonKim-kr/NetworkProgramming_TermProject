import java.util.List;

/**
 * 기물의 이동 전략을 정의하는 인터페이스입니다.
 * 전략 패턴을 적용하여 각 기물의 이동 규칙 계산을 캡슐화합니다.
 */
public interface MoveStrategy {
    /**
     * 지정된 위치의 기물이 이동할 수 있는 모든 유효한 위치 목록을 반환합니다.
     *
     * @param board 현재 게임 보드
     * @param piece 이동할 기물
     * @param r     현재 기물의 행(row)
     * @param c     현재 기물의 열(column)
     * @return 이동 가능한 모든 위치의 [r, c] 배열 리스트
     */
    List<int[]> getValidMoves(GameBoard board, Piece piece, int r, int c);
}
