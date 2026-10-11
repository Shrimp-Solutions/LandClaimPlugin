package org.ayosynk.landClaimPlugin.managers;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.bukkit.Bukkit;

/**
 * Manages integrations with third-party plugins such as WorldGuard and
 * PlaceholderAPI.
 */
public class HookManager {

    private final LandClaimPlugin plugin;
    private boolean worldGuardEnabled = false;

    public HookManager(LandClaimPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuardEnabled = true;
            plugin.getLogger().info("WorldGuard detected. Enabling region gap protection.");
        }

        FoliaScheduler.runTask(plugin, () -> {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                new org.ayosynk.landClaimPlugin.hooks.papi.LandClaimExpansion(plugin).register();
                new org.ayosynk.landClaimPlugin.hooks.papi.ClaimPluginExpansion(plugin).register();
                plugin.getLogger().info("PlaceholderAPI detected. Expansions registered.");
            }
        });
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

}
