import javax.swing.*;
import java.awt.*;

/**
 * 게임방의 채팅 UI를 구성하는 패널입니다.
 * 채팅 메시지를 보여주는 텍스트 영역과 메시지를 입력하는 필드로 구성됩니다.
 */
public class ChatPanel extends JPanel {
    private final GameController controller;
    private final JTextArea chatArea;

    /**
     * ChatPanel 생성자입니다.
     * @param controller 채팅 메시지 전송을 처리할 게임 컨트롤러
     */
    public ChatPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(250, 0)); // 채팅 패널의 너비 고정

        // 채팅 내용이 표시될 텍스트 영역
        chatArea = new JTextArea();
        chatArea.setEditable(false); // 사용자가 직접 수정 불가
        chatArea.setLineWrap(true);  // 자동 줄바꿈 활성화
        this.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 채팅을 입력하는 텍스트 필드
        JTextField chatField = new JTextField();
        chatField.addActionListener(e -> { // Enter 키를 눌렀을 때 이벤트 처리
            String msg = chatField.getText();
            if (!msg.isEmpty()) {
                controller.sendChatMessage(msg); // 컨트롤러를 통해 메시지 전송
                chatField.setText(""); // 입력 필드 초기화
            }
        });
        this.add(chatField, BorderLayout.SOUTH);
    }

    /**
     * 채팅 영역에 새로운 메시지를 추가합니다.
     * @param message 추가할 메시지
     */
    public void appendChatMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * 채팅 영역의 모든 내용을 지웁니다.
     * 주로 방에서 나가거나 새 게임이 시작될 때 호출됩니다.
     */
    public void clearChat() {
        chatArea.setText("");
    }
    
    /**
     * 채팅 영역을 초기화합니다. clearChat과 기능적으로 동일합니다.
     */
    public void resetChat() {
        chatArea.setText("");
    }

    public GameController getController() {
        return controller;
    }
}
