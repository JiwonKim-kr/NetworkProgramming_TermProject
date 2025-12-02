import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;

public class ClientHandler extends Thread implements PlayerConnection {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;
    private GameRoom currentRoom = null;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            if (!handleNickname()) {
                return; // 닉네임 설정 실패 시 연결 종료
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleClientMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println(nickname + " 클라이언트 연결 끊김: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean handleNickname() throws IOException {
        while (true) {
            String requestedNickname = in.readLine();
            if (requestedNickname == null) {
                return false; // 클라이언트가 닉네임 입력 전에 연결을 끊음
            }

            if (!Server.isNicknameTaken(requestedNickname)) {
                this.nickname = requestedNickname;
                Server.addNickname(this.nickname);
                sendMessage(Protocol.NICKNAME_OK);
                Server.broadcastToLobby(Protocol.SYSTEM + " " + nickname + "님이 로비에 입장했습니다.");
                sendRoomList();
                return true;
            } else {
                sendMessage(Protocol.NICKNAME_TAKEN);
            }
        }
    }

    private void handleClientMessage(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        if (currentRoom == null) { // In Lobby
            switch (command) {
                case Protocol.CREATE_ROOM:
                    Server.createGameRoom(payload, this);
                    break;
                case Protocol.JOIN_ROOM:
                    Server.joinGameRoom(payload, this);
                    break;
                case Protocol.CHAT:
                    Server.broadcastToLobby(Protocol.CHAT + " [로비] " + nickname + ": " + payload);
                    break;
                case Protocol.CHANGE_NICKNAME:
                    handleChangeNickname(payload);
                    break;
            }
        } else { // In Room
            switch (command) {
                case Protocol.CHAT:
                    currentRoom.broadcastChat(Protocol.CHAT + " [" + currentRoom.getTitle() + "] " + nickname + ": " + payload);
                    break;
                case Protocol.LEAVE_ROOM:
                    currentRoom.removePlayer(this);
                    this.currentRoom = null;
                    sendMessage(Protocol.GOTO_LOBBY);
                    sendRoomList();
                    break;
                default: // Game commands (READY, MOVE, PLACE, UNDO_REQUEST, etc.)
                    currentRoom.handlePlayerCommand(this, message);
                    break;
            }
        }
    }

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
    }


    public void sendRoomList() {
        String roomListStr = Server.getGameRooms().values().stream()
            .map(room -> String.format("%s (%d/8) %s",
                room.getTitle(), room.getPlayerCount(), room.isGameInProgress() ? "[게임중]" : "[대기중]"))
            .collect(Collectors.joining(","));
        sendMessage(Protocol.UPDATE_ROOMLIST + " " + roomListStr);
    }

    private void cleanup() {
        if (nickname != null) {
            Server.removeClient(this);
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }
}
