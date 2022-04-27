package me.Alloca.UnendurableLove.NoTalking;

import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PetSilencers implements Listener
{
    private final UnendurableLove plugin;

    private Set<String> diedPetsNeedingToRespawnWithGag = new HashSet<String>();

    private static final Material silencerMat = Material.LEATHER_HELMET;
    private static final Material silencerKeyMat = Material.STICK;

    public PetSilencers(UnendurableLove plugin)
    {
        this.plugin = plugin;
        registerAll();
    }

    private ItemStack getSilencerItemStack()
    {
        ItemStack result = new ItemStack(silencerMat, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(1984);
        meta.setDisplayName("Simple Ballgag");
        meta.setLore(List.of(ChatColor.GRAY + "No pet can talk while wearing this.",
                ChatColor.GRAY + "*Maybe* it's only for the naughty ones..."));
        result.setItemMeta(meta);
        result.addEnchantment(Enchantment.BINDING_CURSE, 1);

        return result;
    }

    private ItemStack getSilencerKeyItemStack()
    {
        ItemStack result = new ItemStack(silencerKeyMat, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(2984);
        meta.setDisplayName("Ballgag Key");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to let the bad pet talk again.",
                ChatColor.GRAY + "Only use if you *really* need to."));
        result.setItemMeta(meta);

        return result;
    }

    private void registerRecipe(ItemStack item, String[] shape, Material[] ingredients)
    {
        ItemStack result = item;

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(UnendurableLove.Instance,
                        "UnendurableLove" + item.getItemMeta().getCustomModelData()), result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i++)
        {
            recipe.setIngredient((char) ('A' + i), ingredients[i]);
        }

        Bukkit.addRecipe(recipe);
    }

    public void registerAll()
    {
        // string-leather-string
        registerRecipe(getSilencerItemStack(), new String[] { "ABA" },
                new Material[] { Material.STRING, Material.LEATHER });
        // iron-iron / empty-stick
        registerRecipe(getSilencerKeyItemStack(), new String[]{ "AA", " B" },
                new Material[]{ Material.IRON_INGOT, Material.STICK } );
    }

    public boolean isSilencer(Material material, ItemMeta meta)
    {
        return material == silencerMat &&
                meta.getCustomModelData() == getSilencerItemStack().getItemMeta().getCustomModelData();
    }

    public boolean isKeyForSilencer(Material material, ItemMeta meta)
    {
        int a = meta.getCustomModelData();
        int b = getSilencerKeyItemStack().getItemMeta().getCustomModelData();
        return material == silencerKeyMat &&
                meta.getCustomModelData() == getSilencerKeyItemStack().getItemMeta().getCustomModelData();
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEntityEvent evt)
    {
        Player owner = evt.getPlayer();
        EquipmentSlot hand = evt.getHand();

        if (hand != EquipmentSlot.HAND || !(evt.getRightClicked() instanceof Player pet)) return;

        ItemStack handContent = owner.getInventory().getItemInMainHand();

        if (isSilencer(handContent.getType(), handContent.getItemMeta()))
        {
            if(plugin.TalkingEvents.isSilent(pet.getName()))
                return;

            if (plugin.TalkingEvents.silence(owner.getName(), pet.getName()))
            {
                evt.setCancelled(true);
                ItemStack helmet = pet.getInventory().getHelmet();
                pet.getInventory().setHelmet(handContent.asOne());
                owner.getInventory().setItemInMainHand(handContent.subtract());

                if (helmet != null)
                    pet.getWorld().dropItemNaturally(pet.getLocation(), helmet);
            }
        }
        else
        {
            ItemStack helmet = pet.getInventory().getHelmet();
            if (helmet != null && isKeyForSilencer(handContent.getType(), handContent.getItemMeta()))
            {
                if (plugin.TalkingEvents.tryLetSpeak(owner, pet.getName()))
                {
                    pet.getWorld().dropItemNaturally(pet.getLocation(), helmet);
                    pet.getInventory().setHelmet(null); // take if off
                }
            }
        }
    }

    @EventHandler
    public void onSilencedPetDeathEvent(PlayerDeathEvent event)
    {
        String pet = event.getPlayer().getName();
        if(plugin.TalkingEvents.isSilent(pet))
        {
            for(ItemStack item : event.getDrops())
            {
                if (item.getItemMeta().getCustomModelData() == 1984)
                {
                    event.getDrops().remove(item);
                    break;
                }
            }

            diedPetsNeedingToRespawnWithGag.add(pet);
        }
    }

    @EventHandler
    public void onSilencedPetRespawnEvent(PlayerRespawnEvent event)
    {
        String pet = event.getPlayer().getName();
        if(plugin.TalkingEvents.isSilent(pet) &&
                diedPetsNeedingToRespawnWithGag.contains(pet))
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                Player petPlayer = event.getPlayer();
                petPlayer.getInventory().setHelmet(getSilencerItemStack());
                diedPetsNeedingToRespawnWithGag.remove(pet);
            },1);
        }
    }
}
