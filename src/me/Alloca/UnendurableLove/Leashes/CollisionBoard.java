package me.Alloca.UnendurableLove.Leashes;


import org.apache.commons.lang.ObjectUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class CollisionBoard {
    private Scoreboard board;
    private List<CollisionTeam> teams;

    public CollisionBoard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        board = scoreboard;
        teams = new ArrayList<CollisionTeam>();

    }

    public void addTeam(String entity1, String entity2)
    {
        if(findTeam(entity1,entity2) != null)
            throw new RuntimeException(
                    MessageFormat.format("{0} and {1} are already a collision team", entity1, entity2));
        teams.add(new CollisionTeam(this, entity1, entity2));
    }

    public void addTeam(Player player, Entity entity)
    {
        addTeam(player.getName(), entity.getUniqueId().toString());
    }

    private CollisionTeam findTeam(String entity1, String entity2)
    {
        for(CollisionTeam itTeam : teams)
        {
            if(itTeam.getUnderlyingTeam().getName() == entity1 + entity2)
                return itTeam;
        }

        return null;
    }

    public void removeTeam(String entity1, String entity2)
    {
        CollisionTeam team = findTeam(entity1,entity2);
        teams.remove(team);
        team.unregister();
    }

    public Scoreboard getBoard()
    {
        return this.board;
    }
}