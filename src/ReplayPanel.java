import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 저장된 게임 기보를 재생하는 리플레이 화면 UI를 구성하는 패널입니다.
 * 리플레이용 보드, 기보 표시 영역, 이전/다음 수 이동 버튼 등을 포함합니다.
 */
public class ReplayPanel extends JPanel {

    private final GameController controller;
    private final GameUI gameUI;
    private final BoardPanel boardPanel; // 리플레이 화면을 표시할 보드 패널
    private final JTextArea moveHistoryArea; // 전체 기보를 표시할 텍스트 영역
    private final JButton prevButton, nextButton; // 이전/다음 수 버튼

    private final GameLogic replayLogic; // 리플레이 재생을 위한 독립적인 게임 로직 인스턴스
    private List<String> moveNotations; // 파일에서 읽어온 기보 목록
    private int currentMoveIndex; // 현재 재생 중인 수의 인덱스

    /**
     * ReplayPanel 생성자입니다.
     * @param controller 컨트롤러 (현재는 사용되지 않음)
     * @param gameUI     화면 전환(로비로 돌아가기)을 위해 필요한 상위 UI 객체
     */
    public ReplayPanel(GameController controller, GameUI gameUI) {
        this.controller = controller;
        this.gameUI = gameUI;
        this.replayLogic = new GameLogic();
        this.moveNotations = new ArrayList<>();
        this.currentMoveIndex = 0;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 리플레이용 보드 패널 초기화
        this.boardPanel = new BoardPanel(null); // 리플레이에서는 컨트롤러를 null로 하여 클릭 이벤트 방지
        this.boardPanel.setPlayerRoleForView(Protocol.P1); // 리플레이는 항상 P1(아래쪽) 시점으로 고정

        // 기보 표시 영역
        moveHistoryArea = new JTextArea(2, 20);
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setLineWrap(false); // 가로로 길게 표시되도록 줄바꿈 방지
        JScrollPane historyScrollPane = new JScrollPane(moveHistoryArea,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 버튼 패널 (이전, 다음, 로비로 돌아가기)
        JPanel buttonPanel = new JPanel(new FlowLayout());
        prevButton = new JButton("<");
        nextButton = new JButton(">");
        JButton backToLobbyButton = new JButton("로비로 돌아가기");

        prevButton.addActionListener(e -> previousMove());
        nextButton.addActionListener(e -> nextMove());
        backToLobbyButton.addActionListener(e -> gameUI.showLobby());

        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(backToLobbyButton);

        // 전체 레이아웃 구성
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(historyScrollPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 리플레이 파일을 로드하여 재생을 준비합니다.
     * @param replayFile 기보가 담긴 텍스트 파일
     */
    public void loadReplay(File replayFile) {
        try {
            this.moveNotations = Files.readAllLines(replayFile.toPath());
            this.currentMoveIndex = 0;
            this.replayLogic.startGame(); // 게임 로직을 초기 상태로 리셋
            refreshReplayView(); // 초기 보드 상태를 화면에 표시
            
            // 기보 전체를 공백으로 구분하여 한 줄로 표시
            moveHistoryArea.setText(String.join(" ", moveNotations));

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "리플레이 파일을 읽는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            gameUI.showLobby(); // 오류 발생 시 로비로 이동
        }
    }

    /**
     * 이전 수로 되돌아갑니다.
     */
    private void previousMove() {
        if (currentMoveIndex > 0) {
            currentMoveIndex--;
            // GameLogic에는 직접적인 '이전 수' 기능이 없으므로,
            // 처음부터 목표 지점까지 수를 다시 실행하여 상태를 복원합니다.
            replayTo(currentMoveIndex);
        }
    }

    /**
     * 다음 수로 진행합니다.
     */
    private void nextMove() {
        if (currentMoveIndex < moveNotations.size()) {
            String notation = moveNotations.get(currentMoveIndex);
            if (replayLogic.executeMove(notation)) {
                currentMoveIndex++;
                refreshReplayView();
            } else {
                 JOptionPane.showMessageDialog(this, "기보 실행 중 오류가 발생했습니다: " + notation, "오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 게임을 초기 상태에서부터 지정된 수(인덱스)까지 재생합니다.
     * @param targetIndex 재생할 마지막 수의 인덱스
     */
    private void replayTo(int targetIndex) {
        replayLogic.startGame(); // 보드를 초기 상태로 리셋
        for (int i = 0; i < targetIndex; i++) {
            replayLogic.executeMove(moveNotations.get(i));
        }
        refreshReplayView();
    }

    /**
     * 현재 replayLogic의 게임 상태를 보드 패널에 반영하여 화면을 갱신합니다.
     */
    private void refreshReplayView() {
        // GameLogic의 현재 보드 상태를 가져옴
        GameBoard currentBoard = replayLogic.getBoard();
        
        // 보드 상태를 문자열로 직렬화하여 BoardPanel에 전달
        StringBuilder boardStr = new StringBuilder();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                Piece piece = currentBoard.getPieceAt(r, c);
                if (piece != null) {
                    boardStr.append(String.format("%s,%d,%d;", piece.name(), r, c));
                }
            }
        }
        boardPanel.updateBoard(boardStr.toString());

        // 이전/다음 버튼의 활성화 상태를 갱신
        prevButton.setEnabled(currentMoveIndex > 0);
        nextButton.setEnabled(currentMoveIndex < moveNotations.size());
    }

    public GameController getController() {
        return controller;
    }
}
