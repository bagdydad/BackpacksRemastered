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

import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * This is an adaptor interface for defining 3rd-party plugin-specific functionality.
 */
public abstract class PluginAdaptor {

    /**
     * Initializes the adaptor with the resolved plugin instance.
     * @param parent
     */
    public void onEnable(Plugin parent) throws Exception {}

    private AdaptorManager manager;
    private PluginAdaptorLoader loader;
    private Logger logger;

    final void init(AdaptorManager manager, PluginAdaptorLoader loader) {
        this.manager = manager;
        this.loader = loader;
        this.logger = new AdaptorLogger(this);
    }

    public final AdaptorManager getManager() {
        return manager;
    }

    public final PluginAdaptorLoader getLoader() {
        return loader;
    }

    public Logger getLogger() {
        return logger;
    }
}
