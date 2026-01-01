package org.emulinker.kaillera.model.event;

public interface KailleraEventListener {
    void actionPerformed(KailleraEvent event);

    void stop();
}
