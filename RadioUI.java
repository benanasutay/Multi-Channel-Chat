import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Single class responsible for the entire client-side user interface.
 * Contains:
 * - Inner static classes for reusable custom-painted Swing components
 * (IosTheme, IosCard, IosButton, IosTextField, IosSpinner, IosScrollBarUI)
 * - The JFrame window, all panels, and layout
 * - Public update methods that RadioClient calls to reflect server data
 * No network or business logic lives here.
 */
public class RadioUI extends JFrame {

    public static final class IosTheme {
        public static final Color BG        = new Color(242, 242, 247);
        public static final Color CARD      = new Color(255, 255, 255);
        public static final Color BLUE      = new Color(0,   122, 255);
        public static final Color GREEN     = new Color(52,  199,  89);
        public static final Color RED       = new Color(255,  59,  48);
        public static final Color GRAY      = new Color(142, 142, 147);
        public static final Color GRAY2     = new Color(174, 174, 178);
        public static final Color LABEL     = new Color(0,     0,   0);
        public static final Color SEC_LABEL = new Color(60,   60,  67, 180);
        public static final Color FILL      = new Color(120, 120, 128,  36);

        public static final Font TITLE   = new Font("Segoe UI", Font.BOLD,  16);
        public static final Font BODY    = new Font("Segoe UI", Font.PLAIN, 15);
        public static final Font SUBHEAD = new Font("Segoe UI", Font.PLAIN, 13);
        public static final Font CAPTION = new Font("Segoe UI", Font.BOLD,  10);

        private IosTheme() {}
    }

    public static class IosCard extends JPanel {
        private final int radius;

        public IosCard(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(IosTheme.CARD);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
        }
    }

    public static class IosButton extends JButton {
        private final Color   baseColor;
        private final boolean pill;
        private boolean hovered = false;
        private boolean pressed = false;

        public IosButton(String text, Color baseColor, boolean pill) {
            super(text);
            this.baseColor = baseColor;
            this.pill      = pill;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(IosTheme.BODY);
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered (MouseEvent e) { hovered = true;              repaint(); }
                @Override public void mouseExited  (MouseEvent e) { hovered = false; pressed = false; repaint(); }
                @Override public void mousePressed (MouseEvent e) { pressed = true;              repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false;             repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg  = pressed ? baseColor.darker() : (hovered ? baseColor.brighter() : baseColor);
            int   arc = pill ? getHeight() : 12;
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            FontMetrics fm = g2.getFontMetrics(getFont());
            int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    public static class IosTextField extends JTextField {

        public IosTextField() {
            setOpaque(false);
            setFont(IosTheme.BODY);
            setForeground(IosTheme.LABEL);
            setCaretColor(IosTheme.BLUE);
            setBorder(new EmptyBorder(10, 14, 10, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(IosTheme.FILL);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            if (isFocusOwner()) {
                g2.setColor(IosTheme.BLUE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.75f, 0.75f,
                        getWidth() - 1.5f, getHeight() - 1.5f, 12, 12));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class IosSpinner extends JPanel {
        private int          value;
        private final int    min;
        private final int    max;
        private final JTextField valueField;

        public IosSpinner(int initial, int min, int max) {
            this.value = initial;
            this.min   = min;
            this.max   = max;
            setOpaque(false);
            setLayout(new BorderLayout(4, 0));

            valueField = new JTextField(String.valueOf(value));
            valueField.setHorizontalAlignment(JTextField.CENTER);
            valueField.setFont(new Font("Segoe UI", Font.BOLD, 17));
            valueField.setForeground(IosTheme.LABEL);
            valueField.setOpaque(false);
            valueField.setBorder(null);
            valueField.setCaretColor(IosTheme.BLUE);

            valueField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    validateAndApplyText();
                }
            });
            valueField.addActionListener(e -> validateAndApplyText());

            IosButton minusBtn = new IosButton("\u2212", IosTheme.GRAY2, false);
            minusBtn.setPreferredSize(new Dimension(36, 36));
            minusBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            minusBtn.addActionListener(e -> decrement());

            IosButton plusBtn = new IosButton("+", IosTheme.GRAY2, false);
            plusBtn.setPreferredSize(new Dimension(36, 36));
            plusBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            plusBtn.addActionListener(e -> increment());

            add(minusBtn,   BorderLayout.WEST);
            add(valueField, BorderLayout.CENTER);
            add(plusBtn,    BorderLayout.EAST);
        }

        private void validateAndApplyText() {
            try {
                int parsed = Integer.parseInt(valueField.getText().trim());
                if (parsed >= min && parsed <= max) {
                    setValue(parsed);
                } else {
                    valueField.setText(String.valueOf(value));
                }
            } catch (NumberFormatException ex) {
                valueField.setText(String.valueOf(value));
            }
        }

        public int  getValue() {
            validateAndApplyText();
            return value;
        }

        public void setValue(int newValue) {
            if (newValue >= min && newValue <= max) {
                value = newValue;
                valueField.setText(String.valueOf(value));
            }
        }

        private void decrement() { setValue(value - 1); }
        private void increment() { setValue(value + 1); }
    }


    public static class IosScrollBarUI extends BasicScrollBarUI {

        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(0, 0, 0, 55);
        }

        @Override protected JButton createDecreaseButton(int o) { return ghostButton(); }
        @Override protected JButton createIncreaseButton(int o) { return ghostButton(); }

        private JButton ghostButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setOpaque(false);
            return b;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fill(new RoundRectangle2D.Float(
                    r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6));
            g2.dispose();
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
    }


    private JTextPane      chatPane;
    private StyledDocument chatDoc;
    private IosTextField   inputField;
    private IosSpinner     channelSpinner;
    private JTextArea      channelInfoArea;
    private JLabel         channelBadge;
    private JLabel         connectionDot;

    // Callbacks wired in by RadioClient
    private final ActionListener onSend;
    private final ActionListener onTune;
    private final ActionListener onReturn16;


    /**
     * @param onSend      fired when user clicks Transmit or presses Enter
     * @param onTune      fired when user clicks Tune
     * @param onReturn16  fired when user clicks "Return to CH 16"
     */
    public RadioUI(ActionListener onSend, ActionListener onTune, ActionListener onReturn16) {
        super("Radio");
        this.onSend     = onSend;
        this.onTune     = onTune;
        this.onReturn16 = onReturn16;
        buildWindow();
    }

    private void buildWindow() {
        setSize(930, 650);
        setMinimumSize(new Dimension(760, 500));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(IosTheme.BG);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildNavBar(),   BorderLayout.NORTH);
        root.add(buildCenter(),   BorderLayout.CENTER);
        root.add(buildInputBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    //  Navigation bar

    private JPanel buildNavBar() {
        IosCard nav = new IosCard(14);
        nav.setLayout(new BorderLayout());
        nav.setBorder(new EmptyBorder(10, 16, 10, 16));
        nav.setPreferredSize(new Dimension(0, 52));
        nav.add(buildNavLeft(),   BorderLayout.WEST);
        nav.add(buildNavCenter(), BorderLayout.CENTER);
        nav.add(buildNavRight(),  BorderLayout.EAST);
        return nav;
    }

    private JPanel buildNavLeft() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        connectionDot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getForeground());
                int s = 10;
                g2.fillOval((getWidth() - s) / 2, (getHeight() - s) / 2, s, s);
                g2.dispose();
            }
        };
        connectionDot.setPreferredSize(new Dimension(14, 14));
        connectionDot.setForeground(IosTheme.RED);

