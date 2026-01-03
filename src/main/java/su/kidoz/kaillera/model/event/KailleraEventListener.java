package su.kidoz.kaillera.model.event;

public interface KailleraEventListener {
    void actionPerformed(KailleraEvent event);

    void stop();
}
