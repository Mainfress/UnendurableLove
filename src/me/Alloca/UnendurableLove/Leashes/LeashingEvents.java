package me.Alloca.UnendurableLove.Leashes;

import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.A;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

record Couple(Player owner, Player pet, LivingEntity slime){}
record CoupleIds(String owner, String pet, UUID slime) {}

public class LeashingEvents implements Listener
{
    private UnendurableLove plugin;
    private List<CoupleIds> couplesIds;

    public LeashingEvents(UnendurableLove plug)
    {
        couplesIds = new ArrayList<CoupleIds>();
        plugin = plug;
    }

    private LivingEntity spawnDummyMobOnALeash(Player owner, Player targetPlayer)
    {
        Location location = targetPlayer.getLocation().add(0,1,0);
        //spawn entity far away from the players because it is visible for 0.1 seconds
        Slime slime = (Slime)targetPlayer.getWorld().spawnEntity(location.add(50,0,0), EntityType.SLIME);

        slime.setInvisible(true);
        int tenHoursInTicks = 10 * 60 * 60 * 20;
        PotionEffect stealthInvisibility = new PotionEffect(PotionEffectType.INVISIBILITY,
                tenHoursInTicks, 1, false, false);
        stealthInvisibility.apply(slime);

        slime.teleport(location);
        slime.setSize(0);
        slime.setAI(false);
        slime.setGravity(false);
        slime.setLeashHolder(owner);
        slime.setInvulnerable(true);

        return slime;
    }

    @EventHandler
    public void onLeashEvent(PlayerInteractEntityEvent event)
    {
        Player owner, pet;
        owner = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if(hand == EquipmentSlot.HAND && event.getRightClicked() instanceof Player &&
                owner.getInventory().getItemInMainHand().getType() == Material.LEAD)
        {
            pet = (Player)event.getRightClicked();

            LivingEntity mob = spawnDummyMobOnALeash(owner, pet);

            try
            {
                plugin.getBoard().addTeam(pet, mob);
            }
            catch(RuntimeException e)
            {
                Bukkit.getLogger().info(e.getMessage());
                return;
            }

            pet.setScoreboard(plugin.getBoard().getBoard());

            CoupleIds newCouple = new CoupleIds(owner.getName(),pet.getName(),mob.getUniqueId());
            if(couplesIds.contains(newCouple))
                return;
            couplesIds.add(newCouple);

            owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);

            LeashMechanic mechanic = new LeashMechanic(couplesIds,
                    new CoupleIds(owner.getName(), pet.getName(), mob.getUniqueId()));
            mechanic.runTaskTimer(this.plugin, 0L, 0L);
        }
    }

    public List<CoupleIds> getCouplesIds()
    {
        return couplesIds;
    }

    private CoupleIds SearchForPlayerInCouples(Player player)
    {
        for(CoupleIds couple : couplesIds)
        {
            if(couple.owner() == player.getName() || couple.pet() == player.getName())
                return couple;
        }

        return null;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event)
    {
        CoupleIds couple = SearchForPlayerInCouples(event.getPlayer());
        if (couple != null && couple.pet() == event.getPlayer().getName())
        {
            Player pet = event.getPlayer();
            Entity mob = Bukkit.getEntity(couple.slime());
            mob.teleport(pet.getLocation().add(0.0D, 1.0D, 0.0D));
        }
    }
}



class LeashMechanic extends BukkitRunnable
{
    private CoupleIds coupleToProcess;
    private List<CoupleIds> allCouples;

    LeashMechanic(List<CoupleIds> couples, CoupleIds couple)
    {
        coupleToProcess = couple;
        allCouples = couples;
    }

    @Override
    public void run()
    {
        if (!allCouples.contains(coupleToProcess))
            this.cancel();

        Player owner = Bukkit.getPlayer(coupleToProcess.owner());
        Player pet = Bukkit.getPlayer(coupleToProcess.pet());
        Entity mob = Bukkit.getEntity(coupleToProcess.slime());

        if (owner.getLocation().distanceSquared(pet.getLocation()) > 10)
            pet.setVelocity(owner.getLocation().toVector().subtract(
                            pet.getLocation().toVector()).multiply(0.05D));

        if(owner.getLocation().distanceSquared(pet.getLocation()) > 30)
            pet.teleport(owner);
    }
}

