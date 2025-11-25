import javax.swing.*;
import java.awt.*;

/**
 * 게임 로비 화면을 구성하는 패널입니다.
 * 방 목록을 표시하고, 방에 입장하거나 새로운 방을 생성하는 기능을 제공합니다.
 */
public class LobbyPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] lobbyButtons = new JButton[4][3];

    public LobbyPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.add(new JLabel("대기실 목록", SwingConstants.CENTER), BorderLayout.NORTH);
        this.add(createLobbyGrid(), BorderLayout.CENTER);
        this.add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JScrollPane createLobbyGrid() {
        JPanel lobbyGrid = new JPanel(new GridLayout(4, 3, 8, 8));
        lobbyGrid.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
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
        return new JScrollPane(lobbyGrid);
    }

    private JPanel createBottomPanel() {
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

    public void updateRoomList(String payload) {
        SwingUtilities.invokeLater(() -> {
            String[] items = (payload == null || payload.isBlank()) ? new String[0] : payload.split("\\s*,\\s*");
            int n = Math.min(items.length, 12);
            for (int i = 0; i < 12; i++) {
                JButton b = lobbyButtons[i / 3][i % 3];
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
}
