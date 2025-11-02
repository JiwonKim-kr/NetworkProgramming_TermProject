public enum Piece {
    P1_KING("왕", Player.P1), P1_GENERAL("장", Player.P1), P1_ELEPHANT("상", Player.P1), P1_PAWN("자", Player.P1), P1_PRINCE("후", Player.P1),
    P2_KING("왕", Player.P2), P2_GENERAL("장", Player.P2), P2_ELEPHANT("상", Player.P2), P2_PAWN("자", Player.P2), P2_PRINCE("후", Player.P2);

    public enum Player { P1, P2 }

    private final String displayName;
    private final Player owner;

    Piece(String displayName, Player owner) {
        this.displayName = displayName;
        this.owner = owner;
    }

    public String getDisplayName() { return displayName; }
    public Player getOwner() { return owner; }

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
