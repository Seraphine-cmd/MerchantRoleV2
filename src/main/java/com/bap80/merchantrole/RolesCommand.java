package com.bap80.merchantrole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RolesCommand implements CommandExecutor, TabCompleter {
    private final MerchantRolePlugin plugin;

    public RolesCommand(MerchantRolePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("reload")) {
            reload(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        switch (subcommand) {
            case "progress" -> progress(player);
            case "upgrade" -> advance(player, false);
            case "rankup" -> advance(player, true);
            case "start" -> start(player);
            case "help" -> help(player);
            default -> help(player);
        }
        return true;
    }

    private void progress(Player player) {
        PlayerProfile profile = plugin.store().getOrCreate(player);
        String prefix = plugin.settings().prefix();
        Rank target = profile.rank().next();

        if (target == null) {
            player.sendMessage(prefix + "§aYou have reached the highest rank: §e"
                    + profile.rank().displayName() + "§a.");
            return;
        }

        List<TaskResult> results = plugin.progress().results(player, profile);
        int completed = 0;

        player.sendMessage("");
        player.sendMessage("§6§lMerchantRole §7— §e" + profile.rank().displayName()
                + " §7→ §a" + target.displayName());
        player.sendMessage("§8§m--------------------------------");
        player.sendMessage("§f§l" + target.displayName() + " Tasks");
        for (TaskResult result : results) {
            TaskProgress task = result.progress();
            if (task.complete()) {
                completed++;
            }
            player.sendMessage(taskLine(result));
            if (!task.complete() && task.detail() != null && !task.detail().isBlank()) {
                player.sendMessage("§8   " + task.detail());
            }
        }
        player.sendMessage("§8§m--------------------------------");
        player.sendMessage("§7Tasks: §e" + completed + "/" + results.size() + " §7complete");

        boolean rankup = profile.rank().isRankupTransition();
        double cost = rankup
                ? plugin.settings().rankupCost(profile)
                : plugin.settings().upgradeCost(profile.rank(), target);
        player.sendMessage("§7" + (rankup ? "Rankup" : "Upgrade") + " cost: "
                + (cost <= 0 ? "§aFree" : "§e" + plugin.economy().format(cost)));

        if (!results.isEmpty() && completed == results.size()) {
            player.sendMessage("§aAll tasks complete! §7Use §e/roles "
                    + (rankup ? "rankup" : "upgrade"));
        } else {
            player.sendMessage("§7Finish every white task, then use §e/roles "
                    + (rankup ? "rankup" : "upgrade"));
        }
        player.sendMessage("");
    }

    private void advance(Player player, boolean rankupRequested) {
        PlayerProfile profile = plugin.store().getOrCreate(player);
        String prefix = plugin.settings().prefix();
        Rank current = profile.rank();
        Rank target = current.next();

        if (target == null) {
            player.sendMessage(prefix + "§aYou have reached the highest rank: §e"
                    + current.displayName() + "§a.");
            return;
        }

        boolean rankup = current.isRankupTransition();
        if (rankupRequested != rankup) {
            player.sendMessage(prefix + "§cUse §e/roles "
                    + (rankup ? "rankup" : "upgrade") + " §cfor this transition.");
            return;
        }

        if (!plugin.worldGuard().isRankupAllowed(player)) {
            player.sendMessage(prefix + "§cYou must be inside the WorldGuard region §e"
                    + plugin.settings().rankupRegion() + " §cto rank up.");
            return;
        }

        List<TaskResult> results = plugin.progress().results(player, profile);
        List<TaskResult> missing = new ArrayList<>();
        for (TaskResult result : results) {
            if (!result.complete()) {
                missing.add(result);
            }
        }
        if (!missing.isEmpty()) {
            player.sendMessage(prefix + "§cYou still have unfinished tasks:");
            for (TaskResult result : missing) {
                player.sendMessage(taskLine(result));
            }
            player.sendMessage(prefix + "§7Use §e/roles progress §7to see the full list.");
            return;
        }

        double cost = rankup
                ? plugin.settings().rankupCost(profile)
                : plugin.settings().upgradeCost(current, target);

        if (cost > 0) {
            if (!plugin.economy().available()) {
                player.sendMessage(prefix + "§cNo economy provider is available. Ask an admin to install QuickShop-Hikari/Vault.");
                return;
            }
            double balance = plugin.economy().balance(player);
            if (balance < cost) {
                player.sendMessage(prefix + "§cYou need §e" + plugin.economy().format(cost)
                        + " §cbut you only have §e" + plugin.economy().format(balance) + "§c.");
                return;
            }
            if (!plugin.economy().withdraw(player, cost)) {
                player.sendMessage(prefix + "§cThe payment could not be completed. Please try again.");
                return;
            }
        }

        profile.setRank(target);
        if (rankup) {
            profile.incrementRankupCount();
        }
        plugin.store().markDirty(profile);
        plugin.luckPerms().changeGroup(player, current, target);

        player.sendMessage(prefix + "§aYou " + (rankup ? "ranked up" : "upgraded")
                + " from §e" + current.displayName() + " §ato §e" + target.displayName() + "§a!");
        if (cost > 0) {
            player.sendMessage(prefix + "§7Cost: §e" + plugin.economy().format(cost));
        }

        plugin.grantRewards(player, profile, target);
        plugin.store().flush();
    }

    private void start(Player player) {
        PlayerProfile profile = plugin.store().getOrCreate(player);
        plugin.luckPerms().grant(player, profile.rank());
        player.sendMessage(plugin.settings().prefix() + "§7Your current rank is §e"
                + profile.rank().displayName() + "§7. Here is your task list:");
        progress(player);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("merchantrole.admin")) {
            sender.sendMessage("§cYou do not have permission to reload MerchantRole.");
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(plugin.settings().prefix() + "§aConfiguration reloaded.");
    }

    private void help(CommandSender sender) {
        String prefix = plugin.settings().prefix();
        sender.sendMessage(prefix + "§6MerchantRole commands:");
        sender.sendMessage("§e/roles progress §7- show unfinished and finished tasks");
        sender.sendMessage("§e/roles upgrade §7- advance from rank I to rank II");
        sender.sendMessage("§e/roles rankup §7- advance to the next profession");
        sender.sendMessage("§e/roles start §7- show your first task list");
        if (sender.hasPermission("merchantrole.admin")) {
            sender.sendMessage("§e/roles reload §7- reload config without restarting");
        }
        sender.sendMessage("§7Alias: §e/MerchantRole");
    }

    private String taskLine(TaskResult result) {
        TaskProgress task = result.progress();
        String color = task.complete() ? "§a" : "§f";
        StringBuilder line = new StringBuilder(color).append(" - ")
                .append(result.definition().description(task));
        if (task.goal() > 0) {
            line.append(" §7(").append(task.shownCurrent()).append("/").append(task.goal()).append(")");
        }
        return line.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> options = new ArrayList<>(List.of("progress", "upgrade", "rankup", "start", "help"));
        if (sender.hasPermission("merchantrole.admin")) {
            options.add("reload");
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.startsWith(prefix))
                .sorted()
                .toList();
    }
}
