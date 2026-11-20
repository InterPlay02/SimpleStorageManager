package net.interplay.simplestoragemanager.integration;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.interplay.simplestoragemanager.SimplestoragemanagerMod;
import net.interplay.simplestoragemanager.client.gui.StorageManagerGUIScreen;

import java.lang.reflect.Method;

/**
 * Priority rule for the three recipe viewers supported by the terminal:
 *
 * <pre>
 *     EMI  &gt;  REI  &gt;  JEI
 * </pre>
 *
 * <p>When more than one of them is installed, only the highest-priority one is allowed to
 * exist while {@link StorageManagerGUIScreen} is open: the other overlays (search bars,
 * ingredient lists, favorites and their shortcuts) are temporarily disabled and restored
 * when the terminal closes. With a single viewer installed nothing is changed.
 *
 * <p>The winner's own visibility switch is never overridden: if the player has disabled the
 * highest-priority viewer's overlay, the lower-priority overlays are still suppressed, so no
 * overlay is shown at all — exactly as if only that viewer were installed. The same applies
 * when REI is the winner and its overlay is disabled.
 *
 * <p>All access to REI/JEI is done reflectively on purpose: this class is registered as a
 * {@code @EventBusSubscriber}, and NeoForge reflects over its methods at startup. Having a
 * compile-only viewer type in a method signature would make class loading fail when that
 * viewer is not installed (e.g. in the development run).
 */
@EventBusSubscriber(modid = SimplestoragemanagerMod.MODID, value = Dist.CLIENT)
public final class RecipeViewerPriority {

    private static final String MOD_EMI = "emi";
    private static final String MOD_REI = "roughlyenoughitems";
    private static final String MOD_JEI = "jei";

    private enum Viewer { NONE, JEI, REI, EMI }

    private static final boolean EMI_LOADED = ModList.get().isLoaded(MOD_EMI);
    private static final boolean REI_LOADED = ModList.get().isLoaded(MOD_REI);
    private static final boolean JEI_LOADED = ModList.get().isLoaded(MOD_JEI);

    private static final Viewer ACTIVE =
            EMI_LOADED ? Viewer.EMI : REI_LOADED ? Viewer.REI : JEI_LOADED ? Viewer.JEI : Viewer.NONE;

    private static boolean suppressing;
    private static boolean jeiSuppressed;
    private static boolean reiSuppressed;

    private static Boolean savedJeiEnabled;
    private static Boolean savedReiEnabled;

    private RecipeViewerPriority() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof StorageManagerGUIScreen) {
            if (ACTIVE != Viewer.NONE) {
                restore();
                apply();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof StorageManagerGUIScreen) {
            restore();
        }
    }

    /**
     * Cancels only the key that would re-enable a suppressed viewer's overlay. Recipe/uses/
     * favorite keys must NOT be cancelled here: they are shared by several viewers and
     * cancelling the event also blocks the winning viewer's shortcuts.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!suppressing || !(event.getScreen() instanceof StorageManagerGUIScreen)) {
            return;
        }

        InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
        if (jeiSuppressed && JeiSupport.matchesToggleKey(key)) {
            event.setCanceled(true);
        }
        if (reiSuppressed && ReiSupport.matchesHideKey(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    private static void apply() {
        switch (ACTIVE) {
            case EMI -> {
                if (REI_LOADED) {
                    savedReiEnabled = ReiSupport.isEnabled();
                    ReiSupport.setEnabled(false);
                    reiSuppressed = true;
                }
                if (JEI_LOADED) {
                    savedJeiEnabled = JeiSupport.isEnabled();
                    JeiSupport.setEnabled(false);
                    jeiSuppressed = true;
                }
            }
            case REI -> {
                if (JEI_LOADED) {
                    savedJeiEnabled = JeiSupport.isEnabled();
                    JeiSupport.setEnabled(false);
                    jeiSuppressed = true;
                }
            }
            default -> {
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

    /** All REI calls go through reflection so REI can be absent at runtime (dev run). */
    private static final class ReiSupport {
        private static Object config() throws Exception {
            return Class.forName("me.shedaniel.rei.api.client.config.ConfigObject")
                    .getMethod("getInstance").invoke(null);
        }

        static boolean isEnabled() {
            try {
                Object cfg = config();
                return (Boolean) cfg.getClass().getMethod("isOverlayVisible").invoke(cfg);
            } catch (Throwable t) {
                return false;
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

        /** Only the key that would switch REI's overlay back on. */
        static boolean matchesHideKey(int keyCode, int scanCode) {
            try {
                Object cfg = config();
                Object keybind = cfg.getClass().getMethod("getHideKeybind").invoke(cfg);
                Method matches = keybind.getClass().getMethod("matchesKey", int.class, int.class);
                return (Boolean) matches.invoke(keybind, keyCode, scanCode);
            } catch (Throwable t) {
                return false;
            }
        }
    }

    /** JEI 19.x has no public overlay switch; its internal toggle state is used instead. */
    private static final class JeiSupport {
        private static Object toggleState() throws Exception {
            return Class.forName("mezz.jei.common.Internal")
                    .getMethod("getClientToggleState").invoke(null);
        }

        static boolean isEnabled() {
            try {
                Object state = toggleState();
                return (Boolean) state.getClass().getMethod("isOverlayEnabled").invoke(state);
            } catch (Throwable t) {
                return false;
            }
        }

        static void setEnabled(boolean value) {
            try {
                Object state = toggleState();
                state.getClass().getMethod("setOverlayEnabled", boolean.class).invoke(state, value);
            } catch (Throwable t) {
                SimplestoragemanagerMod.LOGGER.debug("Could not toggle JEI overlay", t);
            }
        }

        /** Only the key that would switch JEI's overlay back on. */
        static boolean matchesToggleKey(InputConstants.Key key) {
            try {
                Class<?> internal = Class.forName("mezz.jei.common.Internal");
                Object mappings = internal.getMethod("getKeyMappings").invoke(null);
                Object toggle = mappings.getClass().getMethod("getToggleOverlay").invoke(mappings);
                Method matches = toggle.getClass().getMethod("isActiveAndMatches", InputConstants.Key.class);
                return (Boolean) matches.invoke(toggle, key);
            } catch (Throwable t) {
                return false;
            }
        }
    }
}
