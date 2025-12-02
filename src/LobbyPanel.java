import javax.swing.*;
import java.awt.*;

/**
 * 게임 로비 화면을 구성하는 패널입니다.
 * 방 목록을 표시하고, 방에 입장하거나 새로운 방을 생성하는 기능을 제공합니다.
 */
public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] lobbyButtons = new JButton[4][2];
    private final Color defaultButtonBg = UIManager.getColor("Button.background");
    private JTextArea chatArea;
    private JTextField chatInput;
    
    public LobbyPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);
        this.add(createCenterPanel(), BorderLayout.CENTER);
        this.add(createBottomPanel(), BorderLayout.SOUTH);
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

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                roomScroll,
                chatPanel
        );
        split.setResizeWeight(0.7); // 왼쪽 70%, 오른쪽 30% 비율
        return split;
    }
    private JScrollPane createLobbyGrid() {
        JPanel lobbyGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 2; c++) {
                JButton b = new JButton("빈 방");
                b.setFocusable(false);
                b.setEnabled(false);
                b.addActionListener(e -> {
                    JButton src = (JButton) e.getSource();
                    String title = (String) src.getClientProperty("roomTitle");
                    if (title != null && !title.isBlank()) {
                    	controller.requestRoomInfo(title);
                    }
                });
                lobbyButtons[r][c] = b;
                lobbyGrid.add(b);
            }
        }
        return new JScrollPane(lobbyGrid);
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        JButton createRoomButton = new JButton("방 만들기");

        createRoomButton.addActionListener(e -> {

            // ---------- 다이얼로그 UI 구성 ----------
            JTextField titleField = new JTextField();
            JCheckBox privateCheck = new JCheckBox("비밀방으로 만들기");
            JPasswordField pwField = new JPasswordField();
            pwField.setEnabled(false);

            privateCheck.addActionListener(ev -> {
                pwField.setEnabled(privateCheck.isSelected());
            });

            JPanel form = new JPanel(new GridLayout(0,1,5,5));
            form.add(new JLabel("방 제목:"));
            form.add(titleField);
            form.add(privateCheck);
            form.add(new JLabel("비밀번호:"));
            form.add(pwField);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    form,
                    "방 만들기",
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText().trim();
                boolean isPrivate = privateCheck.isSelected();
                String password = new String(pwField.getPassword());

                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "방 제목은 비워둘 수 없습니다.");
                    return;
                }

                if (isPrivate && password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "비밀번호를 입력하세요.");
                    return;
                }

                // 🔥 서버로 보내는 payload 형식:  title|1|pw   또는  title|0|
                String payload = title + "|" + (isPrivate ? "1" : "0") + "|" + password;
                controller.createRoom(payload);
            }
        });

        panel.add(createRoomButton);
        return panel;
    }

    

    public void updateRoomList(String payload) {
        SwingUtilities.invokeLater(() -> {
        	String[] items = (payload == null || payload.isBlank())
                    ? new String[0]
                    : payload.split("\\s*,\\s*");

            int n = Math.min(items.length, 8);

            for (int i = 0; i < 8; i++) {
                JButton b = lobbyButtons[i % 4][i / 4];
                if (i < n) {
                String item = items[i];  

                
                String roomTitle = item.replaceFirst("\\s*\\(.*$", "").trim();

                
                b.setText("<html><center>" + item + "</center></html>");
                    b.setEnabled(true);
                    b.setToolTipText("입장: " + roomTitle);
                    b.putClientProperty("roomTitle", roomTitle);

                    if (item.contains("[게임중]")) {
                        b.setBackground(Color.YELLOW);
                    } else
                        b.setBackground(Color.GREEN);
                    
                } else {
                    b.setText("빈 방");
                    b.setEnabled(false);
                    b.setToolTipText(null);
                    b.putClientProperty("roomTitle", null);
                    b.setBackground(defaultButtonBg);
                }
            }
        });
    }

}
