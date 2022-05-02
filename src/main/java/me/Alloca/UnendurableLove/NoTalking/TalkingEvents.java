package me.Alloca.UnendurableLove.NoTalking;

import me.Alloca.UnendurableLove.Items;
import me.Alloca.UnendurableLove.UnendurableLove;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TalkingEvents implements Listener, CommandExecutor
{
    private UnendurableLove plugin;
    private Map<String, String> silentWithOwners = new HashMap<>();

    public TalkingEvents(UnendurableLove plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
        String player = event.getPlayer().getName();
        if (silentWithOwners.containsKey(player))
        {
            event.setCancelled(true);

            plugin.getServer().sendMessage(
                    event.getPlayer(),
                    Component.text(ChatColor.GRAY + "*" + player + " tries to say something*"));
        }
    }

    @EventHandler
    public void onPetTryingToRemoveGag(InventoryClickEvent event)
    {
        if(silentWithOwners.containsKey(event.getWhoClicked().getName()))
            if(event.getSlotType() == InventoryType.SlotType.ARMOR &&
                Items.AreItemTypesEqual(Items.getSilencerItemStack(), event.getCurrentItem()))
                    event.setCancelled(true);
    }
    
    public boolean silence(String owner, String player)
    {
        return silentWithOwners.putIfAbsent(player, owner) == null;
    }

    public boolean letSpeak(String owner, String player)
    {
        if (!silentWithOwners.getOrDefault(player, "").equals(owner))
            return false;
        silentWithOwners.remove(player);
        return true;
    }

    public boolean tryLetSpeak(Player sender, String player)
    {
        if (!isSilent(player))
        {
            sender.sendMessage(ChatColor.RED + "You forgot to put a gag on " + player + " first.");
            return false;
        }

        if (!letSpeak(sender.getName(), player))
        {
            sender.sendMessage(ChatColor.RED + "You are not the one to decide when " + player + " can talk.");
            return false;
        }

        return true;
    }

    public boolean isSilent(String player)
    {
        return silentWithOwners.containsKey(player);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (sender instanceof Player owner)
            if (args.length == 1) tryLetSpeak(owner, args[0]);

        return false;
    }
}
