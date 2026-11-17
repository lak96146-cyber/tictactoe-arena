package ui;

import audio.SoundManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Suite of modern custom Swing components styled for the futuristic gaming theme.
 */
public class ModernComponents {

    /**
     * Modern rounded button with hover state, glow borders, and optional sound trigger.
     */
    public static class ModernButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private Color textColor = Theme.TEXT_PRIMARY;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private int cornerRadius = 12;
        private boolean glowingBorder = false;

        public ModernButton(String text) {
            this(text, Theme.BG_CARD, Theme.BORDER_ACTIVE);
        }

        public ModernButton(String text, Color baseColor, Color hoverColor) {
            super(text);
            this.baseColor = baseColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setFont(Theme.FONT_BODY_BOLD);
            setForeground(textColor);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) {
                        isHovered = true;
                        SoundManager.getInstance().playClick();
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        isPressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }

        public void setGlowingBorder(boolean glow) {
            this.glowingBorder = glow;
            repaint();
        }

        public void setBaseColor(Color c) {
            this.baseColor = c;
            repaint();
        }

        public void setHoverColor(Color c) {
            this.hoverColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            Theme.applyQualityRendering(g2d);

            int width = getWidth();
            int height = getHeight();

            Color fill = !isEnabled() ? Theme.BG_PANEL.darker() :
                    (isPressed ? hoverColor.darker() : (isHovered ? hoverColor : baseColor));

            // Background
            g2d.setColor(fill);
            g2d.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Border
            if (glowingBorder || isHovered) {
                g2d.setColor(Theme.ACCENT_PURPLE);
                g2d.setStroke(new BasicStroke(1.5f));
            } else {
                g2d.setColor(Theme.BORDER_COLOR);
                g2d.setStroke(new BasicStroke(1.0f));
            }
            g2d.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Text
            FontMetrics fm = g2d.getFontMetrics(getFont());
            int textX = (width - fm.stringWidth(getText())) / 2;
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;

            g2d.setFont(getFont());
            g2d.setColor(isEnabled() ? (isHovered ? Color.WHITE : getForeground()) : Theme.TEXT_MUTED);
            g2d.drawString(getText(), textX, textY);

            g2d.dispose();
        }
    }

    /**
     * Modern text field with dark background, rounded corners, placeholder support and focus glow.
     */
    public static class ModernTextField extends JTextField {
        private String placeholder;
        private boolean isFocused = false;
        private int cornerRadius = 10;

        public ModernTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(Theme.FONT_BODY);
            setForeground(Theme.TEXT_PRIMARY);
            setCaretColor(Theme.ACCENT_CYAN);
            setBorder(new EmptyBorder(10, 14, 10, 14));

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    isFocused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    isFocused = false;
                    repaint();
                }
            });
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            Theme.applyQualityRendering(g2d);

            int width = getWidth();
            int height = getHeight();

            // Background
            g2d.setColor(Theme.BG_INPUT);
            g2d.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Border
            if (isFocused) {
                g2d.setColor(Theme.BORDER_ACTIVE);
                g2d.setStroke(new BasicStroke(1.5f));
            } else {
                g2d.setColor(Theme.BORDER_COLOR);
                g2d.setStroke(new BasicStroke(1.0f));
            }
            g2d.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Draw placeholder if empty
            if (getText().isEmpty() && !isFocused && placeholder != null) {
                g2d.setFont(getFont());
                g2d.setColor(Theme.TEXT_MUTED);
                FontMetrics fm = g2d.getFontMetrics();
                int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(placeholder, 14, textY);
            }

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Rounded container panel with gaming theme styling.
     */
    public static class ModernPanel extends JPanel {
        private int cornerRadius = 16;
        private Color bgColor = Theme.BG_PANEL;
        private Color borderColor = Theme.BORDER_COLOR;

        public ModernPanel() {
            this(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        }

        public ModernPanel(int cornerRadius, Color bgColor, Color borderColor) {
            this.cornerRadius = cornerRadius;
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        public void setBgColor(Color c) {
            this.bgColor = c;
            repaint();
        }

        public void setBorderColor(Color c) {
            this.borderColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            Theme.applyQualityRendering(g2d);

            int width = getWidth();
            int height = getHeight();

            // Background
            g2d.setColor(bgColor);
            g2d.fill(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));

            // Border
            if (borderColor != null) {
                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.draw(new RoundRectangle2D.Float(1, 1, width - 2, height - 2, cornerRadius, cornerRadius));
            }

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Pill-shaped badge for displaying match mode, status, or role.
     */
    public static class BadgeLabel extends JLabel {
        private Color badgeColor;
        private Color textColor;

        public BadgeLabel(String text, Color badgeColor, Color textColor) {
            super(text, SwingConstants.CENTER);
            this.badgeColor = badgeColor;
            this.textColor = textColor;
            setFont(Theme.FONT_SMALL_BOLD);
            setForeground(textColor);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setOpaque(false);
        }

        public void setColors(Color badgeColor, Color textColor) {
            this.badgeColor = badgeColor;
            this.textColor = textColor;
            setForeground(textColor);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            Theme.applyQualityRendering(g2d);

            int width = getWidth();
            int height = getHeight();

            // Pill background
            g2d.setColor(badgeColor);
            g2d.fillRoundRect(0, 0, width, height, height, height);

            g2d.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Minimalist dark scrollbar UI for modern scroll panes.
     */
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = Theme.BORDER_COLOR;
            this.trackColor = Theme.BG_DARK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityRendering(g2);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 8, 8);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }
    }
}
