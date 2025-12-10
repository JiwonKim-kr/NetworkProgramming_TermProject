import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 개별 게임방을 나타내는 클래스입니다.
 * 방의 정보(제목, 비밀번호 등)와 참여자(호스트, 게스트, 관전자) 목록을 관리합니다.
 * 실제 게임 로직은 GameSession 클래스에 위임합니다.
 */
public class GameRoom {

    private String title;
    private String password;
    private int maxPlayers;
    private ClientHandler host;
    private ClientHandler guest;
    // 관전자 목록 (스레드 안전)
    private List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameSession currentSession;
    private boolean isPrivate;

    /**
     * 새로운 게임방을 생성합니다.
     * @param title 방 제목
     * @param password 비밀번호 (없으면 빈 문자열)
     * @param maxPlayers 최대 수용 인원
     * @param host 방을 생성한 호스트
     */
    public GameRoom(String title, String password, int maxPlayers, ClientHandler host) {
        this.title = title;
        this.password = password;
        this.maxPlayers = maxPlayers;
        this.host = host;
        this.isPrivate = isPrivate;
        this.password = password;
        this.currentSession = new GameSession(this, host, null);
        // 방 생성 성공 메시지를 호스트에게 전송
        host.sendMessage(Protocol.JOIN_SUCCESS + " " + this.title);
    }
    public boolean isPrivateRoom() {
        return isPrivate;
    }

    public String getPassword() {
        return password;
    }

