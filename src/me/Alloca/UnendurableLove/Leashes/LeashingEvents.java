package me.Alloca.UnendurableLove.Leashes;

import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LeashingEvents implements Listener
{
    private UnendurableLove plugin;
    private List<CoupleIds> couplesIds;
    private HashMap<String, Location> hangedPets;

    private final String hangedMetadata = "Hanged";

    public LeashingEvents(UnendurableLove plug)
    {
        couplesIds = new ArrayList<CoupleIds>();
        hangedPets = new HashMap<String, Location>();
        plugin = plug;
    }

    private LivingEntity spawnDummyMobOnALeash(Player owner, Player targetPlayer)
    {
        Location location = targetPlayer.getLocation().add(0,1,0);
        //spawn entity far away from the players because it is visible for 0.1 seconds
        Slime slime = (Slime)targetPlayer.getWorld().spawnEntity(location, EntityType.SLIME);

        //slime.setInvisible(true);
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

        if(hand == EquipmentSlot.HAND && event.getRightClicked() instanceof Player)
        {
            pet = (Player)event.getRightClicked();

            if(owner.getInventory().getItemInMainHand().getType() == Material.LEAD)
            {
                CoupleIds coupleToCheck = couplesIds.stream()
                        .filter(x -> x.pet().equals(pet.getName()))
                        .findFirst().orElse(null);
                if(coupleToCheck != null)
                    return;

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
                couplesIds.add(newCouple);

                owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);

                LeashMechanic mechanic = new LeashMechanic(couplesIds, hangedPets,
                        new CoupleIds(owner.getName(), pet.getName(), mob.getUniqueId()));
                mechanic.runTaskTimer(this.plugin, 0L, 0L);
            }
            else
            {
                hangedPets.remove(pet.getName());
                unleashPet(pet);
            }
        }

    }

    public boolean freeCouple(CoupleIds couple)
    {
        hangedPets.remove(couple.pet());
        return unleashPet(Bukkit.getPlayer(couple.pet()));
    }

    private boolean unleashPet(Player pet)
    {
       CoupleIds couple = couplesIds.stream().
               filter(x -> x.pet().equals(pet.getName())).findFirst().orElse(null);
        if(couple == null)
            return false;

        LivingEntity slime = (LivingEntity)Bukkit.getEntity(couple.slime());
        slime.setLeashHolder(null);
        slime.remove();

        plugin.getBoard().removeTeam(couple.pet(), couple.slime().toString());
        boolean result = couplesIds.remove(couple);

        Player owner = Bukkit.getPlayer(couple.owner());
        owner.getInventory().addItem(new ItemStack[]{new ItemStack(Material.LEAD)});

        return result;

    }

    public List<CoupleIds> getCouplesIds()
    {
        return couplesIds;
    }

    public HashMap<String,Location> getHangedPets()
    {
        return hangedPets;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
        {
            Player pet = event.getPlayer();
            Entity mob = Bukkit.getEntity(couple.slime());
            mob.teleport(pet.getLocation().add(0.0D, 1.0D, 0.0D));
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event)
    {
        List<CoupleIds> couples = couplesIds.stream().
                filter(x -> x.owner().equals(event.getEntity().getPlayer().getName())).toList();

        for(CoupleIds couple : couples)
        {
            if (couple != null)
            {
                if (!hangedPets.containsKey(couple.pet()))
                    unleashPet(Bukkit.getPlayer(couple.pet()));
            }
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event)
    {
        List<CoupleIds> couples = couplesIds.stream().
                filter(x -> x.owner().equals(event.getPlayer().getName())).toList();

        for(CoupleIds couple : couples)
        {
            if (couple != null)
            {
                if (!hangedPets.containsKey(couple.pet()))
                    unleashPet(Bukkit.getPlayer(couple.pet()));
            }
        }
    }

    @EventHandler
    public void onPetJoinEvent(PlayerJoinEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
            ((LivingEntity)Bukkit.getEntity(couple.slime())).setLeashHolder(Bukkit.getPlayer(couple.owner()));
    }
    @EventHandler
    public void onPetQuitEvent(PlayerQuitEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
            ((LivingEntity)Bukkit.getEntity(couple.slime())).setLeashHolder(null);
    }

    @EventHandler
    public void onLeashHitchPlaceEvent(HangingPlaceEvent event)
    {
        if(event.getEntity() instanceof LeashHitch)
        {
            List<Entity> couldBeHanged = event.getEntity().getNearbyEntities(7.0D, 7.0D, 7.0D);
            for (Entity entity : couldBeHanged)
            {
                CoupleIds couple = couplesIds.stream()
                        .filter(x -> x.slime().equals(entity.getUniqueId()) && x.owner().equals(event.getPlayer().getName()))
                        .findFirst().orElse(null);
                if (couple != null)
                    hangedPets.put(couple.pet(), event.getBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onLeashHitchRemoveEvent(EntityRemoveEvent event)
    {
        if(event.getEntity() instanceof LeashHitch)
        {
            List<Entity> couldBeHanged = event.getEntity().getNearbyEntities(7.0D, 7.0D, 7.0D);
            for (Entity entity : couldBeHanged)
            {
                CoupleIds couple = couplesIds.stream()
                        .filter(x -> x.slime().equals(entity.getUniqueId()))
                        .findFirst().orElse(null);
                if (couple != null)
                {
                    hangedPets.remove(couple.pet());
                    unleashPet(Bukkit.getPlayer(couple.pet()));
                }
            }
        }
    }

    @EventHandler
    public void OnSomebodyTryingToLeashSlime(PlayerLeashEntityEvent event)
    {
        CoupleIds couple = couplesIds.stream()
                .filter(x -> x.slime().equals(event.getEntity().getUniqueId()))
                .findFirst().orElse(null);
        if(couple != null)
            if(couple.owner() != event.getPlayer().getName())
                event.setCancelled(true);
    }

    @EventHandler
    public void OnSomebodyTryingToUnleashSlime(PlayerUnleashEntityEvent event)
    {
        CoupleIds couple = couplesIds.stream()
                .filter(x -> x.slime().equals(event.getEntity().getUniqueId()))
                .findFirst().orElse(null);
        if(couple != null)
            if(couple.owner() != event.getPlayer().getName())
                event.setCancelled(true);
    }
}



class LeashMechanic extends BukkitRunnable
{
    private CoupleIds coupleToProcess;
    private List<CoupleIds> allCouples;
    private HashMap<String, Location> hangedPets;

    LeashMechanic(List<CoupleIds> couples, HashMap<String, Location> hanged, CoupleIds couple)
    {
        hangedPets = hanged;
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

        Location bindingLocation =
                hangedPets.containsKey(coupleToProcess.pet()) ?
                        hangedPets.get(coupleToProcess.pet()) : owner.getLocation();

        if (bindingLocation.distanceSquared(pet.getLocation()) > 10)
            pet.setVelocity(bindingLocation.toVector().subtract(
                            pet.getLocation().toVector()).multiply(0.05D));

        if(bindingLocation.distance(pet.getLocation()) > 20)
            pet.teleport(bindingLocation);
    }
}

