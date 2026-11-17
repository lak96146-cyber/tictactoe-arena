package ui;

import audio.SoundManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Settings dialog for toggling sound effects, viewing audio status, and inspecting client config.
 */
public class SettingsDialog extends JDialog {

    public SettingsDialog(JFrame parent) {
        super(parent, "Arena Settings & Configuration", true);
        initUI();
    }

    private void initUI() {
        setSize(420, 380);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Theme.BG_PANEL);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("SYSTEM SETTINGS", SwingConstants.CENTER);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sound Toggle Card
        ModernComponents.ModernPanel soundCard = new ModernComponents.ModernPanel(14, Theme.BG_CARD, Theme.BORDER_COLOR);
        soundCard.setLayout(new BorderLayout(10, 0));
        soundCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        soundCard.setMaximumSize(new Dimension(360, 60));

        JLabel soundLabel = new JLabel("SYNTHESIZED SOUND EFFECTS");
        soundLabel.setFont(Theme.FONT_SMALL_BOLD);
        soundLabel.setForeground(Theme.TEXT_PRIMARY);

        boolean muted = SoundManager.getInstance().isMuted();
        ModernComponents.ModernButton audioBtn = new ModernComponents.ModernButton(
                muted ? "MUTED 🔇" : "ENABLED 🔊",
                muted ? Theme.BG_PANEL : Theme.ACCENT_GREEN.darker(),
                Theme.ACCENT_GREEN
        );
        audioBtn.setPreferredSize(new Dimension(110, 34));
        audioBtn.addActionListener(e -> {
            SoundManager.getInstance().toggleMute();
            boolean isM = SoundManager.getInstance().isMuted();
            audioBtn.setText(isM ? "MUTED 🔇" : "ENABLED 🔊");
            audioBtn.setBaseColor(isM ? Theme.BG_PANEL : Theme.ACCENT_GREEN.darker());
        });

        soundCard.add(soundLabel, BorderLayout.CENTER);
        soundCard.add(audioBtn, BorderLayout.EAST);

        // System Info Card
        ModernComponents.ModernPanel infoCard = new ModernComponents.ModernPanel(14, Theme.BG_CARD, Theme.BORDER_COLOR);
        infoCard.setLayout(new GridLayout(3, 1, 0, 6));
        infoCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        infoCard.setMaximumSize(new Dimension(360, 110));

        JLabel jreLabel = new JLabel("Runtime: Java " + System.getProperty("java.version"));
        jreLabel.setFont(Theme.FONT_SMALL);
        jreLabel.setForeground(Theme.TEXT_SECONDARY);

        JLabel renderLabel = new JLabel("Graphics: Pure Swing 2D Anti-aliased");
        renderLabel.setFont(Theme.FONT_SMALL);
        renderLabel.setForeground(Theme.TEXT_SECONDARY);

        JLabel protoLabel = new JLabel("Protocol: TCP Socket Multi-Client");
        protoLabel.setFont(Theme.FONT_SMALL);
        protoLabel.setForeground(Theme.TEXT_SECONDARY);

        infoCard.add(jreLabel);
        infoCard.add(renderLabel);
        infoCard.add(protoLabel);

        ModernComponents.ModernButton closeBtn = new ModernComponents.ModernButton("SAVE & RETURN", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.setMaximumSize(new Dimension(360, 42));
        closeBtn.addActionListener(e -> dispose());

        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(soundCard);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(infoCard);
        mainPanel.add(Box.createVerticalStrut(24));
        mainPanel.add(closeBtn);

        setContentPane(mainPanel);
    }
}
