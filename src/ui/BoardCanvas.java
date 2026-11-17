package ui;

import audio.SoundManager;
import game.GameBoard;
import game.PlayerSymbol;
import game.WinningLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom-painted high-performance 3x3 Tic-Tac-Toe Arena board.
 * Features glowing vector X & O graphics, hover previews, responsive layout,
 * and animated winning line effects.
 */
public class BoardCanvas extends JComponent {

    public interface CellClickListener {
        void onCellClicked(int row, int col);
    }

    private GameBoard board = new GameBoard();
    private PlayerSymbol mySymbol = PlayerSymbol.NONE;
    private boolean isMyTurn = false;
    private WinningLine winningLine = null;
    private CellClickListener cellClickListener;

    private int hoveredRow = -1;
    private int hoveredCol = -1;

    // Animation timer for pulse effects
    private final Timer pulseTimer;
    private float pulsePhase = 0.0f;
    private boolean pulseAscending = true;

    public BoardCanvas() {
        setOpaque(false);
        setFocusable(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Smooth pulse animation (approx 30 FPS)
        pulseTimer = new Timer(33, e -> {
            if (pulseAscending) {
                pulsePhase += 0.04f;
                if (pulsePhase >= 1.0f) {
                    pulsePhase = 1.0f;
                    pulseAscending = false;
                }
            } else {
                pulsePhase -= 0.04f;
                if (pulsePhase <= 0.2f) {
                    pulsePhase = 0.2f;
                    pulseAscending = true;
                }
            }
            if (winningLine != null || isMyTurn) {
                repaint();
            }
        });
        pulseTimer.start();

        // Mouse listeners for hover and click
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] cell = getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    if (hoveredRow != cell[0] || hoveredCol != cell[1]) {
                        hoveredRow = cell[0];
                        hoveredCol = cell[1];
                        repaint();
                    }
                } else {
                    if (hoveredRow != -1 || hoveredCol != -1) {
                        hoveredRow = -1;
                        hoveredCol = -1;
                        repaint();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                hoveredCol = -1;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isMyTurn || cellClickListener == null) return;
                int[] cell = getCellAt(e.getX(), e.getY());
                if (cell != null) {
                    int r = cell[0];
                    int c = cell[1];
                    if (board.isCellEmpty(r, c)) {
                        SoundManager.getInstance().playMove();
                        cellClickListener.onCellClicked(r, c);
                    } else {
                        SoundManager.getInstance().playError();
                    }
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public synchronized void updateState(GameBoard newBoard, PlayerSymbol mySymbol, boolean isMyTurn, WinningLine line) {
        this.board = (newBoard != null) ? newBoard.copy() : new GameBoard();
        this.mySymbol = mySymbol;
        this.isMyTurn = isMyTurn;
        this.winningLine = line;
        repaint();
    }

    public synchronized void setWinningLine(WinningLine line) {
        this.winningLine = line;
        repaint();
    }

    public synchronized void resetBoard() {
        this.board.reset();
        this.winningLine = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        Theme.applyQualityRendering(g2d);

        int width = getWidth();
        int height = getHeight();

        // Compute square board dimensions with padding
        int padding = 20;
        int boardSize = Math.min(width, height) - (padding * 2);
        if (boardSize < 120) boardSize = 120;

        int startX = (width - boardSize) / 2;
        int startY = (height - boardSize) / 2;
        int cellSize = boardSize / 3;

        // 1. Draw Board Background Panel
        g2d.setColor(Theme.BG_PANEL);
        g2d.fill(new RoundRectangle2D.Float(startX - 10, startY - 10, boardSize + 20, boardSize + 20, 24, 24));

        g2d.setColor(Theme.BORDER_COLOR);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new RoundRectangle2D.Float(startX - 10, startY - 10, boardSize + 20, boardSize + 20, 24, 24));

        // 2. Draw Cells & Hover Previews
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int cellX = startX + c * cellSize;
                int cellY = startY + r * cellSize;

                boolean isHovered = (r == hoveredRow && c == hoveredCol && isMyTurn && board.isCellEmpty(r, c));
                boolean isWinningCell = (winningLine != null && winningLine.contains(r, c));

                // Cell background highlight
                if (isWinningCell) {
                    Color winGlow = new Color(0, 255, 135, (int) (40 + 40 * pulsePhase));
                    g2d.setColor(winGlow);
                    g2d.fillRoundRect(cellX + 4, cellY + 4, cellSize - 8, cellSize - 8, 16, 16);
                } else if (isHovered) {
                    Color hoverGlow = (mySymbol == PlayerSymbol.X) ?
                            new Color(255, 0, 85, 30) : new Color(0, 240, 255, 30);
                    g2d.setColor(hoverGlow);
                    g2d.fillRoundRect(cellX + 4, cellY + 4, cellSize - 8, cellSize - 8, 16, 16);
                }

                // Render placed symbol
                PlayerSymbol symbol = board.getCell(r, c);
                if (symbol == PlayerSymbol.X) {
                    drawGlowingX(g2d, cellX, cellY, cellSize, 1.0f);
                } else if (symbol == PlayerSymbol.O) {
                    drawGlowingO(g2d, cellX, cellY, cellSize, 1.0f);
                } else if (isHovered && mySymbol != PlayerSymbol.NONE) {
                    // Semi-transparent ghost preview
                    if (mySymbol == PlayerSymbol.X) {
                        drawGlowingX(g2d, cellX, cellY, cellSize, 0.35f);
                    } else {
                        drawGlowingO(g2d, cellX, cellY, cellSize, 0.35f);
                    }
                }
            }
        }

