package me.Alloca.UnendurableLove.NoTalking;

import me.Alloca.UnendurableLove.Items;
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
import org.bukkit.event.block.BlockPlaceEvent;
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

    public PetSilencers(UnendurableLove plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEntityEvent evt)
    {
        Player owner = evt.getPlayer();
        EquipmentSlot hand = evt.getHand();

        if (hand != EquipmentSlot.HAND || !(evt.getRightClicked() instanceof Player pet)) return;

        ItemStack handContent = owner.getInventory().getItemInMainHand();

        if (Items.AreItemTypesEqual(handContent, Items.getSilencerItemStack()))
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
            if (helmet != null && Items.AreItemTypesEqual(handContent, Items.getSilencerKeyItemStack()))
            {
                if (plugin.TalkingEvents.tryLetSpeak(owner, pet.getName()))
                {
                    owner.getInventory().addItem(Items.getSilencerItemStack());
                    pet.getInventory().setHelmet(null); // take if off
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event)
    {
        ItemStack itemInHand = event.getItemInHand();

        if (!Items.AreItemTypesEqual(itemInHand, Items.getSilencerItemStack()))
            return;

        event.setCancelled(true);
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
                petPlayer.getInventory().setHelmet(Items.getSilencerItemStack());
                diedPetsNeedingToRespawnWithGag.remove(pet);
            },1);
        }
    }
}