        JLabel title = new JLabel("Wireless Radio");
        title.setFont(IosTheme.TITLE);
        title.setForeground(IosTheme.LABEL);

        left.add(connectionDot);
        left.add(title);
        return left;
    }

    private JPanel buildNavCenter() {
        channelBadge = new JLabel("CH 16") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(IosTheme.BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                g2.drawString(getText(),
                        (w - fm.stringWidth(getText())) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        channelBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        channelBadge.setPreferredSize(new Dimension(72, 28));
        channelBadge.setOpaque(false);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(channelBadge);
        return center;
    }

    private JPanel buildNavRight() {
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(130, 0));
        return right;
    }

    //  Center split

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(10, 0, 10, 0));
        center.add(buildChatPanel(), BorderLayout.CENTER);
        center.add(buildSidePanel(), BorderLayout.EAST);
        return center;
    }

    private JPanel buildChatPanel() {
        IosCard card = new IosCard(14);
        card.setLayout(new BorderLayout());

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setOpaque(false);
        chatPane.setBorder(new EmptyBorder(12, 14, 12, 14));
        chatDoc = chatPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new IosScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setVisible(false);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(218, 0));

        // Active channels list
        side.add(sectionLabel("ACTIVE CHANNELS"));
        side.add(buildChannelInfoCard());

        // Tune controls
        side.add(sectionLabel("RADIO CONTROL"));
        side.add(buildTuneCard());
        side.add(Box.createRigidArea(new Dimension(0, 8)));
        side.add(buildReturnCard());

        return side;
    }

    private JPanel buildChannelInfoCard() {
        IosCard card = new IosCard(14);
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(218, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        channelInfoArea = new JTextArea("Connecting...");
        channelInfoArea.setEditable(false);
        channelInfoArea.setOpaque(false);
        channelInfoArea.setFont(IosTheme.SUBHEAD);
        channelInfoArea.setForeground(IosTheme.SEC_LABEL);
        channelInfoArea.setBorder(new EmptyBorder(10, 14, 10, 14));
        channelInfoArea.setLineWrap(true);
        channelInfoArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(channelInfoArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new IosScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTuneCard() {
        IosCard card = new IosCard(14);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.setMaximumSize(new Dimension(218, 132));
        card.setPreferredSize(new Dimension(218, 130));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tuneTitle = new JLabel("Tune Channel");
        tuneTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tuneTitle.setForeground(IosTheme.LABEL);

        channelSpinner = new IosSpinner(16, 1, 100);

        IosButton tuneBtn = new IosButton("Tune", IosTheme.BLUE, true);
        tuneBtn.setPreferredSize(new Dimension(0, 36));
        tuneBtn.addActionListener(onTune);

        card.add(tuneTitle,      BorderLayout.NORTH);
        card.add(channelSpinner, BorderLayout.CENTER);
        card.add(tuneBtn,        BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildReturnCard() {
        IosCard card = new IosCard(14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.setMaximumSize(new Dimension(218, 54));
        card.setPreferredSize(new Dimension(218, 50));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        IosButton returnBtn = new IosButton("Return to CH 16", IosTheme.GREEN, true);
        returnBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        returnBtn.addActionListener(onReturn16);

        card.add(returnBtn, BorderLayout.CENTER);
        return card;
    }

    //  Input bar

    private JPanel buildInputBar() {
        IosCard bar = new IosCard(14);
        bar.setLayout(new BorderLayout(10, 0));
        bar.setBorder(new EmptyBorder(10, 12, 10, 12));
        bar.setPreferredSize(new Dimension(0, 58));

        inputField = new IosTextField();
        inputField.addActionListener(onSend);

        IosButton sendBtn = new IosButton("Transmit", IosTheme.BLUE, true);
        sendBtn.setPreferredSize(new Dimension(100, 38));
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendBtn.addActionListener(onSend);

        bar.add(inputField, BorderLayout.CENTER);
        bar.add(sendBtn,    BorderLayout.EAST);
        return bar;
    }

    //  Helper

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel("  " + text);
        lbl.setFont(IosTheme.CAPTION);
        lbl.setForeground(IosTheme.GRAY);
        lbl.setBorder(new EmptyBorder(10, 0, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }


    /** Appends a chat message to the log */
    public void appendChatMessage(String user, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String ts = new SimpleDateFormat("HH:mm").format(new Date());
                SimpleAttributeSet a = new SimpleAttributeSet();

                StyleConstants.setForeground(a, IosTheme.GRAY2);
                StyleConstants.setFontFamily(a, "Segoe UI");
                StyleConstants.setFontSize(a, 11);
                StyleConstants.setBold(a, false);
                chatDoc.insertString(chatDoc.getLength(), ts + "  ", a);

                StyleConstants.setForeground(a, IosTheme.BLUE);
                StyleConstants.setFontSize(a, 14);
                StyleConstants.setBold(a, true);
                chatDoc.insertString(chatDoc.getLength(), user, a);

                StyleConstants.setForeground(a, IosTheme.LABEL);
                StyleConstants.setFontFamily(a, "Consolas");
                StyleConstants.setFontSize(a, 14);
                StyleConstants.setBold(a, false);
                chatDoc.insertString(chatDoc.getLength(), "  " + message + "\n", a);

                chatPane.setCaretPosition(chatDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /** Appends a grey italic system notification (join / leave / disconnect) */
    public void appendSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setForeground(a, IosTheme.GRAY);
                StyleConstants.setFontFamily(a, "Segoe UI");
                StyleConstants.setFontSize(a, 12);
                StyleConstants.setItalic(a, true);
                chatDoc.insertString(chatDoc.getLength(), "  " + text + "\n", a);
                chatPane.setCaretPosition(chatDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /** Appends a blue channel-change divider line */
    public void appendChannelDivider(int channelNumber) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet a = new SimpleAttributeSet();
                StyleConstants.setForeground(a, IosTheme.BLUE);
                StyleConstants.setFontFamily(a, "Segoe UI");
                StyleConstants.setFontSize(a, 13);
                StyleConstants.setBold(a, true);
                chatDoc.insertString(chatDoc.getLength(),
                        "\n\u2500\u2500\u2500\u2500 Tuned to Channel " + channelNumber + " \u2500\u2500\u2500\u2500\n\n", a);
                chatPane.setCaretPosition(chatDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /** Replaces the active-channels list in the sidebar */
    public void updateChannelList(String displayText) {
        SwingUtilities.invokeLater(() ->
                channelInfoArea.setText(displayText.isEmpty() ? "(no active channels)" : displayText));
    }

    /** Green dot + refreshes title and badge */
    public void setConnected(String username, int channel) {
        SwingUtilities.invokeLater(() -> {
            connectionDot.setForeground(IosTheme.GREEN);
            connectionDot.repaint();
            refreshBadge(username, channel);
        });
    }

    /** Red dot */
    public void setDisconnected() {
        SwingUtilities.invokeLater(() -> {
            connectionDot.setForeground(IosTheme.RED);
            connectionDot.repaint();
        });
    }

    /** Updates the channel pill and window title */
    public void refreshBadge(String username, int channel) {
        SwingUtilities.invokeLater(() -> {
            channelBadge.setText("CH " + channel);
            channelBadge.setPreferredSize(new Dimension(channel >= 100 ? 84 : 72, 28));
            channelBadge.repaint();
            setTitle("Radio \u2014 " + username + "  |  CH " + channel);
        });
    }

    //  Accessors used by RadioClient

    public String getInputText()        { return inputField.getText().trim(); }
    public void   clearInput()          { inputField.setText(""); }
    public int    getSpinnerValue()     { return channelSpinner.getValue(); }
    public void   setSpinnerValue(int v){ channelSpinner.setValue(v); }
}