package org.emulinker.kaillera.model.event;

/**
 * Base interface for all Kaillera events.
 *
 * <p>
 * The sub-interfaces GameEvent, ServerEvent, and UserEvent are sealed to
 * restrict their implementations to known event types, enabling exhaustive
 * pattern matching.
 */
public interface KailleraEvent {
    String toString();
}
