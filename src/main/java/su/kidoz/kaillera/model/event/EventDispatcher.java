package su.kidoz.kaillera.model.event;

/**
 * Interface for dispatching Kaillera events from the domain model to listeners.
 *
 * <p>
 * This abstraction decouples the domain model (KailleraUserImpl) from the
 * transport layer (V086ClientHandler). The domain model dispatches events
 * through this interface, and the protocol handler receives them via the
 * listener callback.
 *
 * @see KailleraEventListener
 * @see DefaultEventDispatcher
 */
public interface EventDispatcher {

    /**
     * Dispatches an event to the registered listener.
     *
     * @param event
     *            the event to dispatch
     */
    void dispatch(KailleraEvent event);

    /**
     * Sets the listener that will receive dispatched events.
     *
     * @param listener
     *            the event listener to register
     */
    void setListener(KailleraEventListener listener);

    /**
     * Returns the currently registered listener.
     *
     * @return the listener, or null if none registered
     */
    KailleraEventListener getListener();
}
