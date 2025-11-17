import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {

    private final String title;
    private ClientHandler host;
    private ClientHandler guest;
    private final List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameSession currentSession;

    public GameRoom(String title, ClientHandler host) {
        this.title = title;
        this.host = host;
        // 처음에는 플레이어가 한 명이므로, guest가 없는 대기 세션을 생성
        this.currentSession = new GameSession(this, host, null);
    }

    /**
     * 플레이어로부터 받은 명령을 현재 게임 세션으로 전달합니다.
     */
    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        if (currentSession != null) {
            currentSession.processCommand(player, message);
        }
    }

    /**
     * 새로운 플레이어를 방에 추가합니다.
     * 게스트 자리가 비어있으면 게스트로, 아니면 관전자로 추가됩니다.
     */
    public synchronized void addPlayer(ClientHandler player) {
        if (this.guest == null) {
            this.guest = player;
            broadcastSystem(Protocol.SYSTEM + " " + player.getNickname() + "님이 GUEST로 입장했습니다.");
            // 호스트와 게스트가 모두 준비되었으므로 새 게임 세션 시작
            startNewSession();
        } else {
            spectators.add(player);
            broadcastSystem(Protocol.SYSTEM + " " + player.getNickname() + "님이 관전자로 입장했습니다.");
        }
        
        // 게임이 이미 진행 중이라면, 새로 들어온 유저에게 현재 게임 상태 전송
        if (isGameInProgress()) {
            currentSession.broadcastState();
        }
        Server.broadcastRoomList();
    }

    /**
     * 방에서 플레이어를 제거합니다.
     * 플레이어가 호스트나 게스트였다면 게임에 영향을 미칠 수 있습니다.
     */
    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        boolean wasCorePlayer = (player == host || player == guest);

        // 게임 중에 핵심 플레이어(호스트/게스트)가 나갔을 경우, 게임을 종료시킴
        if (wasCorePlayer && isGameInProgress()) {
            ClientHandler winner = (player == host) ? guest : host;
            if (winner != null) {
                currentSession.endGame(winner, "상대방이 퇴장하여 게임에서 승리했습니다.");
                // endGame -> onSessionFinished -> startNewSession의 순서로 호출되어 아래 로직과 중복 실행되지 않음
                return; 
            }
        }

        // 플레이어 목록에서 제거
        if (player == host) {
            host = null;
        } else if (player == guest) {
            guest = null;
        } else {
            spectators.remove(player);
        }

        // 호스트가 나갔다면, 게스트가 새로운 호스트가 됨
        if (host == null) {
            host = guest;
            guest = null;
            if (host != null) {
                broadcastSystem(Protocol.SYSTEM + " 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
            }
        }
        
        // 게스트가 나갔거나, 호스트가 나가서 게스트가 호스트가 된 후, 관전자 중 첫번째가 새로운 게스트가 됨
        if (guest == null && !spectators.isEmpty()) {
            guest = spectators.remove(0);
            broadcastSystem(Protocol.SYSTEM + guest.getNickname() + "님이 새로운 GUEST가 되었습니다.");
        }

        // 방에 아무도 남지 않으면 방을 제거
        if (host == null) {
            Server.removeGameRoom(this.title);
        } else {
            // 핵심 플레이어 구성이 변경되었으므로, 새로운 세션을 시작
            if (wasCorePlayer) {
                startNewSession();
            }
            broadcastSystem(Protocol.SYSTEM + " " + leavingNickname + "님이 퇴장했습니다.");
            Server.broadcastRoomList();
        }
    }

    /**
     * 게임 세션이 종료되었을 때 GameSession에 의해 호출되는 콜백 메소드.
     * 승자가 새로운 호스트가 되고, 새로운 게임 세션을 준비합니다.
     * @param winner 승리한 플레이어
     */
    public void onSessionFinished(ClientHandler winner) {
        // 승자가 새로운 호스트가 됨
        if (winner != host) {
            this.guest = this.host;
            this.host = winner;
        }
        
        broadcastSystem(Protocol.SYSTEM + " " + winner.getNickname() + "님이 새로운 호스트입니다.");
        startNewSession();
        Server.broadcastRoomList();
    }

    /**
     * 새로운 게임 세션을 시작합니다.
     * 호스트와 게스트가 모두 있어야 게임 가능한 세션이 생성됩니다.
     */
    private void startNewSession() {
        if (host != null && guest != null) {
            this.currentSession = new GameSession(this, host, guest);
             broadcastSystem(Protocol.SYSTEM + " 새로운 게임을 시작할 수 있습니다. 준비 버튼을 눌러주세요.");
        } else {
            // 플레이어가 한명일 때는 게임을 시작할 수 없는 대기 세션 상태
            this.currentSession = new GameSession(this, host, null); 
        }
    }

    public void broadcastSystem(String message) {
        getAllUsers().forEach(user -> user.sendMessage(message));
    }

    public void broadcastChat(String message) {
        getAllUsers().forEach(user -> user.sendMessage(message));
    }

    public synchronized List<ClientHandler> getAllUsers() {
        List<ClientHandler> allUsers = new ArrayList<>();
        if (host != null) allUsers.add(host);
        if (guest != null) allUsers.add(guest);
        allUsers.addAll(spectators);
        return allUsers;
    }

    public boolean isGameInProgress() {
        return currentSession != null && currentSession.getGameState() == GameLogic.GameState.IN_PROGRESS;
    }

    public String getTitle() { return title; }
    public int getPlayerCount() { return getAllUsers().size(); }
}
