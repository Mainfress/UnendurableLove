package me.Alloca.UnendurableLove.Riding;

import me.Alloca.UnendurableLove.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

record OwnerAndPadPair(String owner, UUID pad) {}

public class RidingEvents implements Listener
{
    private HashMap <String, OwnerAndPadPair> ridingCouples;

    public RidingEvents()
    {
        ridingCouples = new HashMap<String, OwnerAndPadPair>();
    }

    private Entity spawnPadBetweenOwnerAndMount(Player mount)
    {
        Slime pad = (Slime)mount.getWorld().spawnEntity(mount.getLocation(), EntityType.SLIME);

        int tenHoursInTicks = 10 * 60 * 60 * 20;
        PotionEffect stealthInvisibility = new PotionEffect(PotionEffectType.INVISIBILITY,
                tenHoursInTicks, 1, false, false);
        stealthInvisibility.apply(pad);

        pad.setSize(0);
        pad.setAI(false);
        pad.setGravity(false);
        pad.setInvulnerable(true);

        return pad;
    }

    @EventHandler
    public void OnPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        Player owner, mount;
        owner = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if(hand == EquipmentSlot.HAND && event.getRightClicked() instanceof Player)
        {
            mount = (Player) event.getRightClicked();

            if (owner.getInventory().getItemInMainHand().getType() == Material.SADDLE)
            {
                if (ridingCouples.containsKey(mount.getName()))
                    return;

                Entity pad = (Entity)spawnPadBetweenOwnerAndMount(owner);
                mount.addPassenger(pad);
                pad.addPassenger(owner);

                ridingCouples.put(mount.getName(), new OwnerAndPadPair(owner.getName(), pad.getUniqueId()));
                //mount.hidePlayer(owner);
                owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);
            }
        }
    }

    @EventHandler
    public void onPlayerHitItsMountWithWhip(PlayerInteractEvent event)
    {
        if(event.getAction().isLeftClick())
        {
            ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
            Map.Entry<String, OwnerAndPadPair> pair = ridingCouples.entrySet().stream()
                    .filter(x -> x.getValue().owner().equals(event.getPlayer().getName())).findFirst().orElse(null);

            if(Items.AreItemTypesEqual(itemInHand, Items.getWhipItemStack()) && pair != null)
            {
                Player owner = event.getPlayer();
                Player mount = Bukkit.getPlayer(pair.getKey());

                owner.attack(mount);

                PotionEffect whipEffectJump = new PotionEffect(PotionEffectType.JUMP, 10 * 20, 1, true, true);
                PotionEffect whipEffectSpeed = new PotionEffect(PotionEffectType.SPEED, 10 * 20, 2, true, true);
                mount.addPotionEffect(whipEffectJump);
                mount.addPotionEffect(whipEffectSpeed);
            }

        }
    }

    @EventHandler
    public void onOwnerDismountEvent(EntityDismountEvent event)
    {
        if(event.getEntity() instanceof  Player)
        {
            Entity pad = event.getDismounted();
            Player owner = (Player)event.getEntity();

            Map.Entry<String,OwnerAndPadPair> pairToCheck = ridingCouples.entrySet().stream().
                    filter(x -> x.getValue().pad().equals(pad.getUniqueId())).findFirst().orElse(null);

            if(pairToCheck != null)
            {
                Player mount = Bukkit.getPlayer(pairToCheck.getKey());

                mount.removePassenger(pad);
                pad.remove();
                ridingCouples.remove(pairToCheck.getKey());

                owner.getInventory().addItem(new ItemStack[]{new ItemStack(Material.SADDLE)});
            }
        }
    }

    @EventHandler
    public void onPadDismountEvent(EntityDismountEvent event)
    {
        if(event.getDismounted() instanceof  Player)
        {
            Player mount = (Player)event.getDismounted();
            Entity pad = event.getEntity();

            if(ridingCouples.containsKey(mount.getName()))
            {
                Player owner = Bukkit.getPlayer(ridingCouples.get(mount.getName()).owner());
                pad.removePassenger(owner);
                pad.remove();
                ridingCouples.remove(mount.getName());
            }
        }
    }

    @EventHandler
    public void onSomebodyTriesToInteractWithPad(PlayerInteractEntityEvent event)
    {
        Map.Entry<String,OwnerAndPadPair> pairToCheck = ridingCouples.entrySet().stream()
                .filter(x -> x.getValue().pad().equals(event.getRightClicked().getUniqueId()))
                .findFirst().orElse(null);
        if(pairToCheck != null)
            event.setCancelled(true);
    }

    /*public void onMountDeathEvent(PlayerDeathEvent event)
    {
        if(ridingCouples.containsKey(event.getPlayer()))
            event.getDrops().add(new ItemStack(Material.SADDLE));
    }*/
}
