/**
 * 클라이언트와 서버 간의 통신에 사용되는 프로토콜 명령어 및 상수들을 정의하는 유틸리티 클래스입니다.
 * 모든 상수는 public static final로 선언되어 있어, 코드 전반에서 일관된 메시지 형식을 유지할 수 있습니다.
 * 이 클래스는 인스턴스화될 필요가 없습니다.
 */
public final class Protocol {
    private Protocol() {} // 인스턴스화 방지

    // --- 클라이언트 -> 서버로 전송되는 명령어들 ---
    public static final String CREATE_ROOM = "CREATE_ROOM"; // 방 생성 요청
    public static final String JOIN_ROOM = "JOIN_ROOM";     // 방 참가 요청
    public static final String LEAVE_ROOM = "LEAVE_ROOM";   // 방 나가기 요청
    public static final String READY = "READY";             // 게임 준비 상태 토글
    public static final String MOVE = "MOVE";               // 기물 이동 요청
    public static final String PLACE = "PLACE";             // 잡은 기물 놓기 요청
    public static final String GET_VALID_MOVES = "GET_VALID_MOVES"; // 특정 기물의 유효 이동 경로 요청
    public static final String UNDO_REQUEST = "UNDO_REQUEST"; // 수 무르기 요청
    public static final String UNDO_RESPONSE = "UNDO_RESPONSE"; // 수 무르기 응답 (수락/거절)
    public static final String CHAT = "CHAT";               // 방 내부 채팅 메시지
    public static final String LOBBY_CHAT = "LOBBY_CHAT";   // 로비 채팅 메시지
    public static final String CHANGE_NICKNAME = "CHANGE_NICKNAME"; // 닉네임 변경 요청


    // --- 서버 -> 클라이언트로 전송되는 명령어들 ---
    public static final String NICKNAME_OK = "NICKNAME_OK"; // 닉네임 설정 성공
    public static final String NICKNAME_TAKEN = "NICKNAME_TAKEN"; // 닉네임 중복
    public static final String NICKNAME_CHANGED_OK = "NICKNAME_CHANGED_OK"; // 닉네임 변경 성공
    public static final String NICKNAME_CHANGE_FAILED = "NICKNAME_CHANGE_FAILED"; // 닉네임 변경 실패
    public static final String ASSIGN_ROLE = "ASSIGN_ROLE"; // 플레이어 역할 할당 (P1/P2)
    public static final String UPDATE_ROOMLIST = "UPDATE_ROOMLIST"; // 로비 방 목록 및 접속자 목록 갱신
    public static final String JOIN_SUCCESS = "JOIN_SUCCESS"; // 방 참가 성공
    public static final String GOTO_LOBBY = "GOTO_LOBBY";   // 로비로 이동 명령
    public static final String SYSTEM = "SYSTEM";           // 시스템 메시지
    public static final String PLAYER_READY = "PLAYER_READY"; // 플레이어 준비 상태 갱신
    public static final String GAME_START = "GAME_START";   // 게임 시작 알림
    public static final String UPDATE_STATE = "UPDATE_STATE"; // 게임 상태 갱신 (보드, 잡은 말, 턴 등)
    public static final String VALID_MOVES = "VALID_MOVES"; // 유효한 이동 경로 정보
    public static final String GAME_OVER = "GAME_OVER";     // 게임 종료 알림
    public static final String UNDO_REQUESTED = "UNDO_REQUESTED"; // 수 무르기 요청 받음
    public static final String ERROR = "ERROR";             // 오류 메시지
    public static final String REQUEST_ROOMINFO = "REQUEST_ROOMINFO"; // 방 정보 요청 (현재 사용되지 않음)
    public static final String ROOMINFO_PRIVATE = "ROOMINFO_PRIVATE"; // 방이 비밀방임을 알림 (현재 사용되지 않음)
    public static final String ROOMINFO_PUBLIC = "ROOMINFO_PUBLIC";   // 방이 공개방임을 알림 (현재 사용되지 않음)

    // --- 플레이어 역할 정의 상수 ---
    public static final String HOST = "HOST"; // 방장 역할
    public static final String GUEST = "GUEST"; // 손님 역할
    public static final String P1 = "P1";     // 게임 내 플레이어 1
    public static final String P2 = "P2";     // 게임 내 플레이어 2
}
