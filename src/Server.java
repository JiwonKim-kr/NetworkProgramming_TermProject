import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Server {

    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> nicknames = Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 " + PORT + " 포트에서 시작되었습니다.");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientThread = new ClientHandler(clientSocket);
                clients.add(clientThread);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("서버 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void broadcastToLobby(String message) {
        synchronized (clients) {
            clients.stream()
                   .filter(c -> c.getCurrentRoom() == null)
                   .forEach(c -> c.sendMessage(message));
        }
    }

    public static void broadcastRoomList() {
        String roomListStr = gameRooms.values().stream()
            .map(room -> String.format("%s (%d/%d) %s %s",
                room.getTitle(),
                room.getPlayerCount(),
                room.getMaxPlayers(),
                room.isGameInProgress() ? "[게임중]" : "[대기중]",
                room.isPrivate() ? "[비밀방]" : ""))
            .collect(Collectors.joining(","));
        
        String userListStr;
        synchronized (nicknames) {
            userListStr = String.join(",", nicknames);
        }
        
        String payload = roomListStr + "|" + userListStr;
        broadcastToLobby(Protocol.UPDATE_ROOMLIST + " " + payload);
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getNickname() != null) {
            nicknames.remove(client.getNickname());
            if (client.getCurrentRoom() != null) {
                client.getCurrentRoom().removePlayer(client);
            } else {
                broadcastToLobby(Protocol.SYSTEM + " " + client.getNickname() + "님이 퇴장했습니다.");
            }
        }
        broadcastRoomList();
    }

    public static synchronized boolean isNicknameTaken(String nickname) { return nicknames.contains(nickname); }
    public static synchronized void addNickname(String nickname) { nicknames.add(nickname); }
    public static synchronized void removeNickname(String nickname) { nicknames.remove(nickname); }
    public static List<String> getNicknames() { return nicknames; }

    public static void createGameRoom(String payload, ClientHandler host) {
        String[] parts = payload.split("#", 3);
        String title = parts[0];
        String password = parts[1];
        int maxPlayers = Integer.parseInt(parts[2]);

        if (title.isBlank()) {
            host.sendMessage(Protocol.ERROR + " 방 제목은 비워둘 수 없습니다.");
            return;
        }
        if (gameRooms.containsKey(title)) {
            host.sendMessage(Protocol.ERROR + " 방 생성 실패: 이미 존재하는 방 제목입니다.");
            return;
        }
        
        GameRoom newRoom = new GameRoom(title, password, maxPlayers, host);
        gameRooms.put(title, newRoom);
        host.setCurrentRoom(newRoom);
        broadcastRoomList();
    }

    public static void joinGameRoom(String payload, ClientHandler player) {
        String[] parts = payload.split("#", 2);
        String title = parts[0];
        String password = (parts.length > 1) ? parts[1] : "";

        GameRoom room = gameRooms.get(title);
        if (room != null) {
            room.addPlayer(player, password);
        } else {
            player.sendMessage(Protocol.ERROR + " 방 입장 실패: 존재하지 않는 방입니다.");
        }

        
        player.setCurrentRoom(room);
        player.sendMessage(Protocol.JOIN_SUCCESS + " " + title);
    }

    public static void removeGameRoom(String title) {
        gameRooms.remove(title);
        broadcastRoomList();
    }

    public static ConcurrentHashMap<String, GameRoom> getGameRooms() { return gameRooms; }
}
