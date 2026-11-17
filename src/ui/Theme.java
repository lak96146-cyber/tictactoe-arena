package ui;

import java.awt.*;

/**
 * Design system tokens and styling constants for TIC-TAC-TOE ARENA.
 * Features a dark futuristic gaming theme with deep navy/charcoal backgrounds
 * and neon purple, cyan, electric blue, and emerald accents.
 */
public class Theme {
    // --- Palette ---
    public static final Color BG_DARK = new Color(11, 14, 23);           // #0B0E17 Deep space navy
    public static final Color BG_PANEL = new Color(18, 24, 38);          // #121826 Raised panel
    public static final Color BG_CARD = new Color(25, 33, 52);           // #192134 Interactive card
    public static final Color BG_INPUT = new Color(14, 18, 29);          // #0E121D Input background

    public static final Color BORDER_COLOR = new Color(38, 50, 77);      // #26324D Subtle border
    public static final Color BORDER_ACTIVE = new Color(92, 115, 240);   // #5C73F0 Focused border

    // --- Accents ---
    public static final Color ACCENT_PURPLE = new Color(157, 78, 221);   // #9D4EDD Neon electric purple
    public static final Color ACCENT_CYAN = new Color(0, 240, 255);      // #00F0FF Electric cyan (O Symbol)
    public static final Color ACCENT_BLUE = new Color(58, 134, 255);     // #3A86FF Cobalt electric blue
    public static final Color ACCENT_NEON_PINK = new Color(255, 0, 85);  // #FF0055 Neon pink/red (X Symbol)
    public static final Color ACCENT_GREEN = new Color(0, 255, 135);     // #00FF87 Victory/Ready emerald
    public static final Color ACCENT_AMBER = new Color(255, 184, 0);     // #FFB800 Warning/Draw gold

    // --- Typography Colors ---
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);   // #F8FAFC Crisp white
    public static final Color TEXT_SECONDARY = new Color(148, 163, 184); // #94A3B8 Cool slate
    public static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B Dim gray

    // --- Fonts ---
    private static final String FONT_FAMILY = "Segoe UI";

    public static final Font FONT_DISPLAY = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_SYMBOL = new Font(FONT_FAMILY, Font.BOLD, 48);

    /**
     * Applies high quality anti-aliasing hints to a Graphics2D context.
     */
    public static void applyQualityRendering(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
}
