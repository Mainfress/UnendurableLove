package me.Alloca.UnendurableLove;

import me.Alloca.UnendurableLove.Leashes.CollisionBoard;
import me.Alloca.UnendurableLove.Leashes.LeashingEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class UnendurableLove extends JavaPlugin
{

    private CollisionBoard board;

    @Override
    public void onEnable()
    {
        board = new CollisionBoard();
        Bukkit.getPluginManager().registerEvents(new LeashingEvents(this), this);
        Bukkit.getLogger().info(ChatColor.GREEN + "sosi " + this.getName());
    }

    public CollisionBoard getBoard()
    {
        return board;
    }
}