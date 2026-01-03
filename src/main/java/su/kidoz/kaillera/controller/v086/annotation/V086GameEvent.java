package su.kidoz.kaillera.controller.v086.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import su.kidoz.kaillera.model.event.GameEvent;

/**
 * Maps a game event type to its V086 event renderer.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface V086GameEvent {
    Class<? extends GameEvent> eventType();
}
