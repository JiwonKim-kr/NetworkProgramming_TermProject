import java.util.ArrayList;
import java.util.List;

/**
 * 십이장기 게임의 보드 상태를 관리하는 클래스입니다.
 * 기물의 배치 정보(4x3 배열)와 각 플레이어가 잡은 기물 목록을 포함합니다.
 * 기물의 이동, 잡기, 배치 등 보드와 직접적으로 관련된 로직을 처리합니다.
 */
public class GameBoard implements Cloneable {

    private Piece[][] board = new Piece[4][3];
    private List<Piece> p1Captured = new ArrayList<>();
    private List<Piece> p2Captured = new ArrayList<>();

    /**
     * GameBoard 생성자입니다.
     * 생성 시 보드를 초기 기물 배치 상태로 설정합니다.
     */
    public GameBoard() {
        setupInitialBoard();
    }

    /**
     * 게임 보드를 초기 기물 배치 상태로 설정합니다.
     * 모든 기물을 정해진 위치에 배치하고 잡은 말 목록을 비웁니다.
     */
    public void setupInitialBoard() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = null;
            }
        }
        p1Captured.clear();
        p2Captured.clear();

        // P1(아래쪽) 기물 배치
        board[3][0] = Piece.P1_ELEPHANT;
        board[3][1] = Piece.P1_KING;
        board[3][2] = Piece.P1_GENERAL;
        board[2][1] = Piece.P1_PAWN;

        // P2(위쪽) 기물 배치
        board[0][0] = Piece.P2_GENERAL;
        board[0][1] = Piece.P2_KING;
        board[0][2] = Piece.P2_ELEPHANT;
        board[1][1] = Piece.P2_PAWN;
    }

    /**
     * 지정된 위치의 기물을 반환합니다.
     * @param row 행 좌표
     * @param col 열 좌표
     * @return 해당 위치의 기물 객체, 없거나 범위를 벗어나면 null
     */
    public Piece getPieceAt(int row, int col) {
        return isValid(row, col) ? board[row][col] : null;
    }

    /**
     * 기물을 지정된 위치로 이동시킵니다.
     * 이동, 잡기, 승급 처리를 포함합니다.
     * @param fromRow 시작 행
     * @param fromCol 시작 열
     * @param toRow   목표 행
     * @param toCol   목표 열
     * @return 이동 성공 여부
     */
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) return false;

        Piece movingPiece = board[fromRow][fromCol];
        Piece targetPiece = board[toRow][toCol];

        // 목표 위치에 상대 기물이 있으면 잡기 처리
        if (targetPiece != null) {
            capturePiece(targetPiece);
        }

        board[toRow][toCol] = movingPiece;
        board[fromRow][fromCol] = null;

        // '자'가 상대 진영 끝에 도달하면 '후'로 승급 처리
        if (movingPiece.getDisplayName().equals("자")) {
            if (movingPiece.getOwner() == Piece.Player.P1 && toRow == 0) {
                board[toRow][toCol] = movingPiece.promote();
            } else if (movingPiece.getOwner() == Piece.Player.P2 && toRow == 3) {
                board[toRow][toCol] = movingPiece.promote();
            }
        }

        return true;
    }

    /**
     * 상대 기물을 잡아 해당 플레이어의 잡은 기물 목록에 추가합니다.
     * @param piece 잡힌 기물
     */
    private void capturePiece(Piece piece) {
        // 잡힌 기물은 소유주가 바뀌고, '후'는 '자'로 강등됨
        Piece capturedAs = Piece.flipOwner(piece).demote();
        if (capturedAs.getOwner() == Piece.Player.P1) {
            p1Captured.add(capturedAs);
        } else {
            p2Captured.add(capturedAs);
        }
    }

    /**
     * 플레이어가 잡은 기물을 보드의 빈 칸에 내려놓습니다.
     * @param placingPlayer 기물을 놓는 플레이어
     * @param pieceToPlace  놓을 기물
     * @param row           놓을 위치의 행
     * @param col           놓을 위치의 열
     * @return 배치 성공 여부
     */
    public boolean placeCapturedPiece(Piece.Player placingPlayer, Piece pieceToPlace, int row, int col) {
        if (board[row][col] != null) return false; // 빈 칸에만 놓을 수 있음
        
        // '자'는 상대 진영 첫 줄에 놓을 수 없는 규칙 처리
        if (pieceToPlace.getDisplayName().equals("자")) {
            if (placingPlayer == Piece.Player.P1 && row == 0) return false;
            if (placingPlayer == Piece.Player.P2 && row == 3) return false;
        }
        if (pieceToPlace.getOwner() != placingPlayer) return false; // 자신의 기물만 놓을 수 있음

        List<Piece> capturedList = (placingPlayer == Piece.Player.P1) ? p1Captured : p2Captured;
        // 해당 기물을 잡은 목록에서 제거하고 보드에 추가
        if (capturedList.remove(pieceToPlace)) {
            board[row][col] = pieceToPlace;
            return true;
        }
        return false;
    }

    /**
     * 해당 위치의 기물에 대한 유효한 이동 목록을 반환합니다.
     * 실제 계산은 기물이 가진 MoveStrategy에 위임합니다.
     */
    public List<int[]> getValidMoves(int row, int col) {
        Piece piece = getPieceAt(row, col);
        if (piece == null) {
            return new ArrayList<>();
        }
        return piece.getMoveStrategy().getValidMoves(this, piece, row, col);
    }

    /**
     * 특정 기물이 보드 위에 어디에 있는지 찾습니다.
     * @param pieceToFind 찾을 기물
     * @return 기물의 [행, 열] 좌표, 없으면 null
     */
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

    /**
     * 주어진 좌표가 보드 범위 내에 있는지 확인합니다.
     * MoveStrategy에서 보드 경계를 확인할 수 있도록 public으로 변경합니다.
     */
    public boolean isValid(int row, int col) {
        return row >= 0 && row < 4 && col >= 0 && col < 3;
    }

    /**
     * 특정 이동이 유효한지 확인하는 내부 헬퍼 메서드입니다.
     */
    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        return getValidMoves(fromRow, fromCol).stream().anyMatch(m -> m[0] == toRow && m[1] == toCol);
    }

    public List<Piece> getP1Captured() {
        return p1Captured;
    }

    public List<Piece> getP2Captured() {
        return p2Captured;
    }

    /**
     * 현재 GameBoard 객체의 깊은 복사본을 생성합니다.
     * '수 무르기' 기능을 위해 사용됩니다.
     * @return 복제된 GameBoard 객체
     */
    @Override
    public GameBoard clone() {
        try {
            GameBoard cloned = (GameBoard) super.clone();
            // 2차원 배열 깊은 복사
            cloned.board = new Piece[4][3];
            for (int r = 0; r < 4; r++) {
                System.arraycopy(this.board[r], 0, cloned.board[r], 0, 3);
            }
            // 잡은 말 리스트 깊은 복사
            cloned.p1Captured = new ArrayList<>(this.p1Captured);
            cloned.p2Captured = new ArrayList<>(this.p2Captured);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Cloneable을 구현했으므로 발생하지 않아야 함
        }
    }
}
