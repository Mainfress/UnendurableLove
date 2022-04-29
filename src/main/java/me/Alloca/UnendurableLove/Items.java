package me.Alloca.UnendurableLove;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Items
{

    private static boolean isRecipeAlreadyLoaded(NamespacedKey key)
    {
        return Bukkit.getRecipe(key) != null;
    }

    public static void registerRecipe(ItemStack item, String[] shape, Material[] ingredients)
    {
        ItemStack result = item;
        String recipeName = "UnendurableLove" + item.getItemMeta().getCustomModelData();
        NamespacedKey key = new NamespacedKey(UnendurableLove.Instance, recipeName);

        if(isRecipeAlreadyLoaded(key))
            return;

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i++)
        {
            recipe.setIngredient((char) ('A' + i), ingredients[i]);
        }

        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getSilencerItemStack()
    {
        ItemStack result = new ItemStack(Material.LEATHER_HELMET, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(1984);
        meta.setDisplayName("Simple Ballgag");
        meta.setLore(List.of(ChatColor.GRAY + "No pet can talk while wearing this.",
                ChatColor.GRAY + "*Maybe* it's only for the naughty ones..."));
        result.setItemMeta(meta);
        result.addEnchantment(Enchantment.BINDING_CURSE, 1);

        return result;
    }

    public static ItemStack getSilencerKeyItemStack()
    {
        ItemStack result = new ItemStack(Material.STICK, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(2984);
        meta.setDisplayName("Ballgag Key");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to let the bad pet talk again.",
                ChatColor.GRAY + "Only use if you *really* need to."));
        result.setItemMeta(meta);

        return result;
    }

    public static ItemStack getWhipItemStack()
    {
        ItemStack result = new ItemStack(Material.TWISTING_VINES, 1);
        ItemMeta meta = result.getItemMeta();

        meta.setCustomModelData(1985);
        meta.setDisplayName("Twisted Whip");
        meta.setLore(List.of(ChatColor.GRAY + "If your pet is too slow then kindly tell it about it"));
        result.setItemMeta(meta);

        return result;
    }

    public static void registerAllItems()
    {
        // string-leather-string
        registerRecipe(getSilencerItemStack(), new String[] { "ABA" },
                new Material[] { Material.STRING, Material.LEATHER });
        // iron-iron / empty-stick
        registerRecipe(getSilencerKeyItemStack(), new String[]{ "AA", " B" },
                new Material[]{ Material.IRON_INGOT, Material.STICK } );
        registerRecipe(getWhipItemStack(), new String[]{"ABC"},
                new Material[] {Material.TWISTING_VINES, Material.STRING, Material.STICK});
    }

    public static boolean AreItemTypesEqual(ItemStack item1, ItemStack item2)
    {
        boolean customModelDataIdentical = false;

        if(item1.hasItemMeta() == item2.hasItemMeta())
        {
            if(item1.hasItemMeta() && item2.hasItemMeta())
            {
                if(item1.getItemMeta().hasCustomModelData() == item2.getItemMeta().hasCustomModelData())
                {
                    if(item1.getItemMeta().hasCustomModelData() && item2.getItemMeta().hasCustomModelData())
                    {
                        if (item1.getItemMeta().getCustomModelData() == item2.getItemMeta().getCustomModelData())
                            customModelDataIdentical = true;
                    }
                    else
                    {
                        customModelDataIdentical = true;
                    }
                }
            }
            else
            {
                customModelDataIdentical = true;
            }
        }

        return item1.getType() == item2.getType() && customModelDataIdentical;
    }

}
