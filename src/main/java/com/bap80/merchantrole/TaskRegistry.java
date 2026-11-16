package com.bap80.merchantrole;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The task list for every target rank. Tasks belong to the rank being earned,
 * which is why Miner II lists "Obtain the Miner I role".
 */
public final class TaskRegistry {
    private TaskRegistry() {
    }

    public static List<TaskDefinition> forRank(Rank rank) {
        return switch (rank) {
            case MINER_I -> minerI();
            case MINER_II -> minerII();
            case FIGHTER_I -> fighterI();
            case FIGHTER_II -> fighterII();
            case EXPLORER_I -> explorerI();
            case EXPLORER_II -> explorerII();
            case FARMER_I -> farmerI();
            case FARMER_II -> farmerII();
            case PEASANTS -> List.of();
        };
    }

    private static List<TaskDefinition> minerI() {
        TaskDefinition smelt = new TaskDefinition("smelt-raw-iron",
                "Smelt at least %d Raw Iron from Deepslate Iron Ore", context -> {
            int goal = context.goal("smelt-raw-iron", 32);
            return TaskProgress.of(context.counter("smelt_raw_iron"), goal);
        });

        TaskDefinition diamonds = new TaskDefinition("mine-diamond-ore",
                "Mine at least 32 Diamond Ores (or obtain 64 Diamonds)", context -> {
            int mineGoal = context.goal("mine-diamond-ore", 32);
            int diamondGoal = context.goal("obtain-diamonds", 64);
            int mined = context.counter("mined_diamond_ore");
            int diamondsHeld = context.max("max.diamond");
            boolean complete = mined >= mineGoal || diamondsHeld >= diamondGoal;
            int equivalent = Math.max(mined, (int) Math.floor(diamondsHeld * (double) mineGoal / diamondGoal));
            int shown = Math.min(equivalent, mineGoal);
            String detail = "Mined " + mined + "/" + mineGoal + " ores; diamonds "
                    + diamondsHeld + "/" + diamondGoal;
            return new TaskProgress(shown, mineGoal, detail, complete);
        });

        TaskDefinition rawOres = new TaskDefinition("raw-ore-stack",
                "Collect at least %d of each Raw Ore type (Iron, Gold, Copper, Lapis)", context -> {
            int goal = context.goal("raw-ore-stack", 64);
            int iron = context.max("max.raw_iron");
            int gold = context.max("max.raw_gold");
            int copper = context.max("max.raw_copper");
            int lapis = context.max("max.lapis_lazuli");
            int lowest = Math.min(Math.min(iron, gold), Math.min(copper, lapis));
            boolean complete = iron >= goal && gold >= goal && copper >= goal && lapis >= goal;
            String detail = "Iron " + iron + "/" + goal + ", Gold " + gold + "/" + goal
                    + ", Copper " + copper + "/" + goal + ", Lapis " + lapis + "/" + goal;
            return new TaskProgress(lowest, goal, detail, complete);
        });

        return List.of(smelt, diamonds, rawOres);
    }

    private static List<TaskDefinition> minerII() {
        TaskDefinition role = roleTask("miner-i-role", "Obtain the Miner I Role", Rank.MINER_I);

        TaskDefinition tools = new TaskDefinition("diamond-tools",
                "Craft a full set of Diamond Tools (Pickaxe, Axe, Shovel, Sword)", context -> {
            int goal = context.goal("diamond-tools", 4);
            Set<String> collected = context.profile().set("diamond_tools");
            int count = 0;
            List<String> missing = new ArrayList<>();
            for (Material material : ItemUtils.DIAMOND_TOOLS) {
                if (collected.contains(material.name())) {
                    count++;
                } else {
                    missing.add(prettyTool(material));
                }
            }
            String detail = missing.isEmpty() ? "" : "Missing: " + String.join(", ", missing);
            return new TaskProgress(count, goal, detail, count >= goal);
        });

        TaskDefinition enchanted = new TaskDefinition("high-tier-tools",
                "Enchant all Diamond tools with at least one high-tier enchantment", context -> {
            int goal = context.goal("high-tier-tools", 4);
            int count = ItemUtils.countHighTierTools(context.player());
            String detail = ItemUtils.highTierToolStatus(context.player());
            return new TaskProgress(count, goal, detail, count >= goal);
        });

        return List.of(role, tools, enchanted);
    }

