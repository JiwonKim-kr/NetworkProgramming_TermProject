
public final class Protocol {
    private Protocol() {} // 인스턴스화 방지

    // 클라이언트 -> 서버
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String LEAVE_ROOM = "LEAVE_ROOM";
    public static final String READY = "READY";
    public static final String MOVE = "MOVE";
    public static final String PLACE = "PLACE";
    public static final String GET_VALID_MOVES = "GET_VALID_MOVES";
    public static final String UNDO_REQUEST = "UNDO_REQUEST";
    public static final String UNDO_RESPONSE = "UNDO_RESPONSE";
    public static final String CHAT = "CHAT";
    public static final String LOBBY_CHAT = "LOBBY_CHAT";
    // 서버 -> 클라이언트
    public static final String NICKNAME_OK = "NICKNAME_OK";
    public static final String NICKNAME_TAKEN = "NICKNAME_TAKEN";
    public static final String ASSIGN_ROLE = "ASSIGN_ROLE";
    public static final String UPDATE_ROOMLIST = "UPDATE_ROOMLIST";
    public static final String JOIN_SUCCESS = "JOIN_SUCCESS";
    public static final String GOTO_LOBBY = "GOTO_LOBBY";
    public static final String SYSTEM = "SYSTEM";
    public static final String PLAYER_READY = "PLAYER_READY";
    public static final String GAME_START = "GAME_START";
    public static final String UPDATE_STATE = "UPDATE_STATE";
    public static final String VALID_MOVES = "VALID_MOVES";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String UNDO_REQUESTED = "UNDO_REQUESTED";
    public static final String ERROR = "ERROR";
    public static final String REQUEST_ROOMINFO = "REQUEST_ROOMINFO";
    public static final String ROOMINFO_PRIVATE = "ROOMINFO_PRIVATE";
    public static final String ROOMINFO_PUBLIC = "ROOMINFO_PUBLIC";
    // 플레이어 역할
    public static final String HOST = "HOST";
    public static final String GUEST = "GUEST";
    public static final String P1 = "P1";
    public static final String P2 = "P2";
}
