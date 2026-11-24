package net.interplay.simplestoragemanager.integration;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;

import java.lang.reflect.Method;

/**
 * Keeps a single recipe-viewer overlay while the terminal GUI is open.
 *
 * <p>Priority is always <b>EMI &gt; REI &gt; JEI</b>. When several viewers are
 * installed, only the highest-priority one may show its overlay inside the
 * terminal; lower-priority overlays are hidden and restored when the GUI closes.
 * With one viewer (or none) installed nothing changes. A viewer's own state is
 * never forced on: if the player had already disabled the winner's overlay, the
 * lower-priority ones stay hidden as well, so no overlay appears at all.
 *
 * <p>Detection uses mod ids only, so no recipe viewer is a required dependency.
 * REI has a real 20.0.811 Fabric plugin in this build, but its overlay is still
 * toggled through guarded reflection on {@code ConfigObject} (isOverlayVisible/
 * setOverlayVisible/getHideKeybind) to keep the rule optional. EMI and JEI are
 * not supported here: missing classes make their support helpers safe no-ops.
 * Any reflection failure is logged at debug level and changes nothing.
 *
 * <p>The listeners are registered by {@link #register()}; the class also
 * registers itself when loaded, so either style is safe.
 */
public final class RecipeViewerPriority {

    private static final String MOD_EMI = "emi";
    private static final String MOD_REI = "roughlyenoughitems";
    private static final String MOD_JEI = "jei";

    private enum Viewer { NONE, JEI, REI, EMI }

    private static final boolean EMI_LOADED = FabricLoader.getInstance().isModLoaded(MOD_EMI);
    private static final boolean REI_LOADED = FabricLoader.getInstance().isModLoaded(MOD_REI);
    private static final boolean JEI_LOADED = FabricLoader.getInstance().isModLoaded(MOD_JEI);

    private static final Viewer ACTIVE =
            EMI_LOADED ? Viewer.EMI : REI_LOADED ? Viewer.REI : JEI_LOADED ? Viewer.JEI : Viewer.NONE;

    private static boolean registered;
    private static boolean suppressing;
    private static boolean jeiSuppressed;
    private static boolean reiSuppressed;
    private static Boolean savedJeiEnabled;
    private static Boolean savedReiEnabled;
    private static Screen boundScreen;

    private RecipeViewerPriority() {
    }

