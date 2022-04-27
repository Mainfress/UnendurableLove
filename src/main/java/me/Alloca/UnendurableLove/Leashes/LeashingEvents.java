package me.Alloca.UnendurableLove.Leashes;

import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.util.*;

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

    private LivingEntity spawnDummyMobOnALeash(String ownerId, String targetPlayerId)
    {
        Player owner = Bukkit.getPlayer(ownerId);
        Player targetPlayer = Bukkit.getPlayer(targetPlayerId);

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

    private void safelyDespawnDummyMobOnALeash(String pet, UUID slimeId)
    {
        LivingEntity slime = (LivingEntity)Bukkit.getEntity(slimeId);
        if(slime != null)
        {
            slime.setLeashHolder(null);
            slime.remove();
        }
        plugin.getBoard().removeTeam(pet, slimeId.toString());
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

            CoupleIds coupleToCheck = couplesIds.stream()
                    .filter(x -> x.pet().equals(pet.getName()))
                    .findFirst().orElse(null);

            if(owner.getInventory().getItemInMainHand().getType() == Material.LEAD)
            {
                if(coupleToCheck != null)
                    return;

                LivingEntity mob = spawnDummyMobOnALeash(owner.getName(), pet.getName());

                try
                {
                    plugin.getBoard().addTeam(pet, mob);
                }
                catch(RuntimeException e)
                {
                    Bukkit.getLogger().info(e.getMessage());
                    return;
                }

                /*Objective obj = plugin.getBoard().getBoard().registerNewObjective("test", "dummy");
                obj.setDisplayName("&b&lTest");
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);*/
                pet.setScoreboard(plugin.getBoard().getBoard());

                CoupleIds newCouple = new CoupleIds(owner.getName(),pet.getName(),mob.getUniqueId());
                couplesIds.add(newCouple);

                owner.getInventory().getItemInMainHand().setAmount(owner.getInventory().getItemInMainHand().getAmount() - 1);

                LeashMechanic mechanic = new LeashMechanic(couplesIds, hangedPets, owner.getName(), pet.getName());
                mechanic.runTaskTimer(this.plugin, 0L, 0L);
            }
            else
            {
                if(coupleToCheck == null)
                    return;

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

        safelyDespawnDummyMobOnALeash(couple.pet(), couple.slime());

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

    private boolean teleportSlimeToPet(String petId, UUID slimeId)
    {
        Player pet = Bukkit.getPlayer(petId);
        Entity slime = Bukkit.getEntity(slimeId);

        if(pet == null || slime == null)
            return false;

        slime.teleport(pet.getLocation().add(0.0D, 1.0D, 0.0D));

        return true;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
        {
            teleportSlimeToPet(couple.pet(), couple.slime());
        }
    }

    @EventHandler
    public void onPlayersMoveInVehicleEvent(VehicleMoveEvent event)
    {
        List<Entity> passengers = event.getVehicle().getPassengers();
        List<Player> playersInVehicle = passengers.stream().
                filter(x -> x instanceof Player).map(x -> (Player)x).toList();
        List<CoupleIds> couplesOfPetsInVehicle = playersInVehicle.stream()
                .map(x -> couplesIds.stream().filter(y -> y.pet().equals(x.getName())).findFirst().orElse(null))
                .filter(x -> x != null).toList();

        for(CoupleIds couple : couplesOfPetsInVehicle)
        {
            teleportSlimeToPet(couple.pet(), couple.slime());
        }
    }

    @EventHandler
    public void onOwnerDeathEvent(PlayerDeathEvent event)
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
    public void onOwnerQuitEvent(PlayerQuitEvent event)
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
        {
            /*The need to wait 1 tick before starting all releashing routines is caused by a ridiculous bug
            i have spent around 10 hours to fix.After a player joins a server or simply respawns, the game doesn't
             manage to process it correctly as soon as it fires corresponding events.So you need to set up a delay
             with duration of 1 tick and ONLY after that do what you wanted to do with the player.The other way is
             undefined behaviour*/

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                UUID newSlimeId = spawnDummyMobOnALeash(couple.owner(), couple.pet()).getUniqueId();
                couplesIds.remove(couple);
                couplesIds.add(new CoupleIds(couple.owner(), couple.pet(), newSlimeId));
                plugin.getBoard().addTeam(couple.pet(), newSlimeId.toString());
                Bukkit.getPlayer(couple.pet()).setScoreboard(plugin.getBoard().getBoard());
            }, 1);
            //((LivingEntity)Bukkit.getEntity(couple.slime())).setLeashHolder(Bukkit.getPlayer(couple.owner()));
        }
    }

    @EventHandler
    public void onPetRespawnEvent(PlayerRespawnEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
        {
            /*The need to wait 1 tick before starting all releashing routines is caused by a ridiculous bug
            i have spent around 10 hours to fix.After a player joins a server or simply respawns, the game doesn't
             manage to process it correctly as soon as it fires corresponding events.So you need to set up a delay
             with duration of 1 tick and ONLY after that do what you wanted to do with the player.The other way is
             undefined behaviour*/

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
            {
                UUID newSlimeId = spawnDummyMobOnALeash(couple.owner(), couple.pet()).getUniqueId();
                couplesIds.remove(couple);
                couplesIds.add(new CoupleIds(couple.owner(), couple.pet(), newSlimeId));
                plugin.getBoard().addTeam(couple.pet(), newSlimeId.toString());
                Bukkit.getPlayer(couple.pet()).setScoreboard(plugin.getBoard().getBoard());
            }, 1);
        }
    }
    @EventHandler
    public void onPetQuitEvent(PlayerQuitEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
        {
            safelyDespawnDummyMobOnALeash(couple.pet(), couple.slime());
            //((LivingEntity)Bukkit.getEntity(couple.slime())).setLeashHolder(null);
        }
    }

    @EventHandler
    public void onPetDeathEvent(PlayerDeathEvent event)
    {
        CoupleIds couple = couplesIds.stream().
                filter(x -> x.pet().equals(event.getPlayer().getName())).findFirst().orElse(null);
        if(couple != null)
        {
            safelyDespawnDummyMobOnALeash(couple.pet(), couple.slime());
        }
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
    public void onLeashHitchBreakByPet(HangingBreakByEntityEvent event)
    {
        if(event.getEntity() instanceof LeashHitch)
        {
            CoupleIds couple = couplesIds.stream().
                    filter(x -> x.pet().equals(event.getRemover().getName())).findFirst().orElse(null);
            if(couple.pet() != null && hangedPets.containsKey(couple.pet()))
                event.setCancelled(true);
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
            //if(couple.owner() != event.getPlayer().getName())
                event.setCancelled(true);
    }
}



class LeashMechanic extends BukkitRunnable
{

    private String ownerId;
    private String petId;
    private List<CoupleIds> allCouples;
    private HashMap<String, Location> hangedPets;

    LeashMechanic(List<CoupleIds> couples, HashMap<String, Location> hanged, String idOfOwner, String idOfPet)
    {
        hangedPets = hanged;
        ownerId = idOfOwner;
        petId = idOfPet;
        allCouples = couples;
    }

    @Override
    public void run()
    {

        CoupleIds coupleToCheckExistence = allCouples.stream()
                .filter(x -> x.pet().equals(petId) && x.owner().equals(ownerId)).findFirst().orElse(null);
        if (coupleToCheckExistence == null)
            this.cancel();

        Player owner = Bukkit.getPlayer(ownerId);
        Player pet = Bukkit.getPlayer(petId);

        Location bindingLocation =
                hangedPets.containsKey(petId) ?
                        hangedPets.get(petId) : owner.getLocation();

        if (bindingLocation.distanceSquared(pet.getLocation()) > 10)
            pet.setVelocity(bindingLocation.toVector().subtract(
                            pet.getLocation().toVector()).multiply(0.05D));

        if(bindingLocation.distance(pet.getLocation()) > 20)
            pet.teleport(bindingLocation);
    }
}

