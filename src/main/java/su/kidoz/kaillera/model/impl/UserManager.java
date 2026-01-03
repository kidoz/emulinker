package su.kidoz.kaillera.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages user storage, ID generation, and user lifecycle tracking. Extracted
 * from KailleraServerImpl to improve separation of concerns.
 */
public class UserManager {
    private static final int MAX_USER_ID = 0xFFFF;

    private final Map<Integer, KailleraUserImpl> users;
    private final AtomicInteger connectionCounter;

    public UserManager(int initialCapacity) {
        this.users = new ConcurrentHashMap<>(initialCapacity);
        this.connectionCounter = new AtomicInteger(1);
    }

    /**
     * Generates the next unique user ID, wrapping around at MAX_USER_ID.
     */
    public int getNextUserID() {
        for (int attempts = 0; attempts < MAX_USER_ID; attempts++) {
            int candidate = connectionCounter.getAndUpdate(val -> val >= MAX_USER_ID ? 1 : val + 1);
            if (!users.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No available user IDs");
    }

    /**
     * Adds a user to the managed collection.
     */
    public void addUser(KailleraUserImpl user) {
        users.put(user.getID(), user);
    }

    /**
     * Retrieves a user by ID.
     */
    public KailleraUserImpl getUser(int userID) {
        return users.get(userID);
    }

    /**
     * Removes a user by ID.
     *
     * @return the removed user, or null if not found
     */
    public KailleraUserImpl removeUser(int userID) {
        return users.remove(userID);
    }

    /**
     * Checks if a user exists.
     */
    public boolean containsUser(int userID) {
        return users.containsKey(userID);
    }

    /**
     * Returns an unmodifiable view of all users.
     */
    public Collection<KailleraUserImpl> getUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    /**
     * Returns the number of users.
     */
    public int getNumUsers() {
        return users.size();
    }

    /**
     * Checks if there are any users.
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * Stops all users and clears the collection.
     */
    public void stopAllUsers() {
        for (KailleraUserImpl user : users.values()) {
            user.stop();
        }
        users.clear();
    }

    /**
     * Clears all users without stopping them.
     */
    public void clear() {
        users.clear();
    }
}
