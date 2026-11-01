package net.interplay.simplestoragemanager.logic;

public final class DepositRules {
    private DepositRules() {
    }

    /** Storage is preferred; machine input is a fallback and output slots are never selected. */
    public static boolean prefer(int sourceKind, boolean acceptsItem) {
        return acceptsItem && (sourceKind == 0 || sourceKind == 1);
    }

    public static final int STORAGE = 0;
    public static final int MACHINE_INPUT = 1;
    public static final int MACHINE_OUTPUT = 2;
}
