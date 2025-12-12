import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 게임 클라이언트의 네트워크 통신을 담당하는 클래스입니다.
 * 서버에 연결하고, 메시지를 송수신하며, 수신된 메시지를 GameController에 전달합니다.
 */
public class GameClient {

    private static final String SERVER_ADDRESS = "localhost"; // 서버 주소
    private static final int SERVER_PORT = 12345;             // 서버 포트
    private Socket socket;
    private BufferedReader in;  // 서버로부터 메시지를 읽기 위한 스트림
    private PrintWriter out;    // 서버로 메시지를 쓰기 위한 스트림
    private String nickname;    // 클라이언트의 닉네임
    private volatile String playerRole; // 게임 내에서 할당받은 역할 (P1 또는 P2), volatile로 가시성 보장
    private final Consumer<String> onMessageReceived; // 서버로부터 메시지 수신 시 호출될 콜백 함수

    /**
     * GameClient 생성자입니다.
     * @param onMessageReceived 서버로부터 메시지를 받았을 때 처리할 로직을 담은 Consumer
     */
    public GameClient(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    /**
     * 서버에 연결을 시도하고 메시지 수신 스레드를 시작합니다.
     * @param nickname 서버에 등록할 클라이언트의 닉네임
     */
    public void start(String nickname) {
        this.nickname = nickname;
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush 활성화

            // 서버로부터 메시지를 지속적으로 수신하는 별도의 스레드 시작
            new Thread(this::handleServerConnection).start();

        } catch (IOException e) {
            // 연결 실패 시 오류 메시지를 컨트롤러에 전달
            onMessageReceived.accept(Protocol.ERROR + " 서버에 연결할 수 없습니다.");
        }
    }

    /**
     * 서버로부터 메시지를 지속적으로 읽고 처리하는 스레드의 로직입니다.
     */
    private void handleServerConnection() {
        try {
            // 서버에 닉네임 전송
            out.println(nickname);

            String message;
            // 서버로부터 메시지를 한 줄씩 읽음
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(" ", 2);
                String command = parts[0];

                // 플레이어 역할 할당 메시지는 GameClient 내부에서 처리
                if (command.equals(Protocol.ASSIGN_ROLE)) {
                    this.playerRole = parts[1];
                } else {
                    // 그 외의 메시지는 등록된 콜백 함수를 통해 GameController로 전달
                    onMessageReceived.accept(message);
                }
            }
        } catch (IOException e) {
            // 서버와의 연결이 끊어졌을 때 오류 처리
            onMessageReceived.accept(Protocol.ERROR + " 서버와 연결이 끊어졌습니다.");
        } finally {
            // 연결 종료 시 자원 정리
            closeConnection();
        }
    }

    /**
     * 서버로 메시지를 전송합니다.
     * @param message 서버로 보낼 메시지
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * 클라이언트 소켓 연결을 닫아 자원을 해제합니다.
     */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // 연결 종료 중 발생하는 예외는 무시 (이미 연결이 끊어졌을 가능성 높음)
        }
    }

    // --- Getter/Setter 메서드 ---
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
