import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {

    private String title;
    private String password;
    private int maxPlayers;
    private ClientHandler host;
    private ClientHandler guest;
    private List<ClientHandler> spectators = Collections.synchronizedList(new ArrayList<>());
    private GameSession currentSession;
    private boolean isPrivate;
    

    public GameRoom(String title, String password, int maxPlayers, ClientHandler host) {
        this.title = title;
        this.password = password;
        this.maxPlayers = maxPlayers;
        this.host = host;
        this.isPrivate = isPrivate;
        this.password = password;
        this.currentSession = new GameSession(this, host, null);
        host.sendMessage(Protocol.JOIN_SUCCESS + " " + this.title);
    }
    public boolean isPrivateRoom() {
        return isPrivate;
    }

    public String getPassword() {
        return password;
    }
    public synchronized void handlePlayerCommand(ClientHandler player, String message) {
        if (currentSession != null) {
            currentSession.processCommand(player, message);
        }
    }

    public synchronized void addPlayer(ClientHandler player, String password) {
        if (getPlayerCount() >= maxPlayers) {
            player.sendMessage(Protocol.ERROR + " 방이 꽉 찼습니다.");
            return;
        }
        if (!this.password.isEmpty() && !this.password.equals(password)) {
            player.sendMessage(Protocol.ERROR + " 비밀번호가 일치하지 않습니다.");
            return;
        }

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

        player.setCurrentRoom(null);
        player.sendMessage(Protocol.GOTO_LOBBY);

        if (wasCorePlayer && isGameInProgress()) {
            currentSession.abortGame("상대방이 퇴장하여 게임이 종료되었습니다.", player);
        }

        if (player == host) host = null;
        else if (player == guest) guest = null;
        else spectators.remove(player);

        if (host == null && guest == null && spectators.isEmpty()) {
            Server.removeGameRoom(this.title);
            return;
        }

        boolean hostChanged = false;
        if (host == null) {
            if (guest != null) {
                host = guest;
                guest = null;
            } else if (!spectators.isEmpty()) {
                host = spectators.remove(0);
            }
            hostChanged = true;
        }

        if (guest == null && !spectators.isEmpty()) {
            guest = spectators.remove(0);
            broadcastSystem("SYSTEM: " + guest.getNickname() + "님이 새로운 GUEST가 되었습니다.");
        }

        if (hostChanged) {
            broadcastSystem("SYSTEM: 호스트가 " + host.getNickname() + "님으로 변경되었습니다.");
        }
        if (wasCorePlayer) {
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

    public void broadcastSystem(String message) { getAllUsers().forEach(user -> user.sendMessage(message)); }
    public void broadcastSystemExcept(ClientHandler except, String message) { getAllUsers().stream().filter(user -> user != except).forEach(user -> user.sendMessage(message)); }
    public void broadcastChat(String message) { getAllUsers().forEach(user -> user.sendMessage(message)); }

    public synchronized List<ClientHandler> getAllUsers() {
        List<ClientHandler> allUsers = new ArrayList<>();
        if (host != null) allUsers.add(host);
        if (guest != null) allUsers.add(guest);
        allUsers.addAll(spectators);
        return allUsers;
    }

    public boolean isGameInProgress() { return currentSession != null && currentSession.getGameState() == GameLogic.GameState.IN_PROGRESS; }
    public String getTitle() { return title; }
    public int getPlayerCount() { return getAllUsers().size(); }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isPrivate() { return password != null && !password.isEmpty(); }
}
