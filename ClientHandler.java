import java.io.*;
import java.net.*;

/**
 * ClientHandler

 * Each connected client gets its own ClientHandler running on a dedicated thread.
 * Responsible for: reading client messages, routing commands, and sending replies.

 * Communicates with RadioServer via the static broadcast/pushState methods
 * and with ChannelManager for all channel-state mutations.
 */
public class ClientHandler implements Runnable {

    private final Socket         socket;
    private final ChannelManager channelManager;
    private final RadioServer    server;

    private PrintWriter    out;
    private BufferedReader in;

    private String       username;
    private volatile int channel = ChannelManager.COMMON_CHANNEL;

    //  Constructor

    public ClientHandler(Socket socket, ChannelManager channelManager, RadioServer server) {
        this.socket         = socket;
        this.channelManager = channelManager;
        this.server         = server;
    }

    //  Accessors

    public int    getChannel()  { return channel;  }
    public String getUsername() { return username; }

    /** Sends one line to this client. Silently ignores if stream is gone. */
    public void send(String message) {
        if (out != null) out.println(message);
    }

    //  Runnable

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

            username = handshake();

            joinChannel(channel);
            System.out.println("  [+] " + username + " connected on CH" + channel);

            listenLoop();

        } catch (IOException ignored) {
            // client disconnected unexpectedly
        } finally {
            disconnect();
        }
    }

    //  Private — protocol steps

    /**
     * Asks the client for a username and returns it.
     * Falls back to IP-based name if the client sends nothing.
     */
    private String handshake() throws IOException {
        out.println("SYS Please enter your username:");
        String raw = in.readLine();
        if (raw == null || raw.trim().isEmpty()) {
            return "User_" + socket.getInetAddress().getHostAddress();
        }
        return raw.trim();
    }

    /** Reads incoming lines and dispatches to the correct handler. */
    private void listenLoop() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("CHANGE_CHANNEL ")) {
                handleChannelChange(line);
            } else {
                handleChatMessage(line);
            }
        }
    }

    private void handleChannelChange(String line) {
        try {
            int newChannel = Integer.parseInt(line.substring(15).trim());
            if (!channelManager.isValidChannel(newChannel)) {
                send("SYS Invalid channel. Use " +
                        ChannelManager.MIN_CHANNEL + "-" + ChannelManager.MAX_CHANNEL + ".");
                return;
            }
            // Announce departure on old channel
            server.broadcast("SYS " + username + " left the channel.", channel);
            leaveChannel(channel);

            channel = newChannel;
            joinChannel(channel);

            System.out.println("  [~] " + username + " -> CH" + channel);

        } catch (NumberFormatException e) {
            send("SYS Invalid channel number.");
        }
    }

    private void handleChatMessage(String message) {
        server.broadcast("CHAT " + username + ": " + message, channel);
    }

    private void joinChannel(int ch) {
        channelManager.addUser(ch, username);
        server.broadcast("SYS " + username + " joined Channel " + ch, ch);
    }

    private void leaveChannel(int ch) {
        channelManager.removeUser(ch, username);
    }

    private void disconnect() {
        if (username != null) {
            channelManager.removeUser(channel, username);
            server.broadcast("SYS " + username + " disconnected.", channel);
            System.out.println("  [-] " + username + " disconnected.");
        }
        server.removeClient(this);
        try { socket.close(); } catch (IOException ignored) {}
    }
}