    private static List<TaskDefinition> fighterI() {
        TaskDefinition role = roleTask("miner-ii-role", "Obtain the Miner II Role", Rank.MINER_II);

        TaskDefinition sword = new TaskDefinition("diamond-sword", "Craft a Diamond Sword", context -> {
            boolean owned = context.profile().set("diamond_sword").contains(Material.DIAMOND_SWORD.name())
                    || ItemUtils.hasDiamondTool(context.player(), Material.DIAMOND_SWORD);
            return TaskProgress.of(owned ? 1 : 0, 1);
        });

        TaskDefinition armor = new TaskDefinition("diamond-armor",
                "Craft a full set of Diamond Armor", context -> {
            Set<String> collected = context.profile().set("diamond_armor");
            int count = 0;
            List<String> missing = new ArrayList<>();
            for (Material material : ItemUtils.DIAMOND_ARMOR) {
                if (collected.contains(material.name())) {
                    count++;
                } else {
                    missing.add(prettyArmor(material));
                }
            }
            if (count < ItemUtils.DIAMOND_ARMOR.size() && ItemUtils.hasAllDiamondArmor(context.player())) {
                count = ItemUtils.DIAMOND_ARMOR.size();
                missing.clear();
            }
            String detail = missing.isEmpty() ? "" : "Missing: " + String.join(", ", missing);
            return new TaskProgress(count, ItemUtils.DIAMOND_ARMOR.size(), detail,
                    count >= ItemUtils.DIAMOND_ARMOR.size());
        });

        TaskDefinition zombies = counterTask("zombies", "Kill at least %d Zombies", "kill_zombies", 10);
        TaskDefinition babyZombies = counterTask("baby-zombies", "Kill at least %d Baby Zombies",
                "kill_baby_zombies", 5);
        TaskDefinition skeletons = counterTask("skeletons", "Kill at least %d Skeletons",
                "kill_skeletons", 15);

        return List.of(role, sword, armor, zombies, babyZombies, skeletons);
    }

    private static List<TaskDefinition> fighterII() {
        TaskDefinition role = roleTask("fighter-i-role", "Obtain the Fighter I Role", Rank.FIGHTER_I);

        TaskDefinition kills = new TaskDefinition("enchanted-sword-kills",
                "Kill at least %d mobs using a fully enchanted Diamond Sword", context -> {
            int goal = context.goal("enchanted-sword-kills", 200);
            Player player = context.player();
            ItemStack hand = player.getInventory().getItemInMainHand();
            String detail = ItemUtils.requiredSwordStatus(hand);
            return new TaskProgress(context.counter("enchanted_sword_kills"), goal, detail,
                    context.counter("enchanted_sword_kills") >= goal);
        });

        return List.of(role, kills);
    }

    private static List<TaskDefinition> explorerI() {
        TaskDefinition hearts = maxInventoryTask("heart-of-the-sea",
                "Obtain at least %d Heart of the Sea", "max.heart_of_the_sea", 2);
        TaskDefinition egg = maxInventoryTask("sniffer-egg",
                "Obtain at least %d Sniffer Egg", "max.sniffer_egg", 1);
        return List.of(hearts, egg);
    }

    private static List<TaskDefinition> explorerII() {
        TaskDefinition chamber = new TaskDefinition("trial-chamber",
                "Locate and loot at least 1 Trial Chamber", context ->
                TaskProgress.of(context.flag("looted_trial_chamber") ? 1 : 0, 1));

        TaskDefinition vaults = counterTask("ominous-vaults",
                "Open %d Ominous Vaults using Ominous Trial Keys", "ominous_vaults", 5);

        TaskDefinition trims = new TaskDefinition("armor-trims",
                "Collect at least %d distinct Armor Trims from naturally generated structures", context -> {
            int goal = context.goal("armor-trims", 3);
            Set<String> collected = context.profile().set("armor_trims");
            String detail = collected.isEmpty() ? "" : "Collected: " + String.join(", ", collected);
            return new TaskProgress(collected.size(), goal, detail, collected.size() >= goal);
        });

        TaskDefinition sherds = new TaskDefinition("pottery-sherds",
                "Obtain %d unique Pottery Sherds using a Brush", context -> {
            int goal = context.goal("pottery-sherds", 8);
            Set<String> collected = context.profile().set("pottery_sherds");
            String detail = collected.isEmpty() ? "" : "Collected: " + String.join(", ", collected);
            return new TaskProgress(collected.size(), goal, detail, collected.size() >= goal);
        });

        return List.of(chamber, vaults, trims, sherds);
    }

