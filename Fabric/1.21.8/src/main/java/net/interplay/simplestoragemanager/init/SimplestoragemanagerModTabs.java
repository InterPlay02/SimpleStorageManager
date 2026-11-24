package net.interplay.simplestoragemanager.init;

/** Creative tab entries are added from SimplestoragemanagerModItems.register(). */
public final class SimplestoragemanagerModTabs {
    private SimplestoragemanagerModTabs() { }

    public static void register() {
        // The terminal and upgrades join the vanilla functional blocks tab
        // through ItemGroupEvents, so no custom tab is registered.
    }
}
