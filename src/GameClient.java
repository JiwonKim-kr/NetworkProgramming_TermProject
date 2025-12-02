import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class GameClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private volatile String playerRole; // "P1" or "P2"
    private final Consumer<String> onMessageReceived;

    public GameClient(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void start(String nickname) {
        this.nickname = nickname;
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(this::handleServerConnection).start();

        } catch (IOException e) {
            onMessageReceived.accept(Protocol.ERROR + " 서버에 연결할 수 없습니다.");
        }
    }

    private void handleServerConnection() {
        try {
            out.println(nickname);

            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" ", 2);
                String command = parts[0];

                if (command.equals(Protocol.ASSIGN_ROLE)) {
                    this.playerRole = parts[1];
                } else {
                    onMessageReceived.accept(message);
                }
            }
        } catch (IOException e) {
            onMessageReceived.accept(Protocol.ERROR + " 서버와 연결이 끊어졌습니다.");
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public String getPlayerRole() {
        return playerRole;
    }
    
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