    private static List<TaskDefinition> farmerI() {
        TaskDefinition crops = counterTask("primary-crops",
                "Harvest at least %d primary crops (Wheat, Carrots, Potatoes, Beetroot)",
                "primary_crops", 640);
        TaskDefinition breed = counterTask("breed-animals",
                "Breed at least %d farm animals (Cows, Sheep, Pigs, Chickens)",
                "breed_animals", 20);
        TaskDefinition shear = counterTask("shear-sheep", "Shear at least %d Sheep", "shear_sheep", 32);

        TaskDefinition saplings = new TaskDefinition("plant-saplings",
                "Plant and grow %d Saplings", context -> {
            int plantGoal = context.goal("plant-saplings", 32);
            int growGoal = context.goal("grow-saplings", 32);
            int planted = context.counter("plant_saplings");
            int grown = context.counter("grow_saplings");
            boolean complete = planted >= plantGoal && grown >= growGoal;
            String detail = "Grown " + grown + "/" + growGoal;
            return new TaskProgress(planted, plantGoal, detail, complete);
        });

        TaskDefinition boneMeal = counterTask("bone-meal",
                "Produce %d Bone Meal using a Composter", "bone_meal", 16);

        TaskDefinition milkHoney = maxInventoryTask("milk-honey",
                "Collect at least %d Buckets of Milk or Honey Bottles", "max.milk_honey", 8);

        return List.of(crops, breed, shear, saplings, boneMeal, milkHoney);
    }

    private static List<TaskDefinition> farmerII() {
        TaskDefinition pumpkins = counterTask("pumpkins-melons",
                "Harvest at least %d Pumpkins/Melons", "pumpkins_melons", 320);
        TaskDefinition wart = counterTask("nether-wart",
                "Harvest at least %d Nether Wart", "nether_wart", 128);

        TaskDefinition hatch = new TaskDefinition("hatch-sniffer-egg",
                "Hatch %d Sniffer Egg", context ->
                TaskProgress.of(context.flag("hatched_sniffer_egg") ? 1 : 0, 1));

        TaskDefinition seeds = maxInventoryTask("torchflower-seeds",
                "Harvest %d Torchflower Seeds or Pitcher Pods", "max.torchflower_seeds", 16);
        TaskDefinition mounts = counterTask("breed-tame-mounts",
                "Breed or tame %d Horses, Llamas, or Camels", "breed_tame_mounts", 5);
        TaskDefinition cane = counterTask("sugar-cane-cactus",
                "Harvest %d Sugar Cane or Cactus", "sugar_cane_cactus", 512);
        TaskDefinition food = counterTask("cook-food",
                "Cook or bake %d food items", "cook_food", 320);

        return List.of(pumpkins, wart, hatch, seeds, mounts, cane, food);
    }

    private static TaskDefinition roleTask(String id, String description, Rank required) {
        return new TaskDefinition(id, description, context -> {
            boolean complete = context.profile().rank().ordinal() >= required.ordinal();
            return TaskProgress.of(complete ? 1 : 0, 1);
        });
    }

    private static TaskDefinition counterTask(String id, String description, String counterKey, int fallback) {
        return new TaskDefinition(id, description, context -> {
            int goal = context.goal(id, fallback);
            return TaskProgress.of(context.counter(counterKey), goal);
        });
    }

    private static TaskDefinition maxInventoryTask(String id, String description, String counterKey, int fallback) {
        return new TaskDefinition(id, description, context -> {
            int goal = context.goal(id, fallback);
            return TaskProgress.of(context.max(counterKey), goal);
        });
    }

    private static String prettyTool(Material material) {
        return switch (material) {
            case DIAMOND_PICKAXE -> "Pickaxe";
            case DIAMOND_AXE -> "Axe";
            case DIAMOND_SHOVEL -> "Shovel";
            case DIAMOND_SWORD -> "Sword";
            default -> material.name();
        };
    }

    private static String prettyArmor(Material material) {
        return switch (material) {
            case DIAMOND_HELMET -> "Helmet";
            case DIAMOND_CHESTPLATE -> "Chestplate";
            case DIAMOND_LEGGINGS -> "Leggings";
            case DIAMOND_BOOTS -> "Boots";
            default -> material.name();
        };
    }
}
