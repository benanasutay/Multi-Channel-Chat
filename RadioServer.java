import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * RadioServer

 * Entry point for the server side of the Wireless Radio application.
 * Responsibilities:
 *   - Start the TCP server socket on PORT
 *   - Accept incoming connections (up to MAX_USERS)
 *   - Spawn a ClientHandler thread for each connection
 *   - Provide broadcast() so handlers can send to a whole channel
 *   - Own the ChannelManager and push STATE updates to all clients
 */
public class RadioServer implements ChannelManager.StateListener {

    //  Configuration
    private static final int PORT = 8080;
    private static final int MAX_USERS = 50;

    //  State
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ChannelManager channelManager = new ChannelManager();

    //  Entry point

    public static void main(String[] args) {
        new RadioServer().start();
    }

    //  Lifecycle

    public void start() {
        channelManager.setStateListener(this);
        printStartupInfo();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Waiting for connections...\n");
            while (true) {
                Socket socket = serverSocket.accept();
                handleNewConnection(socket);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleNewConnection(Socket socket) {
        if (clients.size() >= MAX_USERS) {
            rejectConnection(socket);
            return;
        }
        ClientHandler handler = new ClientHandler(socket, channelManager, this);
        clients.add(handler);
        new Thread(handler).start();
        System.out.println("[+] Client connected – total: " + clients.size());
    }

    private void rejectConnection(Socket socket) {
        try {
            PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
            pw.println("ERROR Server is full. Max " + MAX_USERS + " users allowed.");
            socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Sends a message to every client currently on the given channel.
     */
    public void broadcast(String message, int channel) {
        for (ClientHandler client : clients) {
            if (client.getChannel() == channel) {
                client.send(message);
            }
        }
    }

    /**
     * Removes a handler from the active-client set (called on disconnect).
     */
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }

    //  ChannelManager.StateListener

    /**
     * Called by ChannelManager whenever a user joins or leaves a channel.
     * Pushes the updated channel list to every connected client.
     */
    @Override
    public synchronized void onStateChanged() {
        String stateMsg = channelManager.buildStateMessage();
        for (ClientHandler client : clients) {
            client.send(stateMsg);
        }
    }

    //  Startup diagnostics

    private void printStartupInfo() {
        System.out.println("=== Wireless Radio Server ===");
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) {
                        System.out.println("  LAN IP : " + addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {}
        System.out.println("  Port   : " + PORT);
        System.out.println("  Limit  : " + MAX_USERS + " users");
        System.out.println("=============================");
    }
}
