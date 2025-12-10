import javax.swing.*;
import java.awt.*;

/**
 * '방 만들기' 다이얼로그에 표시될 UI를 구성하는 패널입니다.
 * 방 제목, 공개/비공개 여부, 비밀번호, 최대 인원 등을 입력받는 필드를 포함합니다.
 */
public class CreateRoomDialogPanel extends JPanel {
    private JTextField roomTitleField;
    private JRadioButton publicRadio, privateRadio;
    private JPasswordField passwordField;
    private JSpinner maxPlayersSpinner;

    /**
     * CreateRoomDialogPanel 생성자입니다.
     * GridBagLayout을 사용하여 컴포넌트들을 배치합니다.
     */
    public CreateRoomDialogPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 방 이름 입력 필드
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("방 이름:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        roomTitleField = new JTextField(15);
        add(roomTitleField, gbc);

        // 공개/비공개 여부 라디오 버튼
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(new JLabel("공개 설정:"), gbc);
        publicRadio = new JRadioButton("공개", true);
        privateRadio = new JRadioButton("비공개");
        ButtonGroup group = new ButtonGroup(); // 라디오 버튼 그룹화
        group.add(publicRadio);
        group.add(privateRadio);
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.add(publicRadio);
        radioPanel.add(privateRadio);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        add(radioPanel, gbc);

        // 비밀번호 입력 필드
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("비밀번호:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        passwordField = new JPasswordField(15);
        passwordField.setEnabled(false); // 공개방이 기본값이므로 초기에는 비활성화
        add(passwordField, gbc);

        // 최대 인원 설정 스피너
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("최대 인원:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        // 최소 2명, 최대 6명, 기본 2명, 1씩 증가/감소
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(2, 2, 6, 1);
        maxPlayersSpinner = new JSpinner(spinnerModel);
        add(maxPlayersSpinner, gbc);

        // 라디오 버튼 선택에 따라 비밀번호 필드 활성화/비활성화 리스너
        privateRadio.addActionListener(e -> passwordField.setEnabled(true));
        publicRadio.addActionListener(e -> passwordField.setEnabled(false));
    }

    // --- 외부에서 입력값을 가져가기 위한 Getter 메서드들 ---

    /**
     * 사용자가 입력한 방 제목을 반환합니다.
     * @return 방 제목
     */
    public String getRoomTitle() {
        return roomTitleField.getText();
    }

    /**
     * 사용자가 입력한 비밀번호를 반환합니다.
     * 공개방으로 설정된 경우 빈 문자열을 반환합니다.
     * @return 비밀번호 문자열
     */
    public String getPassword() {
        return privateRadio.isSelected() ? new String(passwordField.getPassword()) : "";
    }

    /**
     * 사용자가 설정한 최대 인원 수를 반환합니다.
     * @return 최대 인원 수
     */
    public int getMaxPlayers() {
        return (Integer) maxPlayersSpinner.getValue();
    }
}
