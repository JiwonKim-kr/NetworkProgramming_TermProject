import javax.swing.*;
import java.awt.*;

/**
 * 게임 로비 화면을 구성하는 패널입니다.
 * 방 목록, 서버 정보, 로비 채팅 기능을 제공합니다.
 */
public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] lobbyButtons = new JButton[3][2]; // 3x2로 크기 변경
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField chatInputField;

    public LobbyPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 메인 컨텐츠 패널 (방 목록 + 로비 채팅)
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.add(createLobbyGridPanel(), BorderLayout.CENTER);
        mainContentPanel.add(createLobbyChatPanel(), BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);
        this.add(createSideInfoPanel(), BorderLayout.EAST);
    }

    private JPanel createLobbyGridPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel lobbyGrid = new JPanel(new GridLayout(3, 2, 8, 8)); // 3x2 그리드
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 2; c++) {
                JButton b = new JButton("빈 방");
                b.setFocusable(false);
                b.setEnabled(false);
                b.addActionListener(e -> {
                    JButton src = (JButton) e.getSource();
                    String title = (String) src.getClientProperty("roomTitle");
                    if (title != null && !title.isBlank()) {
                        controller.joinRoom(title);
                    }
                });
                lobbyButtons[r][c] = b;
                lobbyGrid.add(b);
            }
        }
        panel.add(new JScrollPane(lobbyGrid), BorderLayout.CENTER);
        panel.add(createBottomButtonPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createBottomButtonPanel() {
        JPanel panel = new JPanel();
        JButton createRoomButton = new JButton("방 만들기");
        createRoomButton.addActionListener(e -> {
            String roomTitle = JOptionPane.showInputDialog(this, "방 제목을 입력하세요:");
            if (roomTitle != null && !roomTitle.trim().isEmpty()) {
                controller.createRoom(roomTitle);
            }
        });
        panel.add(createRoomButton);
        return panel;
    }

    private JPanel createSideInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("접속자 목록"));
        panel.setPreferredSize(new Dimension(200, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLobbyChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("로비 채팅"));

        chatArea = new JTextArea(8, 30);
        chatArea.setEditable(false);
        panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        chatInputField = new JTextField();
        chatInputField.addActionListener(e -> sendChatMessage());
        JButton sendButton = new JButton("전송");
        sendButton.addActionListener(e -> sendChatMessage());

        inputPanel.add(chatInputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void sendChatMessage() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            controller.sendChatMessage(message);
            chatInputField.setText("");
        }
    }

    public void updateRoomList(String payload) {
        SwingUtilities.invokeLater(() -> {
            String[] payloadParts = payload.split("\\|", 2);
            String roomData = payloadParts[0];
            
            if (payloadParts.length > 1) {
                updateUserList(payloadParts[1]);
            }

            String[] items = (roomData == null || roomData.isBlank()) ? new String[0] : roomData.split("\\s*,\\s*");
            int n = Math.min(items.length, 6); // 방 최대 6개
            for (int i = 0; i < 6; i++) {
                JButton b = lobbyButtons[i / 2][i % 2];
                if (i < n) {
                    String item = items[i];
                    String title = item.replaceFirst("\\s*\\(.*$", "").trim();
                    b.setText(item);
                    b.setEnabled(true);
                    b.setToolTipText("입장: " + title);
                    b.putClientProperty("roomTitle", title);
                } else {
                    b.setText("빈 방");
                    b.setEnabled(false);
                    b.setToolTipText(null);
                    b.putClientProperty("roomTitle", null);
                }
            }
        });
    }

    public void updateUserList(String userListPayload) {
        userListModel.clear();
        if (userListPayload != null && !userListPayload.isEmpty()) {
            String[] users = userListPayload.split(",");
            for (String user : users) {
                userListModel.addElement(user);
            }
        }
    }

    public void appendChatMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
