import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {

    private String title;
    private ClientHandler host;
    private ClientHandler guest;
    private List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameSession currentSession;

    public GameRoom(String title, ClientHandler host) {
        this.title = title;
        this.host = host;
        this.currentSession = new GameSession(this, host, null);
        // 방 생성자에게도 입장 성공 메시지 전송
        host.sendMessage(Protocol.JOIN_SUCCESS + " " + this.title);
    }

    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        if (currentSession != null) {
            currentSession.processCommand(player, message);
        }
    }

    public synchronized void addPlayer(ClientHandler player) {
        // 중요: 플레이어에게 현재 방이 어디인지 알려준다.
        player.setCurrentRoom(this);
        player.sendMessage(Protocol.JOIN_SUCCESS + " " + this.title);

        if (this.guest == null) {
            this.guest = player;
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 GUEST로 입장했습니다.");
            startNewSession();
        } else {
            spectators.add(player);
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 관전자로 입장했습니다.");

            if (isGameInProgress()) {
                currentSession.broadcastState();
            }
        }
        Server.broadcastRoomList();
    }

    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        boolean wasCorePlayer = (player == host || player == guest);

        // 1. 나가는 플레이어에게 즉시 로비로 가라고 명령하고, 서버 상태를 업데이트한다.
        player.setCurrentRoom(null);
        player.sendMessage(Protocol.GOTO_LOBBY);

        // 2. 게임 중에 핵심 플레이어가 나갔다면, 게임을 중단시킨다.
        if (wasCorePlayer && isGameInProgress()) {
            currentSession.abortGame("상대방이 퇴장하여 게임이 종료되었습니다.", player);
        }

        // 3. 플레이어를 역할 목록에서 제거한다.
        if (player == host) {
            host = null;
        } else if (player == guest) {
            guest = null;
        } else {
            spectators.remove(player);
        }

        // 4. 방이 비었는지 확인하고, 비었다면 즉시 제거하고 종료한다.
        if (host == null && guest == null && spectators.isEmpty()) {
            Server.removeGameRoom(this.title);
            return; // 중요: 소멸될 방에 대해 더 이상 작업을 수행하지 않음
        }

        // 5. 방이 비지 않았다면, 역할 승격 및 상태 업데이트를 진행한다.
        boolean hostChanged = false;
        if (host == null) {
            if (guest != null) { // 게스트를 호스트로 승격
                host = guest;
                guest = null;
            } else { // 관전자를 호스트로 승격
                host = spectators.remove(0);
            }
            hostChanged = true;
        }

        if (guest == null && !spectators.isEmpty()) { // 게스트 자리를 관전자로 채움
            guest = spectators.remove(0);
            broadcastSystem("SYSTEM: " + guest.getNickname() + "님이 새로운 GUEST가 되었습니다.");
        }

        // 6. 남은 인원과 로비에 변경 사항을 알린다.
        if (hostChanged) {
            broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        }

        if (wasCorePlayer) { // 핵심 플레이어가 나갔으므로 새 게임 세션을 준비
            startNewSession();
        }

        broadcastSystem("SYSTEM: " + leavingNickname + "님이 퇴장했습니다.");
        Server.broadcastRoomList();
    }


    public void onSessionFinished(ClientHandler winner) {
        ClientHandler loser = (winner == host) ? guest : host;
        
        this.host = winner;
        this.guest = loser;
        
        broadcastSystem("SYSTEM: " + winner.getNickname() + "님이 새로운 호스트입니다.");
        startNewSession();
        Server.broadcastRoomList();
    }

    private void startNewSession() {
        if (host != null && guest != null) {
            this.currentSession = new GameSession(this, host, guest);
             broadcastSystem("SYSTEM: 새로운 게임을 시작할 수 있습니다. 준비 버튼을 눌러주세요.");
        } else {
            this.currentSession = new GameSession(this, host, null); 
        }
    }

    public void broadcastSystem(String message) {
        getAllUsers().forEach(user -> user.sendMessage(message));
    }
    public void broadcastSystemExcept(ClientHandler except, String message) {
        getAllUsers().stream()
                .filter(user -> user != except)
                .forEach(user -> user.sendMessage(message));
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
