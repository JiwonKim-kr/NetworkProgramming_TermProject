public enum Piece {
    // 각 기물에 맞는 이동 전략을 주입합니다.
    P1_KING("왕", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}})),
    P1_GENERAL("장", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}})),
    P1_ELEPHANT("상", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,-1}, {-1,1}, {1,-1}, {1,1}})),
    P1_PAWN("자", Player.P1, new PawnMoveStrategy()),
    P1_PRINCE("후", Player.P1, new DefaultMoveStrategy(new int[][]{{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}})),

    P2_KING("왕", Player.P2, P1_KING.moveStrategy),
    P2_GENERAL("장", Player.P2, P1_GENERAL.moveStrategy),
    P2_ELEPHANT("상", Player.P2, P1_ELEPHANT.moveStrategy),
    P2_PAWN("자", Player.P2, P1_PAWN.moveStrategy),
    P2_PRINCE("후", Player.P2, P1_PRINCE.moveStrategy);

    public enum Player { P1, P2 }

    private final String displayName;
    private final Player owner;
    private final MoveStrategy moveStrategy;

    Piece(String displayName, Player owner, MoveStrategy moveStrategy) {
        this.displayName = displayName;
        this.owner = owner;
        this.moveStrategy = moveStrategy;
    }

    public String getDisplayName() { return displayName; }
    public Player getOwner() { return owner; }
    public MoveStrategy getMoveStrategy() { return moveStrategy; }

    public Piece promote() {
        if (this == P1_PAWN) return P1_PRINCE;
        if (this == P2_PAWN) return P2_PRINCE;
        return this;
    }

    public Piece demote() {
        if (this == P1_PRINCE) return P1_PAWN;
        if (this == P2_PRINCE) return P2_PAWN;
        return this;
    }

    public static Piece flipOwner(Piece piece) {
        if (piece == null) return null;
        switch (piece) {
            case P1_KING: return P2_KING;
            case P1_GENERAL: return P2_GENERAL;
            case P1_ELEPHANT: return P2_ELEPHANT;
            case P1_PAWN: return P2_PAWN;
            case P1_PRINCE: return P2_PRINCE;
            case P2_KING: return P1_KING;
            case P2_GENERAL: return P1_GENERAL;
            case P2_ELEPHANT: return P1_ELEPHANT;
            case P2_PAWN: return P1_PAWN;
            case P2_PRINCE: return P1_PRINCE;
            default: return piece;
        }
    }
}
