/*
 * BackpacksRemastered - remastered version of the popular Backpacks plugin
 * Copyright (C) 2019, Andrew Howard, <divisionind.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.divisionind.bprm;

import com.divisionind.bprm.adapters.AdaptorGriefPrevention;
import com.divisionind.bprm.commands.*;
import com.divisionind.bprm.events.*;
import com.divisionind.bprm.events.custom.GameTickEvent;
import com.divisionind.bprm.nms.KnownVersion;
import com.divisionind.bprm.nms.reflect.NMS;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Backpacks extends JavaPlugin {

    public static final String PREFIX = translate("&9Backpacks &7&l>>&r ");
    public static final String VERSION = "@DivisionVersion@";
    public static final String GIT_HASH = "@DivisionGitHash@";
    public static final String GIT_NUM = "@DivisionGitComm@";
    public static final int CONFIGURATION_VERSION = 7;

    public static ResourceBundle bundle;
    public static long OPEN_BACKPACK_COOLDOWN;
    public static int MAX_COMBINED_BACKPACKS;

    private static Backpacks instance;

    private List<ACommand> commands;
    private Metrics metrics;
    private BackpackRecipes backpackRecipes;
    private AdaptorManager adaptorManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        commands = new ArrayList<>();
        adaptorManager = new AdaptorManager(this);

        // load / init config
        saveDefaultConfig();
        setupFromConfig();

        // initialize game ticking with this plugin
        GameTickEvent.initialize(this);

        registerCommands(new CHelp(),
                new CInfo(),
                new CItemInfo(),
                new CItemInfoGet(),
                new CItemGive(),
                new CConfigReload(),
                new CSplit(),
                new CMaterialsList(),
                new CMaterialsSearch(),
                new CVFurnace());

        getAdaptorManager().registerAdaptors(AdaptorGriefPrevention.class);

        registerEvents(new BackpackCraftEvent(),
                new BackpackDamageEvent(),
                new BackpackCloseEvent(),
                new BackpackOpenEvent(),
                new BackpackLinkEvent(),
                new BackpackInvClickEvent(),
                new BackpackFurnaceTickEvent(),
                new CloseOpenEnforcerEvent());

        // register these events if enabled
        if (getConfig().getBoolean("trackFurnaceBackpacks")) {
            registerEvents(new BackpackTrackEvents());
            if (!KnownVersion.v1_14_R1.isBefore()) {
                registerEvents(new BackpackTrackEvents.InventoryClickTrackEvent());
            } else {
                getLogger().warning("Backpack tracking is enabled but not fully supported in this version. " +
                        "Please consider upgrading.");
            }
        }

        getLogger().info(String.format("Detected NMS %s. Using this for all NMS related functions.", NMS.VERSION));
        List<Exception> nmsExceptions = NMS.initialize();
        if (nmsExceptions.size() > 0) {
            getLogger().severe(nmsExceptions.size() + " error(s) initializing NMS. Was the detected server wrong? " +
                    "If not, then NMS has changed significantly sense this plugin was released and therefore, " +
                    "it can not fully adapt.");
            for (Exception ex : nmsExceptions) ex.printStackTrace();
        }

        // register custom recipes
        backpackRecipes = new BackpackRecipes(getConfig(), getLogger());
        // enable metrics collection
        metrics = new Metrics(this);
        // add task for post load
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::onPostLoad);

        getLogger().info(String.format("BackpacksRemastered v%s (git: %s) was enabled in %.2fs!", VERSION, GIT_HASH,
                ((double)(System.currentTimeMillis() - startTime)) / 1000.0D));
    }

    @Override
    public void onDisable() {
        // TODO look for any open backpacks and close them gracefully (to prevent possible dupe)
        getLogger().info(String.format("BackpacksRemastered v%s (git: %s) has been disabled.", VERSION, GIT_HASH));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("backpacks")) {
            if (args.length > 0) {
                String subcmd = args[0].toLowerCase();
                for (ACommand command : commands) {
                    if (command.matchesAlias(subcmd)) {
                        command.call(sender, label, args);
                        return true;
                    }
                }

                // no command found by sub, attempts to parse as int for help pages
                try {
                    int hpage = Integer.parseInt(subcmd);
                    new CHelp().call(sender, label, new String[] {"help", Integer.toString(hpage)});
                } catch (NumberFormatException e) {
                    // not an int, idk wtf they were doing then
                    ACommand.respondf(sender, "&cError: Command \"%s\" not found.", subcmd);
                }
            } else new CHelp().call(sender, label, args);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> entries = new ArrayList<>();

        if (args.length > 1) {
            for (ACommand cmd : commands) {
                if (cmd.matchesAlias(args[0])) {
                    if (!cmd.hasPerm(sender)) break;
                    entries = cmd.tabComplete(sender, command, alias, args);
                    break;
                }
            }
        } else
        if (args.length == 1) {
            for (ACommand cmd : commands) {
                if (!cmd.hasPerm(sender)) continue;
                String ali = cmd.alias();
                if (ali.startsWith(args[0])) entries.add(ali);
            }
        } else {
            for (ACommand cmd : commands) {
                if (!cmd.hasPerm(sender)) continue;
                entries.add(cmd.alias());
            }
        }

        return entries;
    }

    public void onPostLoad() {
        // this code is executed after every plugin has been loaded
        getAdaptorManager().reloadAdaptors();
    }

    public void setupFromConfig() {
        int inputVersion;
        try {
            inputVersion = Integer.parseInt(getConfig().getString("version"));
        } catch (NumberFormatException e) {
            inputVersion = -1;
        }

        if (inputVersion != CONFIGURATION_VERSION) {
            File configFile = new File(getDataFolder(), "config.yml");
            File configFileBak = new File(getDataFolder(), "config.yml.bak");
            try {
                Files.move(configFile.toPath(), configFileBak.toPath(), StandardCopyOption.REPLACE_EXISTING);
                saveDefaultConfig();
            } catch (IOException e) {
                getLogger().severe("An error occurred backing up the configuration for an update.");
                e.printStackTrace();
            }

            getLogger().warning("Your configuration file was outdated. The old configuration was moved to " +
                    "config.yml.bak and a new one has been created. Please manually copy over your settings.");
        }

        // checks for lang and country code TODO this needs work
        String language = getConfig().getString("language", "en");
        Locale loc;
        if (language.contains("-")) {
            String[] parts = language.split("-");
            loc = new Locale(parts[0], parts[1]);
        } else loc = new Locale(language);

        Locale.setDefault(loc);
        bundle = ResourceBundle.getBundle("lang");

        OPEN_BACKPACK_COOLDOWN = getConfig().getLong("openBackpackCooldown");
        MAX_COMBINED_BACKPACKS = getConfig().getInt("maxNumberOfCombinedBackpacks");
        if (MAX_COMBINED_BACKPACKS > 9 || MAX_COMBINED_BACKPACKS < 1) MAX_COMBINED_BACKPACKS = 9;
    }

    private void registerEvents(Listener... listeners) {
        for (Listener lis : listeners) Bukkit.getPluginManager().registerEvents(lis, this);
    }

    public void registerCommands(ACommand... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    public List<ACommand> getCommands() {
        return commands;
    }

    public static String translate(String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    public static Backpacks getInstance() {
        return instance;
    }

    public static AdaptorManager getAdaptorManager() {
        return instance.adaptorManager;
    }

    public static void removeFakeBackpackViewer(Inventory inv) {
        List<HumanEntity> viewers = inv.getViewers();

        for (int i = 0; i < viewers.size(); i++) {
            if (viewers.get(i) instanceof FakeBackpackViewer) {
                viewers.remove(i);
                return;
            }
        }
    }
}
