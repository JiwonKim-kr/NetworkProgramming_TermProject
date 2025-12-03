import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] lobbyButtons = new JButton[3][2];

    private DefaultListModel<String> userListModel;
    private JTextArea chatArea;
    private JTextField chatInputField;

    public LobbyPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.add(createLobbyGridPanel(), BorderLayout.CENTER);
        mainContentPanel.add(createLobbyChatPanel(), BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);
        this.add(createSideInfoPanel(), BorderLayout.EAST);
    }
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JLabel title = new JLabel("로비 채팅", SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(chatArea);
        panel.add(scroll, BorderLayout.CENTER);

        chatInput = new JTextField();
        JButton sendButton = new JButton("전송");

        Runnable sendAction = () -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                controller.sendLobbyChat(text);  // ★ GameController로 넘김
                chatInput.setText("");
            }
        };

        chatInput.addActionListener(e -> sendAction.run());
        sendButton.addActionListener(e -> sendAction.run());

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(250, 0)); // 오른쪽 폭

        return panel;
    }
    public void appendLobbyChatMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    private JComponent createCenterPanel() {
        JScrollPane roomScroll = createLobbyGrid();   // 기존 방 목록 그대로 사용
        JPanel chatPanel = createChatPanel();    // 오른쪽 채팅창

    private JPanel createLobbyGridPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel lobbyGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 2; c++) {
                JButton b = new JButton("빈 방");
                b.setFocusable(false);
                b.setEnabled(false);
                b.addActionListener(e -> {
                    JButton src = (JButton) e.getSource();
                    String title = (String) src.getClientProperty("roomTitle");
                    boolean isPrivate = (Boolean) src.getClientProperty("isPrivate");

                    if (title != null && !title.isBlank()) {
                        String password = "";
                        if (isPrivate) {
                            password = JOptionPane.showInputDialog(this, "비밀번호를 입력하세요:", "비밀방 입장", JOptionPane.PLAIN_MESSAGE);
                            if (password == null) return; // 사용자가 취소한 경우
                        }
                        controller.joinRoom(title, password);
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
            CreateRoomDialogPanel dialogPanel = new CreateRoomDialogPanel();
            int result = JOptionPane.showConfirmDialog(this, dialogPanel, "방 만들기", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String title = dialogPanel.getRoomTitle();
                if (title != null && !title.trim().isEmpty()) {
                    String password = dialogPanel.getPassword();
                    int maxPlayers = dialogPanel.getMaxPlayers();
                    controller.createRoom(title, password, maxPlayers);
                } else {
                    JOptionPane.showMessageDialog(this, "방 이름은 비워둘 수 없습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                }
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
        JList<String> userList = new JList<>(userListModel);
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
            int n = Math.min(items.length, 6);
            for (int i = 0; i < 6; i++) {
                JButton b = lobbyButtons[i / 2][i % 2];
                if (i < n) {
                    String itemText = items[i].trim();
                    String title = itemText.split("\\s+")[0];
                    boolean isPrivate = itemText.contains("[비밀방]");
                    
                    b.setText("<html>" + itemText.replace("[비밀방]", "<font color='red'>[비밀방]</font>") + "</html>");
                    b.setEnabled(true);
                    b.setToolTipText("입장: " + title);
                    b.putClientProperty("roomTitle", title);
                    b.putClientProperty("isPrivate", isPrivate);
                } else {
                    b.setText("빈 방");
                    b.setEnabled(false);
                    b.setToolTipText(null);
                    b.putClientProperty("roomTitle", null);
                    b.putClientProperty("isPrivate", false);
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

    public void clearChat() {
        chatArea.setText("");
    }
}
