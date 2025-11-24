/**
 * 클라이언트와의 연결을 추상화하는 인터페이스.
 * GameRoom, GameSession 등 게임 로직이 구체적인 네트워크 구현(ClientHandler)에
 * 의존하지 않도록 분리하는 역할을 합니다. (의존 역전 원칙)
 */
public interface PlayerConnection {
    /**
     * 이 연결을 통해 클라이언트에게 메시지를 보냅니다.
     * @param message 보낼 메시지
     */
    void sendMessage(String message);

    /**
     * 이 연결에 해당하는 플레이어의 닉네임을 반환합니다.
     * @return 플레이어 닉네임
     */
    String getNickname();
}