        // 3. Draw Grid Lines with Soft Glowing Bloom
        drawGridLines(g2d, startX, startY, boardSize, cellSize);

        // 4. Draw Winning Strike-through Line if exists
        if (winningLine != null) {
            drawWinningLine(g2d, startX, startY, cellSize);
        }

        g2d.dispose();
    }

    private void drawGridLines(Graphics2D g2d, int startX, int startY, int boardSize, int cellSize) {
        // Subtle outer glow
        g2d.setColor(new Color(92, 115, 240, 40));
        g2d.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawGridSegments(g2d, startX, startY, boardSize, cellSize);

        // Inner crisp neon line
        g2d.setColor(Theme.BORDER_ACTIVE);
        g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawGridSegments(g2d, startX, startY, boardSize, cellSize);
    }

    private void drawGridSegments(Graphics2D g2d, int startX, int startY, int boardSize, int cellSize) {
        // Vertical lines
        for (int i = 1; i <= 2; i++) {
            int x = startX + i * cellSize;
            g2d.drawLine(x, startY + 12, x, startY + boardSize - 12);
        }
        // Horizontal lines
        for (int i = 1; i <= 2; i++) {
            int y = startY + i * cellSize;
            g2d.drawLine(startX + 12, y, startX + boardSize - 12, y);
        }
    }

    private void drawGlowingX(Graphics2D g2d, int cellX, int cellY, int cellSize, float alpha) {
        int pad = (int) (cellSize * 0.22);
        int x1 = cellX + pad;
        int y1 = cellY + pad;
        int x2 = cellX + cellSize - pad;
        int y2 = cellY + cellSize - pad;

        Color base = Theme.ACCENT_NEON_PINK;
        int aBloom = (int) (60 * alpha);
        int aCore = (int) (255 * alpha);

        // Multi-pass neon bloom
        g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), aBloom));
        g2d.setStroke(new BasicStroke(16.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
        g2d.drawLine(x1, y2, x2, y1);

        g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (120 * alpha)));
        g2d.setStroke(new BasicStroke(9.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
        g2d.drawLine(x1, y2, x2, y1);

        // Crisp inner core
        g2d.setColor(new Color(255, 230, 240, aCore));
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
        g2d.drawLine(x1, y2, x2, y1);
    }

    private void drawGlowingO(Graphics2D g2d, int cellX, int cellY, int cellSize, float alpha) {
        int pad = (int) (cellSize * 0.22);
        int d = cellSize - (pad * 2);
        int x = cellX + pad;
        int y = cellY + pad;

        Color base = Theme.ACCENT_CYAN;
        int aBloom = (int) (60 * alpha);
        int aCore = (int) (255 * alpha);

        // Multi-pass neon bloom
        g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), aBloom));
        g2d.setStroke(new BasicStroke(16.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(new Ellipse2D.Float(x, y, d, d));

        g2d.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (120 * alpha)));
        g2d.setStroke(new BasicStroke(9.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(new Ellipse2D.Float(x, y, d, d));

        // Crisp inner core
        g2d.setColor(new Color(230, 255, 255, aCore));
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(new Ellipse2D.Float(x, y, d, d));
    }

    private void drawWinningLine(Graphics2D g2d, int startX, int startY, int cellSize) {
        int x1 = startX + winningLine.c1 * cellSize + cellSize / 2;
        int y1 = startY + winningLine.r1 * cellSize + cellSize / 2;
        int x2 = startX + winningLine.c3 * cellSize + cellSize / 2;
        int y2 = startY + winningLine.r3 * cellSize + cellSize / 2;

        Color green = Theme.ACCENT_GREEN;
        int pulseAlpha = (int) (180 + 75 * pulsePhase);

        // Outer neon aura
        g2d.setColor(new Color(green.getRed(), green.getGreen(), green.getBlue(), (int) (50 * pulsePhase)));
        g2d.setStroke(new BasicStroke(22.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);

        // Mid glow
        g2d.setColor(new Color(green.getRed(), green.getGreen(), green.getBlue(), pulseAlpha));
        g2d.setStroke(new BasicStroke(10.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);

        // Hot center core
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    private int[] getCellAt(int px, int py) {
        int width = getWidth();
        int height = getHeight();
        int padding = 20;
        int boardSize = Math.min(width, height) - (padding * 2);
        if (boardSize < 120) boardSize = 120;

        int startX = (width - boardSize) / 2;
        int startY = (height - boardSize) / 2;
        int cellSize = boardSize / 3;

        if (px >= startX && px < startX + boardSize && py >= startY && py < startY + boardSize) {
            int col = (px - startX) / cellSize;
            int row = (py - startY) / cellSize;
            if (row >= 0 && row < 3 && col >= 0 && col < 3) {
                return new int[]{row, col};
            }
        }
        return null;
    }
}
