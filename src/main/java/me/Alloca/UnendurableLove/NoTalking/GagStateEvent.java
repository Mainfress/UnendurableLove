package me.Alloca.UnendurableLove.NoTalking;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GagStateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private boolean isGagged;
    private String playerName;

    public GagStateEvent(String player, boolean isGagged) {
        this.isGagged = isGagged;
        this.playerName = player;
    }

    public boolean isGagged() {
        return isGagged;
    }

    public void setGagged(boolean isGagged) {
        this.isGagged = isGagged;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
