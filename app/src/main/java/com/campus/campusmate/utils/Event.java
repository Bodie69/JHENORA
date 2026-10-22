package com.campus.campusmate.utils;

import androidx.annotation.Nullable;

/**
 * A one-time value delivered through {@link androidx.lifecycle.LiveData}, such as a toast or
 * snackbar message. LiveData re-delivers its last value to every new or re-attached observer;
 * wrapping the value in an Event lets the UI show it once and ignore the replays (e.g. an old
 * error popping up again when returning to a tab or after rotation).
 *
 * @param <T> The wrapped value type.
 */
public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** @return the value the first time it's called, then {@code null} on every later call. */
    @Nullable
    public synchronized T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    /** @return the value regardless of whether it was already handled (for non-UI reactions). */
    public T peekContent() {
        return content;
    }
}
