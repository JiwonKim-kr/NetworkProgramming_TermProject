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
     * 모든 로비 유저에게 현재 게임 방 목록을 브로드캐스트합니다.
     */
    public static void broadcastRoomList() {
        String roomListStr = gameRooms.values().stream()
        .map(room -> String.format("%s (%d/8) %s",
        	room.getTitle(), room.getPlayerCount(), room.isGameInProgress() ? "[게임중]" : "[대기중]"))
        	.collect(Collectors.joining(","));

        broadcastToLobby(Protocol.UPDATE_ROOMLIST + " " + roomListStr);
    }

    /**
     * 클라이언트 연결 종료 시 호출되어 관련 정보를 정리합니다.
     */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getNickname() != null) {
            nicknames.remove(client.getNickname());
            // 클라이언트가 방에 있었다면, 방에서 나가는 처리를 위임
            if (client.getCurrentRoom() != null) {
                client.getCurrentRoom().removePlayer(client);
            } else {
                // 로비에 있었다면, 로비에 퇴장 메시지 전송
                broadcastToLobby(Protocol.SYSTEM + " " + client.getNickname() + "님이 퇴장했습니다.");
            }
        }
    }

    public static synchronized boolean isNicknameTaken(String nickname) {
        return nicknames.contains(nickname);
    }

    public static synchronized void addNickname(String nickname) {
        nicknames.add(nickname);
    }

    public static void createGameRoom(String payload, ClientHandler host) {
    	

    	String[] p = payload.split("\\|");

    	String title     = p.length > 0 ? p[0] : "";
    	boolean isPrivate = p.length > 1 && p[1].equals("1");
    	String password   = p.length > 2 ? p[2] : "";


        GameRoom newRoom = new GameRoom(title, host, isPrivate, password);
    	if (title == null || title.isBlank()) {
            host.sendMessage(Protocol.ERROR + " 방 제목은 비워둘 수 없습니다.");
            return;
        }
        if (gameRooms.containsKey(title)) {
            host.sendMessage(Protocol.ERROR + " 방 생성 실패: 이미 존재하는 방 제목입니다.");
            return;
        }

        gameRooms.put(title, newRoom);	
        host.setCurrentRoom(newRoom);
        // GameRoom에 진입하는 것은 ClientHandler가 처리, 여기서는 방 목록 갱신만 처리
        broadcastRoomList();
        host.sendMessage(Protocol.JOIN_SUCCESS + " " + title);
    }

    public static void joinGameRoom(String payload, ClientHandler player) {

        String[] p = payload.split("\\|");
        String title = p[0];
        String pw = (p.length > 1) ? p[1] : "";

        GameRoom room = gameRooms.get(title);
        if (room.isPrivateRoom()) {
            if (!room.getPassword().equals(pw)) {
                player.sendMessage(Protocol.ERROR + " 비밀번호가 틀렸습니다.");
                return;
            }
        }

        room.addPlayer(player);
        player.setCurrentRoom(room);
        player.sendMessage(Protocol.JOIN_SUCCESS + " " + title);
    }

    public static void removeGameRoom(String title) {
        gameRooms.remove(title);
        broadcastRoomList();
    }

    public static ConcurrentHashMap<String, GameRoom> getGameRooms() {
        return gameRooms;
    }
    public static GameRoom getRoom(String title) {
        return gameRooms.get(title);
    }
}
