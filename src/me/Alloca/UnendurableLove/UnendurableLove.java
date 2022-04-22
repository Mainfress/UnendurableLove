package me.Alloca.UnendurableLove;

import me.Alloca.UnendurableLove.Label.LabelingEvents;
import me.Alloca.UnendurableLove.Leashes.CollisionBoard;
import me.Alloca.UnendurableLove.Leashes.CoupleIds;
import me.Alloca.UnendurableLove.Leashes.LeashingEvents;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;

public class UnendurableLove extends JavaPlugin
{
    public static UnendurableLove Instance;
    public LeashingEvents LeashEvents;
    public LabelingEvents LabelEvents;
    private CollisionBoard board;
    @Override
    public void onEnable()
    {
        Instance = this;
        board = new CollisionBoard();
        LeashEvents = new LeashingEvents(this);
        LabelEvents = new LabelingEvents();

        Bukkit.getPluginManager().registerEvents(LeashEvents, this);
        Bukkit.getPluginManager().registerEvents(LabelEvents, this);
        Bukkit.getLogger().info(ChatColor.GREEN + "sosi " + this.getName());

        getCommand("unleash").setExecutor(new CommandUnleash());
        getCommand("chooseTag").setExecutor(new CommandChooseTag());
    }

    public CollisionBoard getBoard()
    {
        return board;
    }
}

class CommandUnleash implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {

        if (sender instanceof Player owner)
        {
            String petName = args[0];
            Player pet = Bukkit.getPlayer(petName);
            if (pet == null)
            {
                sender.sendMessage(ChatColor.RED + "Pet " + petName + " doesn't exist");
                return true;
            }

            CoupleIds couple = UnendurableLove.Instance.LeashEvents.getCouplesIds().stream()
                    .filter(x -> x.owner().equals(owner.getName()) && x.pet().equals(pet.getName()))
                    .findFirst().orElse(null);

            boolean result = false;
            if(couple != null)
                result = UnendurableLove.Instance.LeashEvents.freeCouple(couple);

            if(result)
                sender.sendMessage(ChatColor.GREEN + "Pair " + owner.getName() + "-" + petName + " removed");
            else
                sender.sendMessage(ChatColor.RED + "You haven't put " + petName + " into submission yet");
        }

        return true;
    }
}

class CommandChooseTag implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (sender instanceof Player owner)
        {
            if (args.length == 1)
            {
                UnendurableLove.Instance.LabelEvents.addOwnerTag(owner, args[0] + " ");
                sender.sendMessage(ChatColor.GREEN + MessageFormat.format("{0} is a cute moniker!", args[0]));
            }
            else
            {
                return false;
            }
        }

        return true;
    }
}