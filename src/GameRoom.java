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
    }

    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        if (currentSession != null) {
            currentSession.processCommand(player, message);
        }
    }

    public synchronized void addPlayer(ClientHandler player) {
        if (this.guest == null) {
            this.guest = player;
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 GUEST로 입장했습니다.");
            startNewSession();
        } else {
            spectators.add(player);
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 관전자로 입장했습니다.");
        }
        
        if (isGameInProgress()) {
            currentSession.broadcastState();
        }
        Server.broadcastRoomList();
    }

    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        boolean wasPlayer = (player == host || player == guest);

        // 게임 중에 핵심 플레이어가 나갔을 경우, 게임 세션만 종료
        if (wasPlayer && isGameInProgress()) {
            currentSession.abortGame("상대방이 퇴장하여 게임이 종료되었습니다.");
        }

        // 역할 재할당
        if (player == host) {
            host = guest;
            guest = spectators.isEmpty() ? null : spectators.remove(0);
            if (host != null) broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        } else if (player == guest) {
            guest = spectators.isEmpty() ? null : spectators.remove(0);
        } else {
            spectators.remove(player);
        }

        // 방 상태 최종 결정
        if (host == null) {
            Server.removeGameRoom(this.title);
        } else {
            if (wasPlayer) {
                startNewSession(); // 플레이어가 나갔으므로 새 세션 준비
            }
            broadcastSystem("SYSTEM: " + leavingNickname + "님이 퇴장했습니다.");
            Server.broadcastRoomList();
        }
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
