package me.Alloca.UnendurableLove.PlaceholderAPI;

import me.Alloca.UnendurableLove.UnendurableLove;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LoveBeyondNormalMeans extends PlaceholderExpansion {
    private final UnendurableLove plugin;

    public LoveBeyondNormalMeans(UnendurableLove plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "UnendurableLove";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Alloca";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("nametag")) {
            return plugin.LabelEvents.getTag(player);
        }

        return null;
    }
}
