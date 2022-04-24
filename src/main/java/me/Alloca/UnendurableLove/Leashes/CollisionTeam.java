package me.Alloca.UnendurableLove.Leashes;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

public class CollisionTeam
{
    private CollisionBoard motherBoard;
    private Team underlyingTeam;

    public CollisionTeam(CollisionBoard board,String entity1, String entity2)
    {
        motherBoard = board;
        underlyingTeam = motherBoard.getBoard().registerNewTeam(entity1 + entity2);
        underlyingTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        underlyingTeam.addEntry(entity1);
        underlyingTeam.addEntry(entity2);
    }

    public CollisionTeam(CollisionBoard motherBoard, Player player, Entity entity)
    {
        this(motherBoard,player.getName(), entity.getUniqueId().toString());
    }

    @Override
    public boolean equals(Object o)
    {
        if(o instanceof CollisionTeam anotherTeam)
            return underlyingTeam.getName() == anotherTeam.underlyingTeam.getName();
        else
            return false;
    }

    public Team getUnderlyingTeam() {
        return underlyingTeam;
    }

    public void unregister()
    {
        underlyingTeam.unregister();
    }
}
