package com.dre.brewery;

import com.dre.brewery.api.events.IngedientAddEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.hazelcast.HazelcastCacheManager;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.MinecraftVersion;
import com.dre.brewery.utility.Tuple;
import com.hazelcast.collection.IList;
import com.hazelcast.core.HazelcastInstance;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class BCauldron implements Serializable, Ownable {

	@Serial
	private static final long serialVersionUID = 2943204069862239977L;

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
	private static final BreweryPlugin plugin = BreweryPlugin.getInstance();
	private static final HazelcastInstance hazelcast = BreweryPlugin.getHazelcast();

	public static final byte EMPTY = 0, SOME = 1, FULL = 2;
	public static final int PARTICLEPAUSE = 15;
	public static Random particleRandom = new Random();
	private static final Set<UUID> plInteracted = new HashSet<>(); // Interact Event helper


	private UUID owner;
	private UUID id;
	private Block block;
	private BIngredients ingredients = new BIngredients();
	private BCauldronRecipe particleRecipe; // null if we haven't checked, empty if there is none
	private Color particleColor = null;
	private Location particleLocation;
	private int state = 0;
	private boolean changed = false; // Not really needed anymore


	public BCauldron(Block block) {
		this.block = block;
		this.particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
		this.id = UUID.randomUUID();
		this.owner = HazelcastCacheManager.getClusterId();
	}

	public BCauldron(Block block, BIngredients ingredients, int state, UUID id, UUID owner) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		this.id = id;
		this.particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
		this.owner = owner;
	}

	// loading from file
	public BCauldron(Block block, BIngredients ingredients, int state, UUID id) {
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		this.particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
		this.id = id;
		this.owner = HazelcastCacheManager.getClusterId();
	}

	public static BCauldron get(Block block) {
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());

		for (BCauldron cauldron : cauldrons) {
			if (cauldron.getBlock().equals(block)) {
				return cauldron;
			}
		}
		return null;
	}

	public void saveToHazelcast() {
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());
		int i = 0;
		for (BCauldron cauldron : cauldrons) {
			if (cauldron.getId().equals(id)) {
				cauldrons.set(i, this); // OPERATION SAVED
				System.out.println("Cauldron saved to Hazelcast: " + this.id);
				return;
			}
			i++;
		}
	}

	/**
	 * Updates this Cauldron, increasing the cook time and checking for Heatsource
	 *
	 * @return false if Cauldron needs to be removed
	 */
	public boolean onUpdate() {
		System.out.println("updating cauldron");
		System.out.println("BI: " + ingredients);
		// add a minute to cooking time
		if (!BUtil.isChunkLoaded(block)) {
			increaseState();
		} else {
			if (!LegacyUtil.isWaterCauldron(block.getType())) {
				// Catch any WorldEdit etc. removal
				return false;
			}
			// Check if fire still alive
			if (LegacyUtil.isCauldronHeatsource(block.getRelative(BlockFace.DOWN))) {
				increaseState();
			}
		}
		return true;
	}

	/**
	 * Will add a minute to the cooking time
	 */
	public void increaseState() {
		state++;
		System.out.println("State: " + state);
		if (changed) {
			ingredients = ingredients.copy();
			changed = false;
		}
		particleColor = null;
	}

	// add an ingredient to the cauldron
	public void add(ItemStack ingredient, RecipeItem rItem) {
		if (ingredient == null || ingredient.getType() == Material.AIR) return;
		if (changed) {
			ingredients = ingredients.copy();
			changed = false;
		}

		particleRecipe = null;
		particleColor = null;
		ingredients.add(ingredient, rItem);
		block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
		if (state > 0) {
			state--;
		}
		if (BConfig.enableCauldronParticles && !BConfig.minimalParticles) {
			// Few little sparks and lots of water splashes. Offset by 0.2 in x and z
			block.getWorld().spawnParticle(Particle.SPELL_INSTANT, particleLocation,2, 0.2, 0, 0.2);
			block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 10, 0.2, 0, 0.2);
		}
	}

	/**
	 * Get the Block that this BCauldron represents
	 */
	public Block getBlock() {
		return block;
	}

	/**
	 * Get the State (Time in Minutes) that this Cauldron currently has
	 */
	public int getState() {
		return state;
	}


	public BIngredients getIngredients() {
		return ingredients;
	}

	public UUID getId() {
		return id;
	}


	// get cauldron from block and add given ingredient
	// Calls the IngredientAddEvent and may be cancelled or changed
	public static boolean ingredientAdd(Block block, ItemStack ingredient, Player player) {
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());

		// if not empty
		if (LegacyUtil.getFillLevel(block) != EMPTY) {

			if (!BCauldronRecipe.acceptedMaterials.contains(ingredient.getType()) && !ingredient.hasItemMeta()) {
				// Extremely fast way to check for most items
				return false;
			}
			// If the Item is on the list, or customized, we have to do more checks
			RecipeItem rItem = RecipeItem.getMatchingRecipeItem(ingredient, false);
			if (rItem == null) {
				return false;
			}

			BCauldron bcauldron = get(block);
			if (bcauldron == null) {
				bcauldron = new BCauldron(block);
				cauldrons.add(bcauldron);
			}

			IngedientAddEvent event = new IngedientAddEvent(player, block, bcauldron, ingredient.clone(), rItem);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				bcauldron.add(event.getIngredient(), event.getRecipeItem());
				bcauldron.saveToHazelcast(); // OPERATION SAVED
				return event.willTakeItem();
			} else {
				return false;
			}
		}
		return false;
	}

	// fills players bottle with cooked brew
	public boolean fill(Player player, Block block) {
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());
		BCauldron bCauldron = get(block);


		if (!player.hasPermission("brewery.cauldron.fill")) {
			plugin.msg(player, plugin.languageReader.get("Perms_NoCauldronFill"));
			return true;
		}
		ItemStack potion = ingredients.cook(state, player.getName());
		if (potion == null) return false;

		if (VERSION.isOrLater(MinecraftVersion.V1_13)) {
			BlockData data = block.getBlockData();
			if (!(data instanceof Levelled cauldron)) {
				cauldrons.remove(bCauldron);
				return false;
			}
            if (cauldron.getLevel() <= 0) {
				cauldrons.remove(bCauldron);
				return false;
			}

			// If the Water_Cauldron type exists and the cauldron is on last level
			if (LegacyUtil.WATER_CAULDRON != null && cauldron.getLevel() == 1) {
				// Empty Cauldron
				block.setType(Material.CAULDRON);
				cauldrons.remove(bCauldron);
			} else {
				cauldron.setLevel(cauldron.getLevel() - 1);

				// Update the new Level to the Block
				// We have to use the BlockData variable "data" here instead of the casted "cauldron"
				// otherwise < 1.13 crashes on plugin load for not finding the BlockData Class
				block.setBlockData(data);

				if (cauldron.getLevel() <= 0) {
					cauldrons.remove(bCauldron);
				} else {
					changed = true;
				}
			}

		} else {
			@SuppressWarnings("deprecation")
			byte data = block.getData();
			if (data > 3) {
				data = 3;
			} else if (data <= 0) {
				cauldrons.remove(bCauldron);
				return false;
			}
			data -= 1;
			LegacyUtil.setData(block, data);

			if (data == 0) {
				cauldrons.remove(bCauldron);
			} else {
				changed = true;
			}
		}
		if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
			block.getWorld().playSound(block.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
		}
		// Bukkit Bug, inventory not updating while in event so this
		// will delay the give
		// but could also just use deprecated updateInventory()
		giveItem(player, potion);
		return true;
	}

	// prints the current cooking time to the player
	public static void printTime(Player player, Block block) {
		if (!player.hasPermission("brewery.cauldron.time")) {
			plugin.msg(player, plugin.languageReader.get("Error_NoPermissions"));
			return;
		}
		BCauldron bcauldron = get(block);
		if (bcauldron != null) {
			if (bcauldron.state > 1) {
				plugin.msg(player, plugin.languageReader.get("Player_CauldronInfo1", "" + bcauldron.state));
			} else {
				plugin.msg(player, plugin.languageReader.get("Player_CauldronInfo2"));
			}
		}
	}

	public void cookEffect() {
		if (BUtil.isChunkLoaded(block) && LegacyUtil.isCauldronHeatsource(block.getRelative(BlockFace.DOWN))) {
			Color color = getParticleColor();
			// Colorable spirally spell, 0 count enables color instead of the offset variables
			// Configurable RGB color. The last parameter seems to control the hue and motion, but I couldn't find
			// how exactly in the client code. 1025 seems to be the best for color brightness and upwards motion

			if (VERSION.isOrLater(MinecraftVersion.V1_21)) {
				block.getWorld().spawnParticle(Particle.SPELL_MOB, getRandParticleLoc(), 0, color);
			} else {
				block.getWorld().spawnParticle(Particle.SPELL_MOB, getRandParticleLoc(), 0,
						((double) color.getRed()) / 255.0,
						((double) color.getGreen()) / 255.0,
						((double) color.getBlue()) / 255.0,
						1025.0);
			}

			if (BConfig.minimalParticles) {
				return;
			}

			if (particleRandom.nextFloat() > 0.85) {
				// Dark pixely smoke cloud at 0.4 random in x and z
				// 0 count enables direction, send to y = 1 with speed 0.09
				block.getWorld().spawnParticle(Particle.SMOKE_LARGE, getRandParticleLoc(), 0, 0, 1, 0, 0.09);
			}
			if (particleRandom.nextFloat() > 0.2) {
				// A Water Splash with 0.2 offset in x and z
				block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 1, 0.2, 0, 0.2);
			}

			if (VERSION.isOrLater(MinecraftVersion.V1_13) && particleRandom.nextFloat() > 0.4) {
				// Two hovering pixely dust clouds, a bit of offset and with DustOptions to give some color and size
				block.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 2, 0.15, 0.2, 0.15, new Particle.DustOptions(color, 1.5f));
			}
		}
	}

	private Location getRandParticleLoc() {
		return new Location(particleLocation.getWorld(),
			particleLocation.getX() + (particleRandom.nextDouble() * 0.8) - 0.4,
			particleLocation.getY(),
			particleLocation.getZ() + (particleRandom.nextDouble() * 0.8) - 0.4);
	}

	/**
	 * Get or calculate the particle color from the current best Cauldron Recipe
	 * Also calculates the best Cauldron Recipe if not yet done
	 *
	 * @return the Particle Color, after potentially calculating it
	 */
	@NotNull
	public Color getParticleColor() {
		if (state < 1) {
			return Color.fromRGB(153, 221, 255); // Bright Blue
		}
		if (particleColor != null) {
			return particleColor;
		}
		if (particleRecipe == null) {
			// Check for Cauldron Recipe
			particleRecipe = ingredients.getCauldronRecipe();
		}

		List<Tuple<Integer, Color>> colorList = null;
		if (particleRecipe != null) {
			colorList = particleRecipe.getParticleColorTuple();
		}

		if (colorList == null || colorList.isEmpty()) {
			// No color List configured, or no recipe found
			colorList = new ArrayList<>(1);
			colorList.add(new Tuple<>(10, Color.fromRGB(77, 166, 255))); // Dark Aqua kind of Blue
		}
		int index = 0;
		while (index < colorList.size() - 1 && colorList.get(index).a() < state) {
			// Find the first index where the colorList Minute is higher than the state
			index++;
		}

		int minute = colorList.get(index).a();
		if (minute > state) {
			// going towards the minute
			int prevPos;
			Color prevColor;
			if (index > 0) {
				// has previous colours
				prevPos = colorList.get(index - 1).a();
				prevColor = colorList.get(index - 1).b();
			} else {
				prevPos = 0;
				prevColor = Color.fromRGB(153, 221, 255); // Bright Blue
			}

			particleColor = BUtil.weightedMixColor(prevColor, prevPos, state, colorList.get(index).b(), minute);
		} else if (minute == state) {
			// reached the minute
			particleColor = colorList.get(index).b();
		} else {
			// passed the last minute configured
			if (index > 0) {
				// We have more than one color, just use the last one
				particleColor = colorList.get(index).b();
			} else {
				// Only have one color, go towards a Gray
				Color nextColor = Color.fromRGB(138, 153, 168); // Dark Teal, Gray
				int nextPos = (int) (minute * 2.6f);

				if (nextPos <= state) {
					// We are past the next color (Gray) as well, keep using it
					particleColor = nextColor;
				} else {
					particleColor = BUtil.weightedMixColor(colorList.get(index).b(), minute, state, nextColor, nextPos);
				}
			}
		}
		//P.p.log("RGB: " + particleColor.getRed() + "|" + particleColor.getGreen() + "|" + particleColor.getBlue());
		return particleColor;
	}

	public static void processCookEffects() {
		 IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());

		if (!BConfig.enableCauldronParticles) return;
		if (cauldrons.isEmpty()) {
			return;
		}
		final float chance = 1f / PARTICLEPAUSE;

		for (BCauldron cauldron : cauldrons) {
			if (particleRandom.nextFloat() < chance) {
				BreweryPlugin.getScheduler().runTask(cauldron.block.getLocation(), cauldron::cookEffect);
			}
		}
	}

	public static void clickCauldron(PlayerInteractEvent event) {
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();
		Player player = event.getPlayer();
		Block clickedBlock = event.getClickedBlock();
		assert clickedBlock != null;

		if (materialInHand == Material.AIR || materialInHand == Material.BUCKET) {
			return;

		} else if (materialInHand == LegacyUtil.CLOCK) {
			printTime(player, clickedBlock);
			return;

			// fill a glass bottle with potion
		} else if (materialInHand == Material.GLASS_BOTTLE) {
			assert item != null;
			if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
				BCauldron bcauldron = get(clickedBlock);
				if (bcauldron != null) {
					if (bcauldron.fill(player, clickedBlock)) {
						event.setCancelled(true);
						if (player.hasPermission("brewery.cauldron.fill")) {
							if (item.getAmount() > 1) {
								item.setAmount(item.getAmount() - 1);
							} else {
								BUtil.setItemInHand(event, Material.AIR, false);
							}
						}
					}
				}
			} else {
				event.setCancelled(true);
			}
			return;

			// Ignore Water Buckets
		} else if (materialInHand == Material.WATER_BUCKET) {
			if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) {
				// reset < 1.9 cauldron when refilling to prevent unlimited source of potions
				// We catch >=1.9 cases in the Cauldron Listener
				if (LegacyUtil.getFillLevel(clickedBlock) == 1) {
					// will only remove when existing
					BCauldron.remove(clickedBlock);
				}
			}
			return;
		}

		// Check if fire alive below cauldron when adding ingredients
		Block down = clickedBlock.getRelative(BlockFace.DOWN);
		if (LegacyUtil.isCauldronHeatsource(down)) {

			event.setCancelled(true);
			boolean handSwap = false;

			// Interact event is called twice!!!?? in 1.9, once for each hand.
			// Certain Items in Hand cause one of them to be cancelled or not called at all sometimes.
			// We mark if a player had the event for the main hand
			// If not, we handle the main hand in the event for the offhand
			if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
				if (event.getHand() == EquipmentSlot.HAND) {
					final UUID id = player.getUniqueId();
					plInteracted.add(id);
					BreweryPlugin.getScheduler().runTask(() -> plInteracted.remove(id));
				} else if (event.getHand() == EquipmentSlot.OFF_HAND) {
					if (!plInteracted.remove(player.getUniqueId())) {
						item = player.getInventory().getItemInMainHand();
						if (item.getType() != Material.AIR) {
							handSwap = true;
						} else {
							item = BConfig.useOffhandForCauldron ? event.getItem() : null;
						}
					}
				}
			}
			if (item == null) return;

			if (!player.hasPermission("brewery.cauldron.insert")) {
				plugin.msg(player, plugin.languageReader.get("Perms_NoCauldronInsert"));
				return;
			}
			if (ingredientAdd(clickedBlock, item, player)) {
				boolean isBucket = item.getType().name().endsWith("_BUCKET");
				boolean isBottle = LegacyUtil.isBottle(item.getType());
				if (item.getAmount() > 1) {
					item.setAmount(item.getAmount() - 1);

					if (isBucket) {
						giveItem(player, new ItemStack(Material.BUCKET));
					} else if (isBottle) {
						giveItem(player, new ItemStack(Material.GLASS_BOTTLE));
					}
				} else {
					if (isBucket) {
						BUtil.setItemInHand(event, Material.BUCKET, handSwap);
					} else if (isBottle) {
						BUtil.setItemInHand(event, Material.GLASS_BOTTLE, handSwap);
					} else {
						BUtil.setItemInHand(event, Material.AIR, handSwap);
					}
				}
			}
		}


	}

	/**
	 * Recalculate the Cauldron Particle Recipe
	 */
	public static void reload() {
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());

		int i = 0;
		for (BCauldron cauldron : cauldrons) {
			cauldron.particleRecipe = null;
			cauldron.particleColor = null;
			if (BConfig.enableCauldronParticles) {
				if (BUtil.isChunkLoaded(cauldron.block) && LegacyUtil.isCauldronHeatsource(cauldron.block.getRelative(BlockFace.DOWN))) {
					cauldron.getParticleColor();
				}
			}
			cauldrons.set(i, cauldron); // OPERATION SAVED
		}
	}

	/**
	 * reset to normal cauldron
 	 */
	public static boolean remove(Block block) {
		BCauldron cauldron = get(block);
		assert cauldron != null;
		IList<BCauldron> cauldrons = hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName());

		for (BCauldron c : cauldrons) {
            if (c.getId().equals(cauldron.id)) {
				cauldrons.remove(c);
				return true;
			}
		}

		//return hazelcast.getList(HazelcastCacheManager.CacheType.CAULDRONS.getHazelcastName()).remove();
		return false;
	}


	// bukkit bug not updating the inventory while executing event, have to
	// schedule the give
	public static void giveItem(final Player player, final ItemStack item) {
		BreweryPlugin.getScheduler().runTaskLater(() -> player.getInventory().addItem(item), 1L);
	}

	@Override
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	@Override
	public UUID getOwner() {
		return owner;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BCauldron cauldron)) return false;
		return Objects.equals(id, cauldron.id);
	}

	@Override
	public String toString() {
		return "BCauldron{" +
			"block=" + block +
			", state=" + state +
			", ingredients=" + ingredients +
			'}';
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(owner);
		out.writeObject(id);
		out.writeObject(DataManager.serializeBlock(block));
		out.writeObject(ingredients);
		out.writeObject(particleRecipe);
		out.writeObject(particleColor != null ? particleColor.serialize() : null);
		out.writeObject(DataManager.serializeLocation(particleLocation));
		out.writeInt(state);
		out.writeBoolean(changed);
	}

	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		owner = (UUID) in.readObject();
		id = (UUID) in.readObject();
		block = DataManager.deserializeBlock((String) in.readObject());
		ingredients = (BIngredients) in.readObject();
		particleRecipe = (BCauldronRecipe) in.readObject();
		final Map<String, Object> particleColorMap = (Map<String, Object>) in.readObject();
		particleColor = particleColorMap != null ? Color.deserialize(particleColorMap) : null;
		particleLocation = DataManager.deserializeLocation((String) in.readObject());
		state = in.readInt();
		changed = in.readBoolean();
	}
}
