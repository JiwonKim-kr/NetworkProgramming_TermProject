import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * 서버 측에서 각 클라이언트와의 통신을 담당하는 스레드 클래스입니다.
 * 클라이언트로부터 메시지를 수신하고, 이를 파싱하여 적절한 로직(Server, GameRoom)을 호출합니다.
 * PlayerConnection 인터페이스를 구현하여 게임 로직과의 결합도를 낮춥니다.
 */
public class ClientHandler extends Thread implements PlayerConnection {
    private Socket clientSocket;
    private PrintWriter out;    // 클라이언트로 메시지를 보내기 위한 스트림
    private BufferedReader in; // 클라이언트로부터 메시지를 읽기 위한 스트림
    private String nickname;
    private GameRoom currentRoom = null; // 현재 입장해 있는 게임방, 로비에 있으면 null
    
    /**
     * ClientHandler 생성자입니다.
     * @param socket 연결된 클라이언트의 소켓
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * 스레드의 메인 로직입니다.
     * 닉네임 설정을 처리하고, 이후 클라이언트로부터 메시지를 계속 수신합니다.
     */
    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // 닉네임 설정이 성공적으로 완료될 때까지 대기
            if (!handleNickname()) {
                return; // 닉네임 설정 실패 시 연결 종료
            }

            // 클라이언트로부터 메시지를 계속 읽고 처리
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleClientMessage(inputLine);
            }
        } catch (IOException e) {
            // 클라이언트와의 연결이 끊어졌을 때 발생하는 예외 처리
            System.out.println(nickname + " 클라이언트 연결 끊김: " + e.getMessage());
        } finally {
            // 연결 종료 시 자원 정리
            cleanup();
        }
    }

    /**
     * 클라이언트의 닉네임 설정을 처리합니다.
     * 중복 닉네임 검사를 통과할 때까지 클라이언트에게 재입력을 요청합니다.
     * @return 닉네임 설정 성공 시 true, 실패 시 false
     * @throws IOException 소켓 통신 중 오류 발생 시
     */
    private boolean handleNickname() throws IOException {
        while (true) {
            String requestedNickname = in.readLine();
            if (requestedNickname == null) {
                return false; // 클라이언트가 닉네임 입력 전에 연결을 끊음
            }

            if (!Server.isNicknameTaken(requestedNickname)) {
                this.nickname = requestedNickname;
                Server.addNickname(this.nickname);
                sendMessage(Protocol.NICKNAME_OK); // 닉네임 설정 성공 알림
                Server.broadcastToLobby(Protocol.SYSTEM + " " + nickname + "님이 로비에 입장했습니다.");
                sendRoomList(); // 새로 접속한 클라이언트에게 현재 방 목록 전송
                Server.broadcastRoomList(); // 다른 클라이언트들에게 접속자 목록 갱신 알림
                return true;
            } else {
                sendMessage(Protocol.NICKNAME_TAKEN); // 닉네임 중복 알림
            }
        }
    }

    /**
     * 클라이언트로부터 받은 메시지를 파싱하고, 현재 상태(로비/게임방)에 따라 처리합니다.
     * @param message 클라이언트가 보낸 메시지
     */
    private void handleClientMessage(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        if (currentRoom == null) { // 로비에 있을 때
            switch (command) {
                case Protocol.CREATE_ROOM:
                    Server.createGameRoom(payload, this);
                    break;
                case Protocol.JOIN_ROOM:
                    Server.joinGameRoom(payload, this);
                    break;
                case Protocol.LOBBY_CHAT:
                    String chatMsg = nickname + ": " + payload;
                    Server.broadcastToLobby(Protocol.LOBBY_CHAT + " " + chatMsg);
                    break;	
                case Protocol.CHANGE_NICKNAME:
                    handleChangeNickname(payload);
                    break;
            }
        } else { // 게임방에 있을 때
            switch (command) {
                case Protocol.CHAT:
                    currentRoom.broadcastChat(Protocol.CHAT + " [" + currentRoom.getTitle() + "] " + nickname + ": " + payload);
                    break;
                case Protocol.LEAVE_ROOM:
                    currentRoom.removePlayer(this);
                    break;
                default: // 그 외 게임 관련 명령어들은 GameRoom에 위임
                    currentRoom.handlePlayerCommand(this, message);
                    break;
            }
        }
    }

    /**
     * 닉네임 변경 요청을 처리합니다.
     * @param newNickname 변경할 새 닉네임
     */
    private void handleChangeNickname(String newNickname) {
        if (newNickname == null || newNickname.trim().isEmpty()) {
            sendMessage(Protocol.NICKNAME_CHANGE_FAILED + " 닉네임은 비워둘 수 없습니다.");
            return;
        }

        if (Server.isNicknameTaken(newNickname)) {
            sendMessage(Protocol.NICKNAME_CHANGE_FAILED + " 이미 사용 중인 닉네임입니다.");
            return;
        }

        String oldNickname = this.nickname;
        Server.removeNickname(oldNickname);
        this.nickname = newNickname;
        Server.addNickname(newNickname);

        sendMessage(Protocol.NICKNAME_CHANGED_OK + " " + newNickname);
        Server.broadcastToLobby(Protocol.SYSTEM + " " + oldNickname + "님이 " + newNickname + "(으)로 닉네임을 변경했습니다.");
        Server.broadcastRoomList(); // 닉네임 변경 시 모든 로비 유저의 목록을 갱신
    }

    /**
     * 현재 방 목록과 접속자 목록을 이 클라이언트에게만 전송합니다.
     */
    public void sendRoomList() {
        String roomListStr = Server.getGameRooms().values().stream()
            .map(room -> String.format("%s (%d/2) %s",
                room.getTitle(), room.getPlayerCount(), room.isGameInProgress() ? "[게임중]" : "[대기중]"))
            .collect(Collectors.joining(","));
        
        String userListStr = String.join(",", Server.getNicknames());
        String payload = roomListStr + "|" + userListStr;
        sendMessage(Protocol.UPDATE_ROOMLIST + " " + payload);
    }

    /**
     * 클라이언트 연결 종료 시 호출되어 서버의 관련 정보를 정리하고 소켓을 닫습니다.
     */
    private void cleanup() {
        if (nickname != null) {
            Server.removeClient(this); // 서버의 클라이언트 목록에서 제거
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- PlayerConnection 인터페이스 구현 ---
    @Override
    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    // --- Getter/Setter ---
    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }
}
