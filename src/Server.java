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

    /**
     * 로비에 있는 모든 클라이언트에게 메시지를 브로드캐스트합니다.
     */
    public static void broadcastToLobby(String message) {
        synchronized (clients) {
            clients.stream()
                   .filter(c -> c.getCurrentRoom() == null)
                   .forEach(c -> c.sendMessage(message));
        }
    }

    /**
     * 모든 로비 유저에게 현재 게임 방 목록과 전체 접속자 목록을 브로드캐스트합니다.
     */
    public static void broadcastRoomList() {
        String roomListStr = gameRooms.values().stream()
            .map(room -> String.format("%s (%d/2) %s",
                room.getTitle(), room.getPlayerCount(), room.isGameInProgress() ? "[게임중]" : "[대기중]"))
            .collect(Collectors.joining(","));
        
        String userListStr;
        synchronized (nicknames) {
            userListStr = String.join(",", nicknames);
        }
        
        String payload = roomListStr + "|" + userListStr;

        broadcastToLobby(Protocol.UPDATE_ROOMLIST + " " + payload);
    }

    /**
     * 클라이언트 연결 종료 시 호출되어 관련 정보를 정리합니다.
     */
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
        // 유저가 나갈 때마다 방 목록/인원 수 갱신
        broadcastRoomList();
    }

    public static synchronized boolean isNicknameTaken(String nickname) {
        return nicknames.contains(nickname);
    }

    public static synchronized void addNickname(String nickname) {
        nicknames.add(nickname);
    }

    public static synchronized void removeNickname(String nickname) {
        nicknames.remove(nickname);
    }

    public static List<String> getNicknames() {
        return nicknames;
    }

    public static void createGameRoom(String title, ClientHandler host) {
        if (title == null || title.isBlank()) {
            host.sendMessage(Protocol.ERROR + " 방 제목은 비워둘 수 없습니다.");
            return;
        }
        if (gameRooms.containsKey(title)) {
            host.sendMessage(Protocol.ERROR + " 방 생성 실패: 이미 존재하는 방 제목입니다.");
            return;
        }
        
        GameRoom newRoom = new GameRoom(title, host);
        gameRooms.put(title, newRoom);
        host.setCurrentRoom(newRoom);
        // host.sendMessage(Protocol.JOIN_SUCCESS + " " + title); // GameRoom 생성자에서 처리하므로 중복
        broadcastRoomList();
    }

    public static void joinGameRoom(String title, ClientHandler player) {
        GameRoom room = gameRooms.get(title);
        if (room != null) {
            room.addPlayer(player);
        } else {
            player.sendMessage(Protocol.ERROR + " 방 입장 실패: 존재하지 않는 방입니다.");
        }
    }

    public static void removeGameRoom(String title) {
        gameRooms.remove(title);
        broadcastRoomList();
    }

    public static ConcurrentHashMap<String, GameRoom> getGameRooms() {
        return gameRooms;
    }
}
