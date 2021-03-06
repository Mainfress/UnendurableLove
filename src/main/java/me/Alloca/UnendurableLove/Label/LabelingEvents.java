package me.Alloca.UnendurableLove.Label;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

record LabelCouple(String owner, String pet, Component label ) {}

public class LabelingEvents implements Listener
{
    private Scoreboard tagsScoreboard;
    private List<LabelCouple> couples;
    private HashMap<String, String> ownersAndTagsTheyChose;


    public LabelingEvents()
    {
        tagsScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ownersAndTagsTheyChose = new HashMap<String, String>();
        couples = new ArrayList<LabelCouple>();
    }

    @EventHandler
    public void onLabelEvent(PlayerInteractEntityEvent event)
    {
        Player owner, pet;
        owner = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if(hand == EquipmentSlot.HAND && event.getRightClicked() instanceof Player)
        {
            pet = (Player) event.getRightClicked();

            LabelCouple coupleToCheck = couples.stream()
                    .filter(x -> x.pet().equals(pet.getName()))
                    .findFirst().orElse(null);

            if (owner.getInventory().getItemInMainHand().getType() == Material.NAME_TAG)
            {
                if (coupleToCheck != null)
                    return;

                Component tag;
                ItemMeta item = owner.getInventory().getItemInMainHand().getItemMeta();

                if (ownersAndTagsTheyChose.containsKey(owner.getName()))
                    tag = Component.text(ownersAndTagsTheyChose.get(owner.getName()));
                else if (item.hasDisplayName()) tag = item.displayName();
                else tag = Component.text(MessageFormat.format("{0}`s ",owner.getName()));

                owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);

                addTagToPlayer(pet, tag);
                couples.add(new LabelCouple(owner.getName(), pet.getName(), tag));
            }
            else if(owner.getInventory().getItemInMainHand().getType() == Material.SHEARS)
            {
                if (coupleToCheck == null || !owner.getName().equals(coupleToCheck.owner()))
                    return;

                owner.getInventory().addItem(new ItemStack[]{new ItemStack(Material.NAME_TAG)});

                removeTagFromPlayer(pet);
                couples.remove(coupleToCheck);
            }
        }
    }

    private boolean addTagToPlayer(Player player, Component tag)
    {
        Team teamToCheck = tagsScoreboard.getTeams().stream().filter(x -> x.getName().equals(player.getName()))
                .findFirst().orElse(null);
        if(teamToCheck != null)
            return false;

        Team tagTeam = tagsScoreboard.registerNewTeam(player.getName());
        tagTeam.prefix(tag.color(TextColor.color(255, 0, 0)));
        tagTeam.addEntry(player.getName());

        return true;
    }

    private boolean removeTagFromPlayer(Player player)
    {
        Team teamToCheck = tagsScoreboard.getTeams().stream().filter(x -> x.getName().equals(player.getName()))
                .findFirst().orElse(null);
        if(teamToCheck == null)
            return false;

        teamToCheck.removeEntry(player.getName());
        teamToCheck.unregister();

        return true;
    }

    public void addOwnerTag(Player owner, String tag)
    {
        ownersAndTagsTheyChose.put(owner.getName(), tag);
    }

    public void removeOwnerTag(Player owner)
    {
        ownersAndTagsTheyChose.remove(owner.getName());
    }

}
