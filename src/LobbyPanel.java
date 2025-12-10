import javax.swing.*;
import java.awt.*;

/**
 * 로비 화면 UI를 구성하는 패널입니다.
 * 게임방 목록, 접속자 목록, 로비 채팅, 방 만들기 등의 기능을 제공합니다.
 */
public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] lobbyButtons = new JButton[3][2]; // 6개의 게임방을 표시할 버튼 배열

    private DefaultListModel<String> userListModel; // 접속자 목록을 표시할 리스트 모델
    private JTextArea chatArea; // 채팅 내용을 표시할 텍스트 영역
    private JTextField chatInputField; // 채팅 입력 필드
    private JTextField chatInput;
    private final Color defaultButtonBg = UIManager.getColor("Button.background");

    /**
     * LobbyPanel 생성자입니다.
     * @param controller UI 이벤트를 처리할 게임 컨트롤러
     */
    public LobbyPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 중앙(방 목록, 채팅)과 동쪽(접속자 목록)으로 레이아웃 구성
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 10));
        mainContentPanel.add(createLobbyGridPanel(), BorderLayout.CENTER);
        mainContentPanel.add(createLobbyChatPanel(), BorderLayout.SOUTH);

        this.add(mainContentPanel, BorderLayout.CENTER);
        this.add(createSideInfoPanel(), BorderLayout.EAST);
    }

    /**
     * 로비 채팅 패널을 생성합니다. (createLobbyChatPanel 메서드에 대체됨)
     * @return 생성된 채팅 패널
     */
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
                controller.sendLobbyChat(text);
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

    /**
     * 로비 채팅 메시지를 UI에 추가합니다.
     * @param msg 추가할 메시지
     */
    public void appendLobbyChatMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    /**
     * 게임방 목록을 표시하는 그리드 패널을 생성합니다.
     * @return 생성된 로비 그리드 패널
     */
    private JPanel createLobbyGridPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel lobbyGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 6개의 방 버튼 생성
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
                            // 비밀방일 경우 비밀번호 입력 다이얼로그 표시
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

    /**
     * '방 만들기' 버튼이 포함된 하단 패널을 생성합니다.
     * @return 생성된 하단 버튼 패널
     */
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

    /**
     * 접속자 목록을 표시하는 사이드 패널을 생성합니다.
     * @return 생성된 사이드 정보 패널
     */
    private JPanel createSideInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("접속자 목록"));
        panel.setPreferredSize(new Dimension(200, 0));
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);
        return panel;
    }

    /**
     * 로비 채팅 UI 패널을 생성합니다.
     * @return 생성된 로비 채팅 패널
     */
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

    /**
     * 입력된 채팅 메시지를 컨트롤러를 통해 전송합니다.
     */
    private void sendChatMessage() {
        String message = chatInputField.getText().trim();
        if (!message.isEmpty()) {
            // 방 안에서의 채팅과 로비 채팅을 구분하기 위해 컨트롤러의 다른 메서드 호출
            controller.sendLobbyChat(message); 
            chatInputField.setText("");
        }
    }

    /**
     * 서버로부터 받은 방 목록 정보로 UI를 업데이트합니다.
     * @param payload 방 목록과 접속자 목록 정보가 담긴 문자열
     */
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
                    
                    // HTML을 사용하여 특정 텍스트에 색상 적용
                    b.setText("<html>" + itemText.replace("[비밀방]", "<font color='red'>[비밀방]</font>") + "</html>");
                    b.setEnabled(true);
                    b.setToolTipText("입장: " + title);
                    // 버튼에 방 정보를 저장하여 클릭 시 사용
                    b.putClientProperty("roomTitle", title);
                    b.putClientProperty("isPrivate", isPrivate);
                    if (itemText.contains("[게임중]")) {
                        b.setBackground(Color.YELLOW);
                    } else
                        b.setBackground(Color.GREEN);
                    
                } else {
                    // 빈 방으로 설정
                    b.setText("빈 방");
                    b.setEnabled(false);
                    b.setToolTipText(null);
                    b.putClientProperty("roomTitle", null);
                    b.putClientProperty("isPrivate", false);
                    b.setBackground(defaultButtonBg);
                }
            }
        });
    }

    /**
     * 서버로부터 받은 접속자 목록 정보로 UI를 업데이트합니다.
     * @param userListPayload 접속자 목록 문자열
     */
    public void updateUserList(String userListPayload) {
        userListModel.clear();
        if (userListPayload != null && !userListPayload.isEmpty()) {
            String[] users = userListPayload.split(",");
            for (String user : users) {
                userListModel.addElement(user);
            }
        }
    }

    /**
     * 채팅 영역에 메시지를 추가합니다.
     * @param message 추가할 메시지
     */
    public void appendChatMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    /**
     * 채팅 영역을 비웁니다.
     */
    public void clearChat() {
        chatArea.setText("");
    }
}
