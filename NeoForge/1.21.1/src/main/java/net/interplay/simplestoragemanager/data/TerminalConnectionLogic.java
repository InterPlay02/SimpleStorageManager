package net.interplay.simplestoragemanager.data;

import java.util.ArrayList;
import java.util.List;

/** Pure list operations used by the server interaction handler and unit tests. */
public final class TerminalConnectionLogic {
    private TerminalConnectionLogic() {
    }

    public static ToggleResult toggle(List<TerminalConnection> existing, TerminalConnection target) {
        List<TerminalConnection> result = new ArrayList<>(existing);
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).containsBlock(target.dimension(), target.pos())) {
                result.remove(i);
                return new ToggleResult(List.copyOf(result), false);
            }
        }
        result.add(target);
        return new ToggleResult(List.copyOf(result), true);
    }

    public record ToggleResult(List<TerminalConnection> connections, boolean connected) {
    }
}