    /** Installs the Fabric client listeners once. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Screen init also runs on resize: bind only once per screen instance.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof StorageManagerGUIScreen) {
                onTerminalOpen(screen);
            }
        });

        // Safety net for screens opened/replaced without a matching init callback.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Screen current = client.screen;
            if (current instanceof StorageManagerGUIScreen) {
                if (boundScreen != current) {
                    onTerminalOpen(current);
                }
            } else if (boundScreen != null) {
                onTerminalClose();
            }
        });
    }

    static {
        register();
    }

    private static void onTerminalOpen(Screen screen) {
        if (boundScreen == screen) {
            return;
        }
        restore();
        boundScreen = screen;

        // Per-screen listeners disappear with the screen instance.
        ScreenEvents.remove(screen).register(removed -> {
            if (removed == boundScreen) {
                onTerminalClose();
            }
        });
        ScreenKeyboardEvents.allowKeyPress(screen).register((current, key, scanCode, modifiers) ->
            !isSuppressedToggleKey(key, scanCode));

        if (ACTIVE != Viewer.NONE) {
            apply();
        }
    }

    private static void onTerminalClose() {
        restore();
        boundScreen = null;
    }

    /**
     * Blocks only the key that would switch a suppressed overlay back on.
     * Recipe/uses/favorite keys must stay untouched: they are shared with the
     * winning viewer and with the terminal GUI itself.
     */
    private static boolean isSuppressedToggleKey(int keyCode, int scanCode) {
        if (!suppressing) {
            return false;
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (jeiSuppressed && JeiSupport.matchesToggleKey(key)) {
            return true;
        }
        return reiSuppressed && ReiSupport.matchesHideKey(keyCode, scanCode);
    }

    private static void apply() {
        switch (ACTIVE) {
            case EMI -> {
                if (REI_LOADED && ReiSupport.canSuppress()) {
                    savedReiEnabled = ReiSupport.isEnabled();
                    if (savedReiEnabled != null) {
                        ReiSupport.setEnabled(false);
                        reiSuppressed = true;
                    }
                }
                if (JEI_LOADED && JeiSupport.canSuppress()) {
                    savedJeiEnabled = JeiSupport.isEnabled();
                    if (savedJeiEnabled != null) {
                        JeiSupport.setEnabled(false);
                        jeiSuppressed = true;
                    }
                }
            }
            case REI -> {
                if (JEI_LOADED && JeiSupport.canSuppress()) {
                    savedJeiEnabled = JeiSupport.isEnabled();
                    if (savedJeiEnabled != null) {
                        JeiSupport.setEnabled(false);
                        jeiSuppressed = true;
                    }
                }
            }
            default -> {
                // The winner's own visibility is never changed.
            }
        }
        suppressing = true;
    }

    private static void restore() {
        if (!suppressing) {
            return;
        }
        if (savedJeiEnabled != null) {
            JeiSupport.setEnabled(savedJeiEnabled);
        }
        if (savedReiEnabled != null) {
            ReiSupport.setEnabled(savedReiEnabled);
        }
        savedJeiEnabled = null;
        savedReiEnabled = null;
        jeiSuppressed = false;
        reiSuppressed = false;
        suppressing = false;
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            type.getMethod(name, parameters);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** JEI internals are resolved lazily and never required at compile time. */
    private static final class JeiSupport {
        private static final String INTERNAL = "mezz.jei.common.Internal";
        private static Boolean available;

        private static Class<?> internal() throws Exception {
            return Class.forName(INTERNAL);
        }

        private static Object state() throws Exception {
            return internal().getMethod("getClientToggleState").invoke(null);
        }

        private static Object keyMappings() throws Exception {
            return internal().getMethod("getKeyMappings").invoke(null);
        }

        /** Only suppress JEI if state control and the toggle key can both be resolved. */
        static boolean canSuppress() {
            if (available != null) {
                return available;
            }
            try {
                Object toggleState = state();
                Class<?> stateClass = toggleState.getClass();
                stateClass.getMethod("isOverlayEnabled");
                boolean canToggle = hasMethod(stateClass, "setOverlayEnabled", boolean.class)
                        || hasMethod(stateClass, "toggleOverlayEnabled");
                Object mappings = keyMappings();
                Object toggle = mappings.getClass().getMethod("getToggleOverlay").invoke(mappings);
                hasMethod(toggle.getClass(), "matchesKey", InputConstants.Key.class);
                available = canToggle;
            } catch (Throwable t) {
                SimplestoragemanagerMod.LOGGER.debug("JEI overlay control unavailable; leaving it alone", t);
                available = false;
            }
            return available;
        }

        static Boolean isEnabled() {
            try {
                Object toggleState = state();
                return (Boolean) toggleState.getClass().getMethod("isOverlayEnabled").invoke(toggleState);
            } catch (Throwable t) {
                return null;
            }
        }

        static void setEnabled(boolean value) {
            try {
                Object toggleState = state();
                Class<?> stateClass = toggleState.getClass();
                Boolean current = (Boolean) stateClass.getMethod("isOverlayEnabled").invoke(toggleState);
                if (current != null && current == value) {
                    return;
                }
                try {
                    stateClass.getMethod("setOverlayEnabled", boolean.class).invoke(toggleState, value);
                } catch (NoSuchMethodException absent) {
                    // JEI 1.21.8 exposes only toggleOverlayEnabled() on IClientToggleState.
                    stateClass.getMethod("toggleOverlayEnabled").invoke(toggleState);
                }
            } catch (Throwable t) {
                SimplestoragemanagerMod.LOGGER.debug("Could not toggle JEI overlay", t);
            }
        }

        static boolean matchesToggleKey(InputConstants.Key key) {
            try {
                Object mappings = keyMappings();
                Object toggle = mappings.getClass().getMethod("getToggleOverlay").invoke(mappings);
                Method matches = toggle.getClass().getMethod("matchesKey", InputConstants.Key.class);
                return Boolean.TRUE.equals(matches.invoke(toggle, key));
            } catch (Throwable t) {
                return false;
            }
        }
    }

    /** REI 20.0.811 expõe ConfigObject; reflexão protegida mantém a integração opcional. */
    private static final class ReiSupport {
        private static final String CONFIG = "me.shedaniel.rei.api.client.config.ConfigObject";
        private static Boolean available;

        private static Object config() throws Exception {
            return Class.forName(CONFIG).getMethod("getInstance").invoke(null);
        }

        static boolean canSuppress() {
            if (available != null) {
                return available;
            }
            try {
                Object cfg = config();
                Class<?> cfgClass = cfg.getClass();
                cfgClass.getMethod("isOverlayVisible");
                cfgClass.getMethod("setOverlayVisible", boolean.class);
                Object keybind = cfgClass.getMethod("getHideKeybind").invoke(cfg);
                keybind.getClass().getMethod("matchesKey", int.class, int.class);
                available = true;
            } catch (Throwable t) {
                SimplestoragemanagerMod.LOGGER.debug("REI overlay control unavailable; leaving it alone", t);
                available = false;
            }
            return available;
        }

        static Boolean isEnabled() {
            try {
                Object cfg = config();
                return (Boolean) cfg.getClass().getMethod("isOverlayVisible").invoke(cfg);
            } catch (Throwable t) {
                return null;
            }
        }

        static void setEnabled(boolean value) {
            try {
                Object cfg = config();
                cfg.getClass().getMethod("setOverlayVisible", boolean.class).invoke(cfg, value);
            } catch (Throwable t) {
                SimplestoragemanagerMod.LOGGER.debug("Could not toggle REI overlay", t);
            }
        }

        static boolean matchesHideKey(int keyCode, int scanCode) {
            try {
                Object cfg = config();
                Object keybind = cfg.getClass().getMethod("getHideKeybind").invoke(cfg);
                Method matches = keybind.getClass().getMethod("matchesKey", int.class, int.class);
                return Boolean.TRUE.equals(matches.invoke(keybind, keyCode, scanCode));
            } catch (Throwable t) {
                return false;
            }
        }
    }
}
