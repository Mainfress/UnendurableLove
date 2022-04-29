package me.Alloca.UnendurableLove.Riding;

import com.bergerkiller.bukkit.common.map.MapSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.HashMap;

public class RidingEvents implements Listener
{
    private HashMap <String, String> ridingCouples;

    public RidingEvents()
    {
        ridingCouples = new HashMap<String, String>();
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

                ridingCouples.put(mount.getName(), owner.getName());
                mount.addPassenger(owner);
                //mount.hidePlayer(owner);
                owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);
            }
        }
    }

    @EventHandler
    public void onOwnerDismountEvent(EntityDismountEvent event)
    {
        if(event.getDismounted() instanceof Player && event.getEntity() instanceof  Player)
        {
            Player mount = (Player)event.getDismounted();
            Player owner = (Player)event.getEntity();

            if(ridingCouples.containsKey(mount.getName()))
            {
                ridingCouples.remove(mount.getName());
                //mount.showPlayer(owner);
                owner.getInventory().addItem(new ItemStack[]{new ItemStack(Material.SADDLE)});
            }
        }
    }

    /*public void onMountDeathEvent(PlayerDeathEvent event)
    {
        if(ridingCouples.containsKey(event.getPlayer()))
            event.getDrops().add(new ItemStack(Material.SADDLE));
    }*/
}
