package me.Alloca.UnendurableLove.NoTalking;

import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetSilencers implements Listener {
    private final UnendurableLove plugin;

    private final List<Integer> silencerData = new ArrayList<>();
    private final List<Integer> silencerKeys = new ArrayList<>();

    private static final Material silencerMat = Material.LEATHER_HELMET;
    private static final Material silencerKeyMat = Material.STICK;

    public PetSilencers(UnendurableLove plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerRecipe(int model, String name, String[] shape, Material[] ingredients) {
        silencerData.add(model);

        ItemStack result = new ItemStack(silencerMat, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(model);
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + "No pet can talk while wearing this.",
                             ChatColor.GRAY + "*Maybe* it's only for the naughty ones..."));
        result.setItemMeta(meta);
        result.addEnchantment(Enchantment.BINDING_CURSE, 1);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(UnendurableLove.Instance, "pet_silencer_" + model), result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i++) {
            recipe.setIngredient((char) ('A' + i), ingredients[i]);
        }

        Bukkit.addRecipe(recipe);
    }

    private void registerKeyRecipe(int model, String name, String[] shape, Material[] ingredients) {
        silencerKeys.add(model);

        ItemStack result = new ItemStack(silencerKeyMat, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(model);
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to let the bad pet talk again.",
                             ChatColor.GRAY + "Only use if you *really* need to."));
        result.setItemMeta(meta);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(UnendurableLove.Instance, "pet_silencer_key_" + model), result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i++) {
            recipe.setIngredient((char) ('A' + i), ingredients[i]);
        }

        Bukkit.addRecipe(recipe);
    }

    public void registerAll() {
        // string-leather-string
        registerRecipe(1984, "Simple Ballgag", new String[] { "ABA" }, new Material[] { Material.STRING, Material.LEATHER });
        // iron-iron / empty-stick
        registerKeyRecipe(2984, "Ballgag Key", new String[]{ "AA", " B" }, new Material[]{ Material.IRON_INGOT, Material.STICK } );
    }

    public boolean isSilencer(Material material, ItemMeta meta) {
        return material == silencerMat && silencerData.contains(meta.getCustomModelData());
    }

    public boolean isKeyForSilencer(ItemStack silencer, ItemStack key) {
        return isSilencer(silencer.getType(), silencer.getItemMeta()) &&
                key.getType() == silencerKeyMat &&
                silencerKeys.indexOf(key.getItemMeta().getCustomModelData()) == silencerData.indexOf(silencer.getItemMeta().getCustomModelData());
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEntityEvent evt) {
        Player owner = evt.getPlayer();
        EquipmentSlot hand = evt.getHand();

        if (hand != EquipmentSlot.HAND || !(evt.getRightClicked() instanceof Player pet)) return;

        ItemStack handContent = owner.getInventory().getItemInMainHand();

        if (isSilencer(handContent.getType(), handContent.getItemMeta())) {
            evt.setCancelled(true);
            if (plugin.TalkingEvents.silence(owner.getName(), pet.getName())) {
                ItemStack helmet = pet.getInventory().getHelmet();
                pet.getInventory().setHelmet(handContent.asOne());
                owner.getInventory().setItemInMainHand(handContent.subtract());

                if (helmet != null) pet.getWorld().dropItemNaturally(pet.getLocation(), helmet);
            }
        } else {
            ItemStack helmet = pet.getInventory().getHelmet();
            if (helmet != null && isKeyForSilencer(helmet, handContent)) {
                evt.setCancelled(true);
                if (plugin.TalkingEvents.tryLetSpeak(owner, pet.getName())) {
                    pet.getInventory().setHelmet(null); // take if off
                    pet.getWorld().dropItemNaturally(pet.getLocation(), helmet);
                }
            }
        }
    }
}
