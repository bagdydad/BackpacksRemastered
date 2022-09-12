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

package com.divisionind.bprm.commands;

import com.divisionind.bprm.ACommand;
import com.divisionind.bprm.Backpacks;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CHelp extends ACommand {

    private static final int COMMANDS_PER_PAGE = 4;

    @Override
    public String alias() {
        return "help";
    }

    @Override
    public String desc() {
        return "displays this help information";
    }

    @Override
    public String usage() {
        return "<page>";
    }

    @Override
    public String permission() {
        return "backpacks.help";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        // determine what page of help to display
        int page;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                respond(sender, "&cError: This argument must be an integer.");
                return;
            }
        } else page = 1;

        int numberOfPages = calculateNumberOfPages();
        if (page > numberOfPages) {
            respondf(sender, "&cError: This help page does not exist. The maximum help page is %s.", numberOfPages);
            return;
        }

        respondf(sender, "&bPage (&e%s &b/ &e%s&b)", page, numberOfPages);
        List<ACommand> commands = getOnPage(page);
        for (ACommand cmd : commands) {
            respondnf(sender, "&e/%s %s %s\n  &7%s", label, cmd.alias(), cmd.usage() == null ? "" : cmd.usage(),
                    cmd.desc());
        }
    }

    private static int calculateNumberOfPages() {
        return (Backpacks.getInstance().getCommands().size() / COMMANDS_PER_PAGE) + 1;
    }

    private static List<ACommand> getOnPage(int page) {
        List<ACommand> commands = Backpacks.getInstance().getCommands();
        List<ACommand> newCmds = new ArrayList<>();

        int startCmd = (page - 1) * COMMANDS_PER_PAGE;

        for (int i = startCmd; (i < commands.size() && (i - startCmd) < COMMANDS_PER_PAGE); i++) {
            newCmds.add(commands.get(i));
        }
        return newCmds;
    }
}