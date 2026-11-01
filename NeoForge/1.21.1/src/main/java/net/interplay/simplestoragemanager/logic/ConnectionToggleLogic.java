package net.interplay.simplestoragemanager.logic;

import java.util.ArrayList;
import java.util.List;

/** Loader independent connection toggle used by tests and UI-independent code. */
public final class ConnectionToggleLogic {
    private ConnectionToggleLogic() {
    }

    public static ToggleResult toggle(List<ConnectionId> existing, ConnectionId target) {
        List<ConnectionId> result = new ArrayList<>(existing);
        int index = result.indexOf(target);
        if (index >= 0) {
            result.remove(index);
            return new ToggleResult(List.copyOf(result), false);
        }
        result.add(target);
        return new ToggleResult(List.copyOf(result), true);
    }

    public record ConnectionId(String dimension, int x, int y, int z) {
    }

    public record ToggleResult(List<ConnectionId> connections, boolean connected) {
    }
}
