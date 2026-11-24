package net.interplay.simplestoragemanager.logic;

/** Maps the configured GUI scale option to the terminal texture variant. */
public final class TerminalScaleChoice {
    private TerminalScaleChoice() { }

    public static boolean useBig(int configuredScale) {
        return configuredScale == 1 || configuredScale == 2;
    }
}
