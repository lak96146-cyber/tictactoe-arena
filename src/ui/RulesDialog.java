package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog displaying official game rules, series formats, and system mechanics.
 */
public class RulesDialog extends JDialog {

    public RulesDialog(JFrame parent) {
        super(parent, "Arena Regulations & Rules", true);
        initUI();
    }

    private void initUI() {
        setSize(520, 560);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Theme.BG_PANEL);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("ARENA COMBAT MANUAL", SwingConstants.CENTER);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT_CYAN);

        JEditorPane rulesPane = new JEditorPane();
        rulesPane.setContentType("text/html");
        rulesPane.setEditable(false);
        rulesPane.setOpaque(false);
        rulesPane.setText("<html><body style='font-family:Segoe UI, sans-serif; color:#CBD5E1; font-size:12px; line-height:1.5;'>"
                + "<h3 style='color:#00F0FF; margin-bottom:4px;'>1. CORE OBJECTIVE</h3>"
                + "Place 3 of your assigned marks (<b>X</b> or <b>O</b>) in a continuous horizontal, vertical, or diagonal line on the 3&times;3 arena board."
                + "<h3 style='color:#9D4EDD; margin-bottom:4px; margin-top:12px;'>2. AUTHORITATIVE ARBITRATION</h3>"
                + "All moves are validated in real-time by the central Java Game Server. Moves out of turn, invalid bounds, or occupied cell overwrites are strictly rejected."
                + "<h3 style='color:#3A86FF; margin-bottom:4px; margin-top:12px;'>3. SERIES FORMATS</h3>"
                + "<ul>"
                + "<li><b>Best of 1 (BO1):</b> Sudden death. Winner of round 1 wins the match.</li>"
                + "<li><b>Best of 3 (BO3):</b> Standard championship format. First combatant to win 2 rounds wins the match.</li>"
                + "<li><b>Best of 5 (BO5):</b> Premier endurance series. First to claim 3 rounds wins.</li>"
                + "</ul>"
                + "<h3 style='color:#00FF87; margin-bottom:4px; margin-top:12px;'>4. TIE & ROUND CYCLES</h3>"
                + "If all 9 squares are occupied without a 3-in-a-row alignment, the round ends in a Draw. The starting symbol alternates automatically in subsequent rounds."
                + "<h3 style='color:#FFB800; margin-bottom:4px; margin-top:12px;'>5. REMATCH & LEADERBOARD</h3>"
                + "Following match conclusion, players may request an immediate rematch requiring mutual consent. All official match results are permanently recorded to historical archives."
                + "</body></html>"
        );

        JScrollPane scroll = new JScrollPane(rulesPane);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));
        scroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        ModernComponents.ModernButton closeBtn = new ModernComponents.ModernButton("ACKNOWLEDGE", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        closeBtn.setPreferredSize(new Dimension(140, 38));
        closeBtn.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }
}
