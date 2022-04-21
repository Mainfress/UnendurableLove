package me.Alloca.UnendurableLove.Leashes;

import me.Alloca.UnendurableLove.UnendurableLove;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

record Couple(Player owner, Player pet, LivingEntity slime){}
record CoupleIds(String owner, String pet, UUID slime) {}

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
        Slime slime = (Slime)targetPlayer.getWorld().spawnEntity(location.add(50,0,0), EntityType.SLIME);

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
                CoupleIds coupleToCheck = searchForPlayerInCouples(pet);
                if(coupleToCheck != null && coupleToCheck.pet() == pet.getName())
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

    @EventHandler
    public void onRemoveLeashHitchEvent(PlayerInteractEntityEvent event)
    {
        Player owner = event.getPlayer();
        EquipmentSlot hand = event.getHand();

        if(hand == EquipmentSlot.HAND && event.getRightClicked() instanceof LeashHitch)
        {
            List<Entity> couldBeHanged = event.getRightClicked().getNearbyEntities(7.0D, 7.0D, 7.0D);
            for(Entity entity : couldBeHanged)
            {
                CoupleIds couple = searchForMobInCouples(entity);
                if (couple != null && hangedPets.containsKey(couple.pet()) && couple.owner().equals(owner.getName()))
                {
                    hangedPets.remove(couple.pet());
                    unleashPet(Bukkit.getPlayer(couple.pet()));
                }
            }
        }
    }

    private void unleashPet(Player pet)
    {
        CoupleIds couple = searchForPlayerInCouples(pet);
        if(couple == null || !couple.pet().equals(pet.getName()))
            return;

        LivingEntity slime = (LivingEntity)Bukkit.getEntity(couple.slime());
        slime.setLeashHolder(null);
        slime.remove();

        plugin.getBoard().removeTeam(couple.pet(), couple.slime().toString());
        couplesIds.remove(couple);

        Player owner = Bukkit.getPlayer(couple.owner());
        owner.getInventory().addItem(new ItemStack[]{new ItemStack(Material.LEAD)});

    }

    public List<CoupleIds> getCouplesIds()
    {
        return couplesIds;
    }

    private CoupleIds searchForPlayerInCouples(Player player)
    {
        for(CoupleIds couple : couplesIds)
        {
            if(couple.owner().equals(player.getName()) || couple.pet().equals(player.getName()))
                return couple;
        }

        return null;
    }

    private CoupleIds searchForMobInCouples(Entity entity)
    {
        for(CoupleIds couple : couplesIds)
        {
            if(couple.slime().equals(entity.getUniqueId()))
                return couple;
        }

        return null;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event)
    {
        CoupleIds couple = searchForPlayerInCouples(event.getPlayer());
        if (couple != null && couple.pet().equals(event.getPlayer().getName()))
        {
            Player pet = event.getPlayer();
            Entity mob = Bukkit.getEntity(couple.slime());
            mob.teleport(pet.getLocation().add(0.0D, 1.0D, 0.0D));
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event)
    {
        CoupleIds couple = searchForPlayerInCouples(event.getEntity().getPlayer());
        if (couple != null && couple.owner().equals(event.getEntity().getPlayer().getName()))
        {
            unleashPet(Bukkit.getPlayer(couple.pet()));
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event)
    {
        CoupleIds couple = searchForPlayerInCouples(event.getPlayer());
        if (couple != null && couple.owner().equals(event.getPlayer().getName()))
        {
            unleashPet(Bukkit.getPlayer(couple.pet()));
        }
    }

    @EventHandler
    public void onHangingPlaceEvent(HangingPlaceEvent event)
    {
        List<Entity> couldBeHanged = event.getEntity().getNearbyEntities(7.0D, 7.0D, 7.0D);
        for(Entity entity : couldBeHanged)
        {
            CoupleIds couple = searchForMobInCouples(entity);
            if (couple != null && event.getPlayer().getName().equals(couple.owner()))
                hangedPets.put(couple.pet(), event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onHangingBreakEvent(HangingBreakEvent event)
    {
        List<Entity> couldBeHanged = event.getEntity().getNearbyEntities(7.0D, 7.0D, 7.0D);
        for(Entity entity : couldBeHanged)
        {
            CoupleIds couple = searchForMobInCouples(entity);
            if (couple != null && hangedPets.containsKey(couple.pet()))
            {
                hangedPets.remove(couple.pet());
                unleashPet(Bukkit.getPlayer(couple.pet()));
            }
        }
    }

    @EventHandler
    public void onHangingBreakByEntityEvent(HangingBreakEvent event)
    {
        onHangingBreakEvent(event);
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

        for(CoupleIds cp : allCouples)
            Bukkit.getLogger().info(cp.owner() + cp.pet());

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

