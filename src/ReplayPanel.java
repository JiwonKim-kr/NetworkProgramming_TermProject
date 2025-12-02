import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ReplayPanel extends JPanel {

    private final GameController controller;
    private final GameUI gameUI;
    private final BoardPanel boardPanel;
    private final JTextArea moveHistoryArea;
    private final JButton prevButton, nextButton;

    private final GameLogic replayLogic;
    private List<String> moveNotations;
    private int currentMoveIndex;

    public ReplayPanel(GameController controller, GameUI gameUI) {
        this.controller = controller;
        this.gameUI = gameUI;
        this.replayLogic = new GameLogic();
        this.moveNotations = new ArrayList<>();
        this.currentMoveIndex = 0;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 리플레이용 보드 패널 초기화
        this.boardPanel = new BoardPanel(null); // 리플레이에서는 컨트롤러 직접 호출 방지
        this.boardPanel.setPlayerRoleForView(Protocol.P1); // 리플레이는 항상 P1 시점으로 고정

        // 기보 표시 영역
        moveHistoryArea = new JTextArea(2, 20); // 높이를 2줄로 줄임
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setLineWrap(false); // 가로로 길게 표시되도록 줄바꿈 방지
        JScrollPane historyScrollPane = new JScrollPane(moveHistoryArea,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 버튼 패널
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

    public void loadReplay(File replayFile) {
        try {
            this.moveNotations = Files.readAllLines(replayFile.toPath());
            this.currentMoveIndex = 0;
            this.replayLogic.startGame();
            refreshReplayView();
            
            // 기보 전체를 공백으로 구분하여 한 줄로 표시
            moveHistoryArea.setText(String.join(" ", moveNotations));

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "리플레이 파일을 읽는 중 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
            gameUI.showLobby();
        }
    }

    private void previousMove() {
        if (currentMoveIndex > 0) {
            currentMoveIndex--;
            // 상태를 되돌리기 위해 처음부터 다시 재생
            replayTo(currentMoveIndex);
        }
    }

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

    private void replayTo(int targetIndex) {
        replayLogic.startGame(); // 보드를 초기 상태로 리셋
        for (int i = 0; i < targetIndex; i++) {
            replayLogic.executeMove(moveNotations.get(i));
        }
        refreshReplayView();
    }

    private void refreshReplayView() {
        // GameLogic의 현재 상태를 BoardPanel에 반영
        GameBoard currentBoard = replayLogic.getBoard();
        
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

        // 버튼 활성화/비활성화
        prevButton.setEnabled(currentMoveIndex > 0);
        nextButton.setEnabled(currentMoveIndex < moveNotations.size());
    }
}
