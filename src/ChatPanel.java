import javax.swing.*;
import java.awt.*;

/**
 * 게임방의 채팅 UI를 구성하는 패널입니다.
 */
public class ChatPanel extends JPanel {
    private final GameController controller;
    private final JTextArea chatArea;

    public ChatPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(250, 0));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        this.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JTextField chatField = new JTextField();
        chatField.addActionListener(e -> {
            String msg = chatField.getText();
            if (!msg.isEmpty()) {
                controller.sendChatMessage(msg);
                chatField.setText("");
            }
        });
        this.add(chatField, BorderLayout.SOUTH);
    }

    public void appendChatMessage(String message) {
        chatArea.append(message + "\\n");
    }
}
