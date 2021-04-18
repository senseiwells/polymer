package eu.pb4.polymer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;

public class PolymerMod implements ModInitializer {
	private static HashSet<String> BLOCK_ENTITY_IDENTIFIERS = new HashSet<>();

	public static final Logger LOGGER = LogManager.getLogger("Polymer");
	//public static final String VERSION = FabricLoader.getInstance().getModContainer("polymer").get().getMetadata().getVersion().getFriendlyString();

	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			LOGGER.warn("Client isn't supported yet! It might break!");
		}
	}

	public static void registerVirtualBlockEntity(Identifier identifier) {
		BLOCK_ENTITY_IDENTIFIERS.add(identifier.toString());
	}

	public static boolean isVirtualBlockEntity(Identifier identifier) {
		return BLOCK_ENTITY_IDENTIFIERS.contains(identifier.toString());
	}

	public static boolean isVirtualBlockEntity(String identifier) {
		return BLOCK_ENTITY_IDENTIFIERS.contains(identifier);
	}
}
