package me.Alloca.UnendurableLove;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.net.http.WebSocket;

public class ItemsEvents implements Listener
{
    @EventHandler
    public void onPlayerWasWhippedToDeath(PlayerDeathEvent event)
    {
        Player killer = event.getPlayer().getKiller();

        if(killer != null)
            if(Items.AreItemTypesEqual(killer.getInventory().getItemInMainHand(), Items.getWhipItemStack()))
                event.setDeathMessage(event.getPlayer().getName() + " was whipped to death by " + killer.getName());
    }

    @EventHandler
    public void onPlayerLashWithWhip(EntityDamageByEntityEvent event)
    {
        if(event.getDamager() instanceof Player)
        {
            Player damager = (Player)event.getDamager();
            if(Items.AreItemTypesEqual(damager.getInventory().getItemInMainHand(), Items.getWhipItemStack()))
                event.setDamage(3.0);
        }
    }
}
