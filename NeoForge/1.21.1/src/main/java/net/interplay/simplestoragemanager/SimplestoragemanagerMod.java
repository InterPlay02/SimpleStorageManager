package net.interplay.simplestoragemanager;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.interplay.simplestoragemanager.init.SimplestoragemanagerModTabs;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModMenus;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModItems;
import net.interplay.simplestoragemanager.data.TerminalDataComponents;
import net.interplay.simplestoragemanager.init.SimplestoragemanagerModRecipes;
import net.interplay.simplestoragemanager.world.inventory.StorageManagerGUIMenu;

import java.util.Map;
import java.util.HashMap;

/** Mod bootstrap, payload registration and server-tick connection checks. */
@Mod("simplestoragemanager")
public class SimplestoragemanagerMod {
	public static final Logger LOGGER = LogManager.getLogger(SimplestoragemanagerMod.class);
	public static final String MODID = "simplestoragemanager";

	public SimplestoragemanagerMod(IEventBus modEventBus) {
		// Start of user code block mod constructor
		net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerConfig(
				net.neoforged.fml.config.ModConfig.Type.SERVER,
				net.interplay.simplestoragemanager.config.SimplestoragemanagerConfig.SERVER_SPEC);
		// End of user code block mod constructor
		NeoForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::registerNetworking);
		SimplestoragemanagerModItems.REGISTRY.register(modEventBus);
		SimplestoragemanagerModRecipes.REGISTRY.register(modEventBus);
		TerminalDataComponents.REGISTRY.register(modEventBus);
		SimplestoragemanagerModTabs.REGISTRY.register(modEventBus);
		SimplestoragemanagerModMenus.REGISTRY.register(modEventBus);
		// Start of user code block mod init
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// End of user code block mod methods
	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}

	@SubscribeEvent
	public void tick(ServerTickEvent.Post event) {
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			if (player.containerMenu instanceof StorageManagerGUIMenu menu) {
				menu.tickConnectionAvailability();
			}
		}
	}
}
