import javax.swing.*;
import java.awt.*;

public class CreateRoomDialogPanel extends JPanel {
    private JTextField roomTitleField;
    private JRadioButton publicRadio, privateRadio;
    private JPasswordField passwordField;
    private JSpinner maxPlayersSpinner;

    public CreateRoomDialogPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 방 이름
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("방 이름:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        roomTitleField = new JTextField(15);
        add(roomTitleField, gbc);

        // 공개/비공개 여부
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(new JLabel("공개 설정:"), gbc);
        publicRadio = new JRadioButton("공개", true);
        privateRadio = new JRadioButton("비공개");
        ButtonGroup group = new ButtonGroup();
        group.add(publicRadio);
        group.add(privateRadio);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.add(publicRadio);
        radioPanel.add(privateRadio);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        add(radioPanel, gbc);

        // 비밀번호
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("비밀번호:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        passwordField = new JPasswordField(15);
        passwordField.setEnabled(false); // 초기에는 비활성화
        add(passwordField, gbc);

        // 최대 인원
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("최대 인원:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(2, 2, 6, 1);
        maxPlayersSpinner = new JSpinner(spinnerModel);
        add(maxPlayersSpinner, gbc);

        // 라디오 버튼 리스너 (비밀번호 필드 활성화/비활성화)
        privateRadio.addActionListener(e -> passwordField.setEnabled(true));
        publicRadio.addActionListener(e -> passwordField.setEnabled(false));
    }

    // 외부에서 값을 가져가기 위한 Getter 메서드들
    public String getRoomTitle() {
        return roomTitleField.getText();
    }

    public String getPassword() {
        return privateRadio.isSelected() ? new String(passwordField.getPassword()) : "";
    }

    public int getMaxPlayers() {
        return (Integer) maxPlayersSpinner.getValue();
    }
}
