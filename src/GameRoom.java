import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GameRoom {

    private String title;
    private ClientHandler host;
    private ClientHandler guest;
    private ClientHandler player1; // P1
    private ClientHandler player2; // P2
    private List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameSession currentSession;

    public GameRoom(String title, ClientHandler host) {
        this.title = title;
        this.host = host;
        // 처음에는 플레이어가 한 명이므로, guest가 없는 세션을 생성
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
            // 호스트와 게스트가 모두 준비되면 새 세션 시작
            startNewSession(); 
        } else {
            spectators.add(player);
            broadcastSystem("SYSTEM: " + player.getNickname() + "님이 관전자로 입장했습니다.");
        }
        
        // 게임이 이미 진행 중이라면, 새로 들어온 유저에게 현재 게임 상태 전송
        if (isGameInProgress()) {
            currentSession.broadcastState();
        }
        Server.broadcastRoomList();
    }

    public synchronized void removePlayer(ClientHandler player) {
        String leavingNickname = player.getNickname();
        boolean wasPlayer = (player == host || player == guest);

        // 게임 중에 핵심 플레이어가 나갔을 경우, 세션 종료
        if (wasPlayer && isGameInProgress()) {
            ClientHandler winner = (player == host) ? guest : host;
            if (winner != null) {
                currentSession.endGame(winner, "상대방이 퇴장하여 게임에서 승리했습니다.");
            }
        }

        if (player == host) {
            host = guest;
            guest = spectators.isEmpty() ? null : spectators.remove(0);
            if (host != null) broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        } else if (player == guest) {
            guest = spectators.isEmpty() ? null : spectators.remove(0);
        } else {
            spectators.remove(player);
        }

        if (host == null) {
            // 방에 아무도 없으면 방 제거
            Server.removeGameRoom(this.title);
        } else {
            // 플레이어가 나갔으므로 새 세션 준비
            if (wasPlayer) startNewSession(); 
            broadcastSystem("SYSTEM: " + leavingNickname + "님이 퇴장했습니다.");
            Server.broadcastRoomList();
        }
    }

    /**
     * 게임 세션이 종료되었을 때 호출되는 콜백 메소드
     * @param winner 승리한 플레이어
     */
    public void onSessionFinished(ClientHandler winner) {
        ClientHandler loser = (winner == host) ? guest : host;
        
        // 승자가 호스트, 패자가 게스트가 됨
        this.host = winner;
        this.guest = loser;
        
        broadcastSystem("SYSTEM: " + winner.getNickname() + "님이 새로운 호스트입니다.");
        // 새로운 게임을 위한 세션을 준비
        startNewSession();
        Server.broadcastRoomList();
    }

    /**
     * 새로운 게임 세션을 시작 (플레이어가 변경되었을 때 호출)
     */
    private void startNewSession() {
        if (host != null && guest != null) {
            this.currentSession = new GameSession(this, host, guest);
             broadcastSystem("SYSTEM: 새로운 게임을 시작할 수 있습니다. 준비 버튼을 눌러주세요.");
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
