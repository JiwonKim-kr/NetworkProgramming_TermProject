/**
 * 십이장기 게임의 기물(Piece)을 정의하는 열거형(Enum)입니다.
 * 각 기물은 이름, 소유자, 그리고 이동 전략(MoveStrategy)을 가집니다.
 * 전략 패턴을 사용하여 기물의 이동 로직을 캡슐화합니다.
 */
public enum Piece {
    // 각 기물에 맞는 이동 전략을 주입합니다.
    P1_KING("왕", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}})),
    P1_GENERAL("장", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}})),
    P1_ELEPHANT("상", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,-1}, {-1,1}, {1,-1}, {1,1}})),
    P1_PAWN("자", Player.P1, new PawnMoveStrategy()),
    P1_PRINCE("후", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}})),

    // P2 기물들은 P1 기물과 동일한 이동 전략을 공유합니다.
    P2_KING("왕", Player.P2, P1_KING.moveStrategy),
    P2_GENERAL("장", Player.P2, P1_GENERAL.moveStrategy),
    P2_ELEPHANT("상", Player.P2, P1_ELEPHANT.moveStrategy),
    P2_PAWN("자", Player.P2, P1_PAWN.moveStrategy),
    P2_PRINCE("후", Player.P2, P1_PRINCE.moveStrategy);

    /**
     * 플레이어를 구분하는 열거형입니다. (P1, P2)
     */
    public enum Player { P1, P2 }

    private final String displayName;
    private final Player owner;
    private final MoveStrategy moveStrategy;

    /**
     * 기물 객체를 생성합니다.
     * @param displayName UI에 표시될 기물의 이름 (예: "왕")
     * @param owner 기물의 소유자 (P1 또는 P2)
     * @param moveStrategy 해당 기물의 이동 규칙을 정의하는 전략 객체
     */
    Piece(String displayName, Player owner, MoveStrategy moveStrategy) {
        this.displayName = displayName;
        this.owner = owner;
        this.moveStrategy = moveStrategy;
    }

    public String getDisplayName() { return displayName; }
    public Player getOwner() { return owner; }
    public MoveStrategy getMoveStrategy() { return moveStrategy; }

    /**
     * '자'가 상대 진영 끝에 도달했을 때 '후'로 승급시킵니다.
     * @return 승급된 기물 또는 변경 없는 기존 기물
     */
    public Piece promote() {
        if (this == P1_PAWN) return P1_PRINCE;
        if (this == P2_PAWN) return P2_PRINCE;
        return this;
    }

    /**
     * 잡힌 '후'를 다시 말 판에 놓을 때 '자'로 강등시킵니다.
     * (십이장기 규칙에 따라 잡힌 '후'는 '자'가 됨)
     * @return 강등된 기물 또는 변경 없는 기존 기물
     */
    public Piece demote() {
        if (this == P1_PRINCE) return P1_PAWN;
        if (this == P2_PRINCE) return P2_PAWN;
        return this;
    }

    /**
     * 기물이 잡혔을 때 소유자를 상대방으로 변경합니다.
     * 예를 들어, P1의 왕이 잡히면 P2가 소유한 P2_KING이 됩니다.
     * @param piece 소유자를 변경할 기물
     * @return 소유자가 변경된 새로운 기물
     */
    public static Piece flipOwner(Piece piece) {
        if (piece == null) return null;
        return switch (piece) {
            case P1_KING -> P2_KING;
            case P1_GENERAL -> P2_GENERAL;
            case P1_ELEPHANT -> P2_ELEPHANT;
            case P1_PAWN -> P2_PAWN;
            case P1_PRINCE -> P2_PRINCE;
            case P2_KING -> P1_KING;
            case P2_GENERAL -> P1_GENERAL;
            case P2_ELEPHANT -> P1_ELEPHANT;
            case P2_PAWN -> P1_PAWN;
            case P2_PRINCE -> P1_PRINCE;
            default -> piece;
        };
    }
}