    /**
     * 플레이어로부터 받은 게임 관련 명령을 현재 게임 세션으로 전달합니다.
     * @param player 명령을 보낸 플레이어
     * @param message 플레이어가 보낸 전체 메시지
     */
    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        if (currentSession != null) {
            currentSession.processCommand(player, message);
        }
    }

    /**
     * 새로운 플레이어를 방에 추가합니다.
     * @param player 입장하려는 플레이어
     * @param password 플레이어가 입력한 비밀번호
     */
    public synchronized void addPlayer(ClientHandler player, String password) {
        // 방 인원 제한 확인
        if (getPlayerCount() >= maxPlayers) {
            player.sendMessage(Protocol.ERROR + " 방이 꽉 찼습니다.");
            return;
        }
        // 비밀방일 경우 비밀번호 확인
        if (!this.password.isEmpty() && !this.password.equals(password)) {
            player.sendMessage(Protocol.ERROR + " 비밀번호가 일치하지 않습니다.");
            return;
        }

        player.setCurrentRoom(this);
        player.sendMessage(Protocol.JOIN_SUCCESS + " " + this.title);

        // 게스트 자리가 비어있으면 게스트로 입장
        if (this.guest == null) {
            this.guest = player;
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 GUEST로 입장했습니다.");
            startNewSession(); // 호스트와 게스트가 모두 있으므로 새 게임 세션 시작
        } else { // 게스트 자리가 차있으면 관전자로 입장
            spectators.add(player);
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 관전자로 입장했습니다.");
            // 만약 게임이 진행 중이라면, 현재 게임 상태를 전송하여 관전할 수 있도록 함
            if (isGameInProgress()) {
                currentSession.broadcastState();
            }
        }
        // 로비에 있는 모든 유저에게 방 목록 정보 갱신
        Server.broadcastRoomList();
    }

    /**
     * 플레이어를 방에서 내보냅니다.
     * 플레이어가 호스트나 게스트였을 경우, 게임 상태에 따라 추가적인 처리를 합니다.
     * @param player 퇴장하는 플레이어
     */
    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        boolean wasCorePlayer = (player == host || player == guest);

        player.setCurrentRoom(null);
        player.sendMessage(Protocol.GOTO_LOBBY); // 클라이언트를 로비 화면으로 보냄

        // 게임 진행 중에 핵심 플레이어가 나갔다면 게임을 중단시킴
        if (wasCorePlayer && isGameInProgress()) {
            currentSession.abortGame("상대방이 퇴장하여 게임이 종료되었습니다.", player);
        }

        // 플레이어 역할에 따라 목록에서 제거
        if (player == host) host = null;
        else if (player == guest) guest = null;
        else spectators.remove(player);

        // 방에 아무도 없으면 방을 제거
        if (host == null && guest == null && spectators.isEmpty()) {
            Server.removeGameRoom(this.title);
            return;
        }

        boolean hostChanged = false;
        // 호스트가 나갔을 경우, 다음 순서에 따라 새 호스트를 지정 (게스트 -> 관전자 1순위)
        if (host == null) {
            if (guest != null) {
                host = guest;
                guest = null;
            } else if (!spectators.isEmpty()) {
                host = spectators.remove(0);
            }
            hostChanged = true;
        }

        // 게스트가 나갔거나, 관전자가 게스트로 승격되었을 경우, 다음 관전자를 게스트로 지정
        if (guest == null && !spectators.isEmpty()) {
            guest = spectators.remove(0);
            broadcastSystem("SYSTEM: " + guest.getNickname() + "님이 새로운 GUEST가 되었습니다.");
        }

        if (hostChanged) {
            broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        }
        // 핵심 플레이어가 변경되었으므로, 새 게임을 시작할 수 있도록 세션 초기화
        if (wasCorePlayer) {
            startNewSession();
        }

        broadcastSystem("SYSTEM: " + leavingNickname + "님이 퇴장했습니다.");
        Server.broadcastRoomList();
    }

    /**
     * 한 게임 세션이 끝났을 때 호출됩니다.
     * 승자를 새로운 호스트로, 패자를 게스트로 설정하여 다음 게임을 준비합니다.
     * @param winner 게임의 승자
     */
    public void onSessionFinished(ClientHandler winner) {
        ClientHandler loser = (winner == host) ? guest : host;
        this.host = winner;
        this.guest = loser;
        broadcastSystem("SYSTEM: " + winner.getNickname() + "님이 새로운 호스트입니다.");
        startNewSession();
        Server.broadcastRoomList();
    }

    /**
     * 새로운 게임 세션을 시작합니다.
     * 호스트와 게스트가 모두 존재할 때만 실제 게임을 할 수 있는 세션이 생성됩니다.
     */
    private void startNewSession() {
        if (host != null && guest != null) {
            this.currentSession = new GameSession(this, host, guest);
            broadcastSystem("SYSTEM: 새로운 게임을 시작할 수 있습니다. 준비 버튼을 눌러주세요.");
        } else {
            // 플레이어가 한 명만 있을 경우, 대기 상태의 세션 생성
            this.currentSession = new GameSession(this, host, null);
        }
    }

    // --- 브로드캐스트 헬퍼 메서드 ---
    public void broadcastSystem(String message) { getAllUsers().forEach(user -> user.sendMessage(message)); }
    public void broadcastSystemExcept(ClientHandler except, String message) { getAllUsers().stream().filter(user -> user != except).forEach(user -> user.sendMessage(message)); }
    public void broadcastChat(String message) { getAllUsers().forEach(user -> user.sendMessage(message)); }

    /**
     * 방에 있는 모든 사용자(호스트, 게스트, 관전자)의 리스트를 반환합니다.
     * @return 모든 사용자의 ClientHandler 리스트
     */
    public synchronized List<ClientHandler> getAllUsers() {
        List<ClientHandler> allUsers = new ArrayList<>();
        if (host != null) allUsers.add(host);
        if (guest != null) allUsers.add(guest);
        allUsers.addAll(spectators);
        return allUsers;
    }

    // --- Getter 메서드 ---
    public boolean isGameInProgress() { return currentSession != null && currentSession.getGameState() == GameLogic.GameState.IN_PROGRESS; }
    public String getTitle() { return title; }
    public int getPlayerCount() { return getAllUsers().size(); }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isPrivate() { return password != null && !password.isEmpty(); }
}
