import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

public class BoardPanel extends JPanel {
    private final GameController controller;
    private final JButton[][] boardButtons = new JButton[4][3];
    private final Piece[][] boardState = new Piece[4][3];
    private final List<int[]> validMoveCells = new ArrayList<>();
    private final List<int[]> initialHighlightCells = new ArrayList<>();
    private boolean myTurn = false;

    private static final Font DEFAULT_FONT = new Font("맑은 고딕", Font.BOLD, 24);
    private static final Color HIGHLIGHT_VALID_MOVE = Color.YELLOW;
    private static final Color HIGHLIGHT_SELECTED_PIECE = Color.RED;
    private static final Color HIGHLIGHT_CAPTURED_PIECE = Color.GREEN;
    private static final Color HIGHLIGHT_FIRST_TURN = Color.CYAN;
    
    private final Map<Piece, ImageIcon> pieceIcons = new EnumMap<>(Piece.class);

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
                    int[] modelCoords = viewToModel(viewRow, viewCol);
                    controller.onBoardClicked(modelCoords[0], modelCoords[1]);
                });
                this.add(boardButtons[r][c]);
            }
        }
    }

    // --- 좌표 변환 ---
    private int[] modelToView(int r, int c) {
        return isP1() ? new int[]{r, c} : new int[]{3 - r, 2 - c};
    }

    private int[] viewToModel(int r, int c) {
        return isP1() ? new int[]{r, c} : new int[]{3 - r, 2 - c};
    }

    private boolean isP1() {
        String role = controller.getPlayerRole();
        return role == null || role.equals(Protocol.P1);
    }

    // --- Public API ---
    public void setMyTurn(boolean myTurn) { this.myTurn = myTurn; }
    public boolean isMyTurn() { return myTurn; }
    public String getPieceOwnerRole(int r, int c) { Piece piece = boardState[r][c]; return (piece != null) ? piece.getOwner().name() : null; }
    public boolean isValidMove(int r, int c) { return validMoveCells.stream().anyMatch(m -> m[0] == r && m[1] == c); }

    public void updateBoard(String boardStateStr) {
        clearHighlights(true);
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 3; c++) {
                boardButtons[r][c].setIcon(null);
                boardState[r][c] = null;
            }
        }
        if (boardStateStr == null || boardStateStr.isBlank()) return;

        for (String pieceInfo : boardStateStr.split(";")) {
            if (pieceInfo.isBlank()) continue;
            String[] info = pieceInfo.split(",");
            Piece piece = Piece.valueOf(info[0].trim());
            int r = Integer.parseInt(info[1].trim());
            int c = Integer.parseInt(info[2].trim());
            boardState[r][c] = piece;
            
            int[] viewCoords = modelToView(r, c);
            JButton btn = boardButtons[viewCoords[0]][viewCoords[1]];
            boolean isOpponent = (piece.getOwner() == Piece.Player.P1) != isP1();
            ImageIcon icon = getPieceIcon(piece, btn, isOpponent);
            btn.setIcon(icon);
        }
    }

    public void updateCapturedPieces(JPanel myPanel, JPanel opponentPanel, String p1CapturedStr, String p2CapturedStr) {
        myPanel.removeAll();
        opponentPanel.removeAll();

        String myCapturedStr = isP1() ? p1CapturedStr : p2CapturedStr;
        String opponentCapturedStr = isP1() ? p2CapturedStr : p1CapturedStr;

        populateCapturedPanel(myPanel, myCapturedStr, true);
        populateCapturedPanel(opponentPanel, opponentCapturedStr, false);

        myPanel.revalidate();
        myPanel.repaint();
        opponentPanel.revalidate();
        opponentPanel.repaint();
    }
    private void populateCapturedPanel(JPanel panel, String capturedStr, boolean isMyPanel) {
        if (capturedStr == null || capturedStr.isEmpty()) return;

        Arrays.stream(capturedStr.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .map(Piece::valueOf)   // 문자열 → Piece
              .forEach(piece -> {
                  // 캡쳐 말용 버튼
                  JButton pieceButton = new JButton();

                  // 🔹 버튼 자체 크기(스케일) 줄이기
                  pieceButton.setPreferredSize(new Dimension(36, 36));

                  // 내 패널: 내 말 → 보드랑 같은 방향
                  // 상대 패널: 상대 말 → 180도 뒤집어서 표시
                  boolean rotate180 = !isMyPanel;

                  ImageIcon baseIcon = getPieceIcon(piece, pieceButton, rotate180);

                  if (baseIcon != null) {
                      int iw = baseIcon.getIconWidth();
                      int ih = baseIcon.getIconHeight();
                      int scaledW = (int)(iw * 0.9);   // 90%로 축소
                      int scaledH = (int)(ih * 0.9);

                      Image scaledImg = baseIcon.getImage()
                                                .getScaledInstance(scaledW, scaledH, Image.SCALE_SMOOTH);
                      ImageIcon scaledIcon = new ImageIcon(scaledImg);

                      pieceButton.setIcon(scaledIcon);
                      pieceButton.setToolTipText(piece.getDisplayName());
                      pieceButton.setMargin(new Insets(0, 0, 0, 0));
                      pieceButton.setFocusPainted(false);
                      pieceButton.setContentAreaFilled(false);
                      pieceButton.setBorderPainted(true);
                  }

                  if (isMyPanel) {
                      pieceButton.addActionListener(
                          e -> controller.onCapturedPieceClicked(piece, e.getSource())
                      );
                  } else {
                      pieceButton.setEnabled(false);
                  }

                  panel.add(pieceButton);
              });
    }


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
    
    public void highlightSelectedBoardPiece(int r, int c) { 
        int[] viewCoords = modelToView(r, c);
        boardButtons[viewCoords[0]][viewCoords[1]].setBorder(new LineBorder(HIGHLIGHT_SELECTED_PIECE, 2)); 
    }
    public void highlightSelectedCapturedPiece(Object sourceButton) { ((JButton) sourceButton).setBorder(new LineBorder(HIGHLIGHT_CAPTURED_PIECE, 2)); }

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

    public void clearHighlights(boolean clearSelection) {
        if (clearSelection) {
            for (int r = 0; r < 4; r++) for (int c = 0; c < 3; c++) boardButtons[r][c].setBorder(UIManager.getBorder("Button.border"));
        }
        for (int[] cell : validMoveCells) {
            int[] viewCoords = modelToView(cell[0], cell[1]);
            boardButtons[viewCoords[0]][viewCoords[1]].setBackground(UIManager.getColor("Button.background"));
        }
        validMoveCells.clear();
        for (int[] cell : initialHighlightCells) {
            int[] viewCoords = modelToView(cell[0], cell[1]);
            boardButtons[viewCoords[0]][viewCoords[1]].setBackground(UIManager.getColor("Button.background"));
        }
        initialHighlightCells.clear();
    }

    public void resetBoard() {
        updateBoard("");
    }

    private ImageIcon getPieceIcon(Piece piece, JButton btn, boolean rotate180) {
        ImageIcon icon = pieceIcons.computeIfAbsent(piece, k -> {
            String fileName = "/images/" + k.name() + ".png";
            java.net.URL url = getClass().getResource(fileName);
            return (url != null) ? new ImageIcon(url) : null;
        });

        if (icon == null) return null;
        
        Image img = icon.getImage();
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
        int wBtn = btn.getWidth();
        int hBtn = btn.getHeight();
        if (wBtn <= 1) wBtn = 48;
        if (hBtn <= 1) hBtn = 48;
        int scaledWidth = Math.max(1, wBtn - 18);
        int scaledHeight = Math.max(1, hBtn - 18);
        Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }
}
