package su.kidoz.kaillera.model.event;

/**
 * Default implementation of EventDispatcher that forwards events to a
 * registered listener.
 *
 * <p>
 * This implementation is thread-safe: the listener reference is volatile to
 * ensure visibility across threads.
 *
 * @see EventDispatcher
 */
public class DefaultEventDispatcher implements EventDispatcher {

    private volatile KailleraEventListener listener;

    @Override
    public void dispatch(KailleraEvent event) {
        KailleraEventListener l = this.listener;
        if (l != null) {
            l.actionPerformed(event);
        }
    }

    @Override
    public void setListener(KailleraEventListener listener) {
        this.listener = listener;
    }

    @Override
    public KailleraEventListener getListener() {
        return listener;
    }
}
