package net.interplay.simplestoragemanager.logic;

import java.util.Locale;

public final class StorageSearch {
    private StorageSearch() {
    }

    public static boolean matches(String name, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return name != null && name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }
}
