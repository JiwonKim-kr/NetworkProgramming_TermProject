import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
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

            if (!handleInitialNickname()) {
                return;
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

    private boolean handleInitialNickname() throws IOException {
        while (true) {
            String requestedNickname = in.readLine();
            if (requestedNickname == null) return false;

            if (!Server.isNicknameTaken(requestedNickname)) {
                this.nickname = requestedNickname;
                Server.addNickname(this.nickname);
                out.println("NICKNAME_OK");
                Server.broadcastToLobby("SYSTEM: " + nickname + "님이 로비에 입장했습니다.");
                sendRoomList();
                return true;
            } else {
                out.println("NICKNAME_TAKEN");
            }
        }
    }

    private void handleClientMessage(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String payload = parts.length > 1 ? parts[1] : "";

        if (currentRoom == null) { // 로비에 있을 때
            switch (command) {
                case "CREATE_ROOM":
                    Server.createGameRoom(payload, this);
                    break;
                case "JOIN_ROOM":
                    Server.joinGameRoom(payload, this);
                    break;
                case "CHAT":
                    Server.broadcastToLobby("CHAT: [로비] " + nickname + ": " + payload);
                    break;
                case "CHANGE_NICKNAME":
                    handleChangeNickname(payload);
                    break;
            }
        } else { // 방에 있을 때
            switch (command) {
                case "CHAT":
                    currentRoom.broadcastChat("CHAT: [" + currentRoom.getTitle() + "] " + nickname + ": " + payload);
                    break;
                case "LEAVE_ROOM":
                    currentRoom.removePlayer(this);
                    this.currentRoom = null;
                    sendMessage("GOTO_LOBBY");
                    sendRoomList();
                    break;
                default:
                    currentRoom.handlePlayerCommand(this, message);
                    break;
            }
        }
    }

    private void handleChangeNickname(String newNickname) {
        if (Server.changeNickname(this.nickname, newNickname)) {
            String oldNickname = this.nickname;
            this.nickname = newNickname;
            sendMessage("NICKNAME_CHANGED " + newNickname);
            Server.broadcastToLobby("SYSTEM: " + oldNickname + "님의 닉네임이 " + newNickname + "(으)로 변경되었습니다.");
        } else {
            sendMessage("NICKNAME_TAKEN");
        }
    }

    public void sendRoomList() {
        String roomListStr = Server.getGameRooms().values().stream()
            .map(room -> String.format("%s (%d/8) %s",
                room.getTitle(), room.getPlayerCount(), room.isGameInProgress() ? "[게임중]" : "[대기중]"))
            .collect(Collectors.joining(","));
        sendMessage("UPDATE_ROOMLIST " + roomListStr);
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

    public void sendMessage(String message) {
        out.println(message);
    }

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
