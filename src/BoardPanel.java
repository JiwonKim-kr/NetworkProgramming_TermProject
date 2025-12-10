import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

/**
 * 십이장기 게임 보드를 시각적으로 표현하고 사용자 입력을 처리하는 패널입니다.
 * 4x3 그리드의 버튼으로 보드를 구성하며, 기물 아이콘 표시, 유효 이동 범위 하이라이트 등의 기능을 담당합니다.
 * P1과 P2의 시점에 따라 보드 뷰를 뒤집어서 보여주는 기능도 포함합니다.
 */
public class BoardPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] boardButtons = new JButton[4][3]; // 보드 칸을 나타내는 버튼 배열
    private final Piece[][] boardState = new Piece[4][3]; // 현재 보드의 기물 상태 (논리적 상태)
    private final List<int[]> validMoveCells = new ArrayList<>(); // 현재 하이라이트된 유효 이동 셀 목록
    private final List<int[]> initialHighlightCells = new ArrayList<>();
    private boolean myTurn = false;
    private String playerRoleForView = Protocol.P1; // 기본 시점은 P1

    // UI 상수 정의
    private static final Font DEFAULT_FONT = new Font("맑은 고딕", Font.BOLD, 24);
    private static final Color HIGHLIGHT_VALID_MOVE = Color.YELLOW;
    private static final Color HIGHLIGHT_SELECTED_PIECE = Color.RED;
    private static final Color HIGHLIGHT_CAPTURED_PIECE = Color.GREEN;
    private static final Color HIGHLIGHT_FIRST_TURN = Color.CYAN;
    private final java.util.List<Point> highlightedCells = new java.util.ArrayList<>();
    // 기물 아이콘 캐싱을 위한 맵
    private final Map<Piece, ImageIcon> pieceIcons = new EnumMap<>(Piece.class);

    /**
     * BoardPanel 생성자입니다.
     * @param controller 보드 클릭 이벤트를 처리할 컨트롤러 (리플레이 시에는 null)
     */
    public BoardPanel(GameController controller) {
        this.controller = controller;
        this.setLayout(new GridLayout(4, 3, 5, 5));
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c] = new JButton();
                boardButtons[r][c].setFont(DEFAULT_FONT);
                boardButtons[r][c].setFocusable(false);
                final int viewRow = r;
                final int viewCol = c;
                boardButtons[r][c].addActionListener(e -> {
                    if (controller != null) { // 리플레이 시에는 컨트롤러가 없으므로 이벤트 처리 안함
                        // UI(뷰) 좌표를 게임 로직(모델) 좌표로 변환하여 컨트롤러에 전달
                        int[] modelCoords = viewToModel(viewRow, viewCol);
                        controller.onBoardClicked(modelCoords[0], modelCoords[1]);
                    }
                });
                this.add(boardButtons[r][c]);
            }
        }
    }

    // --- 좌표 변환 (View <-> Model) ---

    /**
     * 모델(게임 로직) 좌표를 뷰(UI) 좌표로 변환합니다.
     * P2의 경우 보드를 180도 회전시켜 보여주기 위해 좌표를 변환합니다.
     * @param r 모델 행
     * @param c 모델 열
     * @return 뷰 좌표 [행, 열]
     */
    private int[] modelToView(int r, int c) {
        return isP1() ? new int[]{r, c} : new int[]{3 - r, 2 - c};
    }

    /**
     * 뷰(UI) 좌표를 모델(게임 로직) 좌표로 변환합니다.
     * @param r 뷰 행
     * @param c 뷰 열
     * @return 모델 좌표 [행, 열]
     */
    private int[] viewToModel(int r, int c) {
        return isP1() ? new int[]{r, c} : new int[]{3 - r, 2 - c};
    }

    /** 현재 뷰가 P1 시점인지 확인합니다. */
    private boolean isP1() {
        return playerRoleForView.equals(Protocol.P1);
    }
    
    // --- 공개 API ---

    /**
     * 이 보드 패널의 시점을 설정합니다. (P1 또는 P2)
     * @param role "P1" 또는 "P2"
     */
    public void setPlayerRoleForView(String role) {
        if (role != null && (role.equals(Protocol.P1) || role.equals(Protocol.P2))) {
            this.playerRoleForView = role;
        }
    }

    public void setMyTurn(boolean myTurn) { this.myTurn = myTurn; }
    public boolean isMyTurn() { return myTurn; }
    public String getPieceOwnerRole(int r, int c) { Piece piece = boardState[r][c]; return (piece != null) ? piece.getOwner().name() : null; }
    public boolean isValidMove(int r, int c) { return validMoveCells.stream().anyMatch(m -> m[0] == r && m[1] == c); }
    
    /**
     * 서버로부터 받은 보드 상태 문자열을 파싱하여 UI에 기물들을 업데이트합니다.
     * @param boardStateStr 서버에서 전달된 보드 상태 정보
     */
    public void updateBoard(String boardStateStr) {
        // 보드 초기화
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c].setIcon(null);
                boardState[r][c] = null;
            }
        }
        if (boardStateStr == null || boardStateStr.isBlank()) return;

        // 상태 문자열 파싱 및 기물 배치
        for (String pieceInfo : boardStateStr.split(";")) {
            if (pieceInfo.isBlank()) continue;
            String[] info = pieceInfo.split(",");
            Piece piece = Piece.valueOf(info[0].trim());
            int r = Integer.parseInt(info[1].trim());
            int c = Integer.parseInt(info[2].trim());
            boardState[r][c] = piece; // 논리적 상태 업데이트
            
            int[] viewCoords = modelToView(r, c); // 뷰 좌표로 변환
            JButton btn = boardButtons[viewCoords[0]][viewCoords[1]];
            boolean isOpponent = (piece.getOwner() == Piece.Player.P1) != isP1();
            ImageIcon icon = getPieceIcon(piece, btn, isOpponent);
            btn.setIcon(icon); // UI 업데이트
        }
        clearHighlights(true);
    }

    /**
     * 잡은 기물 목록을 UI에 업데이트합니다.
     * @param myPanel 내가 잡은 기물을 표시할 패널
     * @param opponentPanel 상대가 잡은 기물을 표시할 패널
     * @param p1CapturedStr P1이 잡은 기물 목록 문자열
     * @param p2CapturedStr P2가 잡은 기물 목록 문자열
     */
    public void updateCapturedPieces(JPanel myPanel, JPanel opponentPanel, String p1CapturedStr, String p2CapturedStr) {
        myPanel.removeAll();
        opponentPanel.removeAll();
        
        // 현재 시점에 따라 '나'와 '상대'의 잡은 기물 목록을 결정
        String myCapturedStr = isP1() ? p1CapturedStr : p2CapturedStr;
        String opponentCapturedStr = isP1() ? p2CapturedStr : p1CapturedStr;

        populateCapturedPanel(myPanel, myCapturedStr, true);
        populateCapturedPanel(opponentPanel, opponentCapturedStr, false);

        myPanel.revalidate();
        myPanel.repaint();
        opponentPanel.revalidate();
        opponentPanel.repaint();
    }

    /**
     * 잡은 기물 문자열을 파싱하여 실제 UI 패널에 버튼(기물)들을 추가합니다.
     */
    private void populateCapturedPanel(JPanel panel, String capturedStr, boolean isMyPanel) {
        if (capturedStr == null || capturedStr.isEmpty()) return;

        Arrays.stream(capturedStr.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(Piece::valueOf)
              .forEach(piece -> {
                  JButton pieceButton = new JButton();
                  pieceButton.setPreferredSize(new Dimension(36, 36));
                  boolean rotate180 = !isMyPanel;
                  ImageIcon baseIcon = getPieceIcon(piece, pieceButton, rotate180);

                  if (baseIcon != null) {
                      int iw = baseIcon.getIconWidth();
                      int ih = baseIcon.getIconHeight();
                      int scaledW = (int)(iw * 0.9);
                      int scaledH = (int)(ih * 0.9);
                      Image scaledImg = baseIcon.getImage().getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);
                      ImageIcon scaledIcon = new ImageIcon(scaledImg);
                      pieceButton.setIcon(scaledIcon);
                      pieceButton.setToolTipText(piece.getDisplayName());
                      pieceButton.setMargin(new Insets(0, 0, 0, 0));
                      pieceButton.setFocusPainted(false);
                      pieceButton.setContentAreaFilled(false);
                      pieceButton.setBorderPainted(true);
                  }

                  if (isMyPanel && controller != null) { // 내 기물이고, 리플레이가 아닐 때만 클릭 이벤트 추가
                      pieceButton.addActionListener(
                          e -> controller.onCapturedPieceClicked(piece, e.getSource())
                      );
                  } else {
                      pieceButton.setEnabled(false);
                  }
                  panel.add(pieceButton);
              });
    }

    /**
     * 서버로부터 받은 유효 이동 좌표 목록을 받아 보드에 노란색으로 하이라이트합니다.
     * @param payload 유효 이동 좌표 목록 문자열 (예: "1,2;2,1")
     */
    public void highlightValidMoves(String payload) {
        clearHighlights(false);
        if (payload == null || payload.isBlank()) return;
        for (String move : payload.split(";")) {
            if (move.isBlank()) continue;
            String[] coords = move.split(",");
            int r = Integer.parseInt(coords[0].trim());
            int c = Integer.parseInt(coords[1].trim());
            int[] viewCoords = modelToView(r, c);
            boardButtons[viewCoords[0]][viewCoords[1]].setBackground(HIGHLIGHT_VALID_MOVE);
            validMoveCells.add(new int[]{r, c});
        }
    }

    /**
     * 잡은 말을 놓을 수 있는 위치를 하이라이트합니다.
     */
    public void highlightSummonRange() {
        clearHighlights(false);
        // 십이장기 규칙에 따라 2~4행의 빈 칸에만 놓을 수 있음 (P1 기준)
        for (int r = 1; r <= 3; r++) {
            for (int c = 0; c < 3; c++) {
             if (getPieceOwnerRole(r, c) == null) {
                    int[] view = modelToView(r, c);
                    boardButtons[view[0]][view[1]].setBackground(Color.YELLOW);
                    highlightedCells.add(new Point(view[0], view[1]));
                }
            }
        }
    }
    
    /**
     * 선택된 보드 위의 기물을 빨간색 테두리로 하이라이트합니다.
     */
    public void highlightSelectedBoardPiece(int r, int c) { 
        int[] viewCoords = modelToView(r, c);
        boardButtons[viewCoords[0]][viewCoords[1]].setBorder(new LineBorder(HIGHLIGHT_SELECTED_PIECE, 2)); 
    }

    /**
     * 선택된 잡은 기물을 초록색 테두리로 하이라이트하고, 놓을 수 있는 위치를 표시합니다.
     */
    public void highlightSelectedCapturedPiece(Object sourceButton) { 
        ((JButton) sourceButton).setBorder(new LineBorder(HIGHLIGHT_CAPTURED_PIECE, 2)); 
        highlightSummonRange();
    }

    /**
     * 게임 시작 시 자신의 모든 기물을 잠시 하이라이트하여 보여줍니다.
     * @param playerRole 하이라이트할 플레이어의 역할 (P1 또는 P2)
     */
    public void highlightPlayerPieces(String playerRole) {
        clearHighlights(true);
        Piece.Player player = Piece.Player.valueOf(playerRole);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                if (boardState[r][c] != null && boardState[r][c].getOwner() == player) {
                    int[] viewCoords = modelToView(r, c);
                    boardButtons[viewCoords[0]][viewCoords[1]].setBackground(HIGHLIGHT_FIRST_TURN);
                    initialHighlightCells.add(new int[]{r, c});
                }
            }
        }
    }

    /**
     * 보드의 모든 하이라이트를 제거합니다.
     * @param clearSelection 선택된 기물의 테두리 하이라이트까지 제거할지 여부
     */
    public void clearHighlights(boolean clearSelection) {
        if (clearSelection) {
            for (int r = 0; r < 4; r++) for (int c = 0; c < 3; c++) boardButtons[r][c].setBorder(UIManager.getBorder("Button.border"));
        }
        // 유효 이동 범위 하이라이트 제거
        for (int[] cell : validMoveCells) {
            int[] viewCoords = modelToView(cell[0], cell[1]);
            boardButtons[viewCoords[0]][viewCoords[1]].setBackground(UIManager.getColor("Button.background"));
        }
        validMoveCells.clear();
        // 시작 턴 하이라이트 제거
        for (int[] cell : initialHighlightCells) {
            int[] viewCoords = modelToView(cell[0], cell[1]);
            boardButtons[viewCoords[0]][viewCoords[1]].setBackground(UIManager.getColor("Button.background"));
        }
        // 소환 범위 하이라이트 제거
        for (Point p : highlightedCells) {
            boardButtons[p.x][p.y].setBackground(UIManager.getColor("Button.background"));
        }
        highlightedCells.clear();
        initialHighlightCells.clear();
    }

    /**
     * 보드를 초기 상태(빈 보드)로 리셋합니다.
     */
    public void resetBoard() {
        updateBoard("");
    }

    /**
     * 기물에 해당하는 아이콘을 반환합니다. 아이콘은 캐시되어 재사용됩니다.
     * @param piece 아이콘을 가져올 기물
     * @param btn 아이콘 크기 조절의 기준이 될 버튼
     * @param rotate180 아이콘을 180도 회전시킬지 여부
     * @return 크기가 조절된 이미지 아이콘
     */
    private ImageIcon getPieceIcon(Piece piece, JButton btn, boolean rotate180) {
        // 캐시에서 아이콘을 먼저 찾아보고, 없으면 파일에서 로드하여 캐시에 저장
        ImageIcon icon = pieceIcons.computeIfAbsent(piece, k -> {
            String fileName = "/images/" + k.name() + ".png";
            java.net.URL url = getClass().getResource(fileName);
            return (url != null) ? new ImageIcon(url) : null;
        });

        if (icon == null) return null;
        
        Image img = icon.getImage();
        // 상대방 기물일 경우 180도 회전
        if (rotate180) {
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            java.awt.image.BufferedImage rotatedImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = rotatedImg.createGraphics();
            g2d.rotate(Math.PI, w / 2.0, h / 2.0);
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();
            img = rotatedImg;
        }
        // 버튼 크기에 맞게 아이콘 이미지 크기 조절
        int wBtn = btn.getWidth();
        int hBtn = btn.getHeight();
        if (wBtn <= 1) wBtn = 48; // 버튼 크기가 아직 정해지지 않은 경우 기본값 사용
        if (hBtn <= 1) hBtn = 48;
        int scaledWidth = Math.max(1, wBtn - 18);
        int scaledHeight = Math.max(1, hBtn - 18);
        Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }
}
