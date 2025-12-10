import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 십이장기 게임 서버의 메인 클래스입니다.
 * 클라이언트의 연결을 수락하고, 각 클라이언트에 대한 ClientHandler 스레드를 생성합니다.
 * 전체 클라이언트, 닉네임, 게임방 목록을 정적(static) 멤버로 관리합니다.
 */
public class Server {

    private static final int PORT = 12345;
    // 연결된 모든 클라이언트 핸들러 리스트
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    // 사용 중인 모든 닉네임 리스트
    private static final List<String> nicknames = Collections.synchronizedList(new ArrayList<>());
    // 생성된 모든 게임방 맵 (Key: 방 이름)
    private static final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();

    /**
     * 서버 애플리케이션의 진입점입니다.
     */
    public static void main(String[] args) {
        // try-with-resources를 사용하여 서버 소켓 자동 해제
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 " + PORT + " 포트에서 시작되었습니다.");
            while (true) {
                // 클라이언트의 연결을 기다림
                Socket clientSocket = serverSocket.accept();
                // 연결된 클라이언트를 처리할 새 스레드 생성 및 시작
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
     * @param message 보낼 메시지
     */
    public static void broadcastToLobby(String message) {
        synchronized (clients) {
            clients.stream()
                   .filter(c -> c.getCurrentRoom() == null) // 현재 방이 없는(로비에 있는) 클라이언트만 필터링
                   .forEach(c -> c.sendMessage(message));
        }
    }

    /**
     * 현재 생성된 방 목록과 접속자 목록을 로비의 모든 클라이언트에게 브로드캐스트합니다.
     */
    public static void broadcastRoomList() {
        // 방 목록을 문자열로 직렬화
        String roomListStr = gameRooms.values().stream()
            .map(room -> String.format("%s (%d/%d) %s %s",
                room.getTitle(),
                room.getPlayerCount(),
                room.getMaxPlayers(),
                room.isGameInProgress() ? "[게임중]" : "[대기중]",
                room.isPrivate() ? "[비밀방]" : ""))
            .collect(Collectors.joining(","));
        
        // 접속자 목록을 문자열로 직렬화
        String userListStr;
        synchronized (nicknames) {
            userListStr = String.join(",", nicknames);
        }
        
        // 방 목록과 접속자 목록을 합쳐서 전송
        String payload = roomListStr + "|" + userListStr;
        broadcastToLobby(Protocol.UPDATE_ROOMLIST + " " + payload);
    }

    /**
     * 클라이언트 연결이 종료되었을 때 호출되어 관련 정보를 정리합니다.
     * @param client 정리할 클라이언트 핸들러
     */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getNickname() != null) {
            nicknames.remove(client.getNickname());
            // 클라이언트가 방에 있었다면, 방에서 내보내는 처리를 함
            if (client.getCurrentRoom() != null) {
                client.getCurrentRoom().removePlayer(client);
            } else {
                // 로비에 있었다면, 로비에 퇴장 메시지 전송
                broadcastToLobby(Protocol.SYSTEM + " " + client.getNickname() + "님이 퇴장했습니다.");
            }
        }
        // 클라이언트가 나갔으므로 전체 방/접속자 목록을 갱신
        broadcastRoomList();
    }

    // 닉네임 관련 동기화 메서드들
    public static synchronized boolean isNicknameTaken(String nickname) { return nicknames.contains(nickname); }
    public static synchronized void addNickname(String nickname) { nicknames.add(nickname); }
    public static synchronized void removeNickname(String nickname) { nicknames.remove(nickname); }
    public static List<String> getNicknames() { return nicknames; }

    /**
     * 클라이언트의 요청에 따라 새로운 게임방을 생성합니다.
     * @param payload 방 정보 (제목#비밀번호#최대인원)
     * @param host 방을 생성한 클라이언트 핸들러
     */
    public static void createGameRoom(String payload, ClientHandler host) {
        String[] parts = payload.split("#", 3);
        String title = parts[0];
        String password = parts[1];
        int maxPlayers = Integer.parseInt(parts[2]);

        // 방 제목 유효성 검사
        if (title.isBlank()) {
            host.sendMessage(Protocol.ERROR + " 방 제목은 비워둘 수 없습니다.");
            return;
        }
        // 중복된 방 제목 검사
        if (gameRooms.containsKey(title)) {
            host.sendMessage(Protocol.ERROR + " 방 생성 실패: 이미 존재하는 방 제목입니다.");
            return;
        }
        
        GameRoom newRoom = new GameRoom(title, password, maxPlayers, host);
        gameRooms.put(title, newRoom);
        host.setCurrentRoom(newRoom); // 방 생성자를 해당 방으로 이동
        broadcastRoomList(); // 방 목록 갱신
    }

    /**
     * 클라이언트를 지정된 게임방에 참가시킵니다.
     * @param payload 참가 정보 (방제목#비밀번호)
     * @param player 참가하려는 클라이언트 핸들러
     */
    public static void joinGameRoom(String payload, ClientHandler player) {
        String[] parts = payload.split("#", 2);
        String title = parts[0];
        String password = (parts.length > 1) ? parts[1] : "";

        GameRoom room = gameRooms.get(title);
        if (room != null) {
            room.addPlayer(player, password); // 실제 입장 처리는 GameRoom에 위임
        } else {
            player.sendMessage(Protocol.ERROR + " 방 입장 실패: 존재하지 않는 방입니다.");
        }


        player.setCurrentRoom(room);
        player.sendMessage(Protocol.JOIN_SUCCESS + " " + title);
    }

    /**
     * 게임방을 서버에서 제거합니다. (주로 방이 비었을 때 호출됨)
     * @param title 제거할 방의 제목
     */
    public static void removeGameRoom(String title) {
        gameRooms.remove(title);
        broadcastRoomList(); // 방 목록 갱신
    }

    public static ConcurrentHashMap<String, GameRoom> getGameRooms() { return gameRooms; }
}
