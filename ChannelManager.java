import java.util.*;
import java.util.concurrent.*;

/**
 * ChannelManager
 * Manages the state of all 100 radio channels.
 * Follows the Single Responsibility Principle: this class only
 * tracks which users are on which channel and notifies listeners.
 */
public class ChannelManager {

    public static final int COMMON_CHANNEL = 16;
    public static final int MIN_CHANNEL    = 1;
    public static final int MAX_CHANNEL    = 100;

    /** channel number → list of usernames currently on that channel */
    private final Map<Integer, List<String>> channelMap = new ConcurrentHashMap<>();

    /** Callback interface so ChannelManager can notify the server to push STATE */
    public interface StateListener {
        void onStateChanged();
    }

    private StateListener stateListener;

    //  Constructor

    public ChannelManager() {
        for (int i = MIN_CHANNEL; i <= MAX_CHANNEL; i++) {
            channelMap.put(i, new CopyOnWriteArrayList<>());
        }
    }

    //  Listener

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    //  Mutations

    public synchronized void addUser(int channel, String username) {
        if (isValidChannel(channel)) {
            channelMap.get(channel).add(username);
            notifyListener();
        }
    }

    public synchronized void removeUser(int channel, String username) {
        List<String> users = channelMap.get(channel);
        if (users != null) {
            users.remove(username);
            notifyListener();
        }
    }

    //  Queries

    public boolean isValidChannel(int channel) {
        return channel >= MIN_CHANNEL && channel <= MAX_CHANNEL;
    }

    public boolean isChannelOccupied(int channel) {
        List<String> users = channelMap.get(channel);
        return users != null && !users.isEmpty();
    }

    public List<String> getUsersOnChannel(int channel) {
        List<String> users = channelMap.get(channel);
        return users != null ? Collections.unmodifiableList(users) : Collections.emptyList();
    }

    /**
     * Builds the full STATE string that is broadcast to all clients.
     */
    public String buildStateMessage() {
        StringBuilder sb = new StringBuilder("STATE ");
        List<Integer> sorted = new ArrayList<>(channelMap.keySet());
        Collections.sort(sorted);
        boolean any = false;
        for (int ch : sorted) {
            List<String> users = channelMap.get(ch);
            if (!users.isEmpty()) {
                sb.append("Ch").append(ch)
                  .append(":[").append(String.join(",", users)).append("]|");
                any = true;
            }
        }
        if (!any) sb.append("EMPTY");
        return sb.toString();
    }

    //  Private

    private void notifyListener() {
        if (stateListener != null) stateListener.onStateChanged();
    }
}
