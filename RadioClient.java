import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**

 * Entry point for the client side of the Wireless Radio application.
 * Responsibilities:
 *   - Connect to the RadioServer over TCP
 *   - Run a background thread that reads server messages
 *   - Dispatch parsed messages to RadioUI for display
 *   - Send user actions (CHANGE_CHANNEL, CHAT) back to the server

 * No Swing code lives here; all UI work is delegated to RadioUI.
 */
public class RadioClient {

    //  Network
    private static final int SERVER_PORT = 8080;

    private PrintWriter    out;
    private BufferedReader in;
    private Socket         socket;

    //  State
    private int    currentChannel = ChannelManager.COMMON_CHANNEL;
    private String username       = "";

    private RadioUI ui;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RadioClient().launch());
    }

    private void launch() {
        // Build the UI, passing callbacks for user actions
        ui = new RadioUI(
                e -> transmit(),
                e -> changeChannel(),
                e -> returnToCommon()
        );

        ui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { gracefulExit(); }
        });

        ui.setLocationRelativeTo(null);
        ui.setVisible(true);

        // Network setup happens after the window is shown
        connectToServer();
    }

    private void transmit() {
        if (out == null) return;
        String msg = ui.getInputText();
        if (!msg.isEmpty()) {
            out.println(msg);
            ui.clearInput();
        }
    }

    private void changeChannel() {
        if (out == null) return;
        int target = ui.getSpinnerValue();
        if (target == currentChannel) {
            ui.appendSystemMessage("Already on channel " + currentChannel + ".");
            return;
        }
        currentChannel = target;
        out.println("CHANGE_CHANNEL " + currentChannel);
        ui.appendChannelDivider(currentChannel);
        ui.refreshBadge(username, currentChannel);
    }

    private void returnToCommon() {
        ui.setSpinnerValue(ChannelManager.COMMON_CHANNEL);
        changeChannel();
    }

    private void gracefulExit() {
        try { if (out    != null) out.close();    } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        System.exit(0);
    }

    private void connectToServer() {
        //  Step 1: server IP
        String ip = iosPrompt("Connect to Server", "Enter server IP address:", "localhost");
        if (ip == null || ip.trim().isEmpty()) System.exit(0);
        ip = ip.trim();

        //  Step 2: open socket
        try {
            socket = new Socket(ip, SERVER_PORT);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(ui,
                    "Cannot connect to " + ip + ":" + SERVER_PORT + "\n" + ex.getMessage(),
                    "Connection Failed", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        //  Step 3: username
        String name = iosPrompt("Callsign", "Enter your callsign (username):", "");
        if (name == null || name.trim().isEmpty()) System.exit(0);
        username = name.trim();

        //  Step 4: background reader thread
        Thread listener = new Thread(this::readLoop, "ServerListener");
        listener.setDaemon(true);
        listener.start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException ex) {
            ui.appendSystemMessage("Connection to server lost.");
            ui.setDisconnected();
        }
    }

    /**
     * Routes a raw server message to the correct handler.
     * All messages use a simple prefix protocol:
     *   SYS   — system notification
     *   CHAT  — chat message from another user
     *   STATE — full channel-occupancy snapshot
     *   ERROR — fatal server error
     */
    private void handleServerMessage(String message) {
        if (message.startsWith("SYS Please enter your username:")) {
            out.println(username);
            ui.setConnected(username, currentChannel);

        } else if (message.startsWith("STATE ")) {
            handleStateMessage(message.substring(6));

        } else if (message.startsWith("CHAT ")) {
            handleChatMessage(message.substring(5));

        } else if (message.startsWith("SYS ")) {
            ui.appendSystemMessage(message.substring(4));

        } else if (message.startsWith("ERROR ")) {
            final String err = message.substring(6);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(ui, err, "Server Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            });
        }
    }

    private void handleStateMessage(String payload) {

        String[] parts = payload.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty() && !part.equals("EMPTY")) {
                sb.append(part
                    .replace("Ch",  "CH ")
                    .replace(":[",  "  \u2192  [")
                    .replace(",",   ", "))
                  .append("\n\n");
            }
        }
        ui.updateChannelList(sb.toString().trim());
    }

    private void handleChatMessage(String text) {

        int colon = text.indexOf(": ");
        String user = colon > 0 ? text.substring(0, colon)  : "?";
        String msg  = colon > 0 ? text.substring(colon + 2) : text;
        ui.appendChatMessage(user, msg);
    }

    /**
     * Displays a modal prompt using our IosTextField for visual consistency.
     * Returns null if the user cancels.
     */
    private String iosPrompt(String title, String prompt, String defaultValue) {
        RadioUI.IosTextField field = new RadioUI.IosTextField();
        field.setText(defaultValue);
        field.setPreferredSize(new java.awt.Dimension(270, 42));

        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = new JLabel(prompt);
        label.setFont(RadioUI.IosTheme.BODY);
        panel.add(label, java.awt.BorderLayout.NORTH);
        panel.add(field, java.awt.BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                ui, panel, title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        return (result == JOptionPane.OK_OPTION) ? field.getText() : null;
    }
}
