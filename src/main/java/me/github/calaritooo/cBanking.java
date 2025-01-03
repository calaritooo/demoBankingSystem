package me.github.calaritooo;

import me.github.calaritooo.commands.BalanceCommand;
import me.github.calaritooo.commands.DebugCommand;
import me.github.calaritooo.commands.PayCommand;
import me.github.calaritooo.listeners.EventHandler;
import me.github.calaritooo.utils.BalancesHandler;
import me.github.calaritooo.utils.MessageHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class cBanking extends JavaPlugin {

    private static cBanking plugin;
    private MessageHandler messageHandler;
    private EventHandler eventHandler;
    private BalancesHandler balancesHandler;
    private VaultHook vaultHook;

    @Override
    public void onEnable() {
        getLogger().info("cBanking is initializing...");

        if (plugin != null) {
            getLogger().severe("Plugin instance already exists! Aborting initialization.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        plugin = this;

        // Creating and/or load config.yml
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // Load messages.yml
        messageHandler = new MessageHandler(this);

        // Initialize balances.yml
        try {
            this.balancesHandler = new BalancesHandler(this);
            getLogger().info("Balance handler initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize balance handler! Disabling cBanking.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        vaultHook = new VaultHook();

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            ServerEconomy serverEconomy = new ServerEconomy(this, balancesHandler);
            serverEconomy.register();
        } else {
            getLogger().severe("Vault is not installed! Disabling cBanking.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        getCommand("balance").setExecutor(new BalanceCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("cbanking").setExecutor(new DebugCommand(this));

        // Register the listeners
        eventHandler = new EventHandler(this);
        eventHandler.registerEvents();

        getLogger().info("cBanking has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("cBanking is disabling...");

        saveConfig();

        if (eventHandler != null) {
            HandlerList.unregisterAll(this);
        }

        // Unregister commands
        getCommand("balance").setExecutor(null);
        getCommand("pay").setExecutor(null);
        getCommand("debug").setExecutor(null);

        if (balancesHandler != null) {
            try {
                balancesHandler.saveBalancesFile();
                getLogger().info("Successfully saved balances.yml!");
            } catch (Exception e) {
                getLogger().severe("Failed to save balances.yml: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Bukkit.getScheduler().cancelTasks(this);

        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            Economy registeredEconomy = registration.getProvider();
            if (registeredEconomy instanceof ServerEconomy) {
                Bukkit.getServicesManager().unregister(registeredEconomy);
                plugin.getLogger().info("Successfully unregistered cBanking from Vault.");
            } else {
                plugin.getLogger().warning("Another Economy provider is registered. No action taken.");
            }
        } else {
            plugin.getLogger().warning("No Economy provider found to unregister.");
        }

        getLogger().info("cBanking has been successfully disabled!");
    }

    // GETTERS //

    public @NotNull FileConfiguration getConfig() {
        return super.getConfig();
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public BalancesHandler getBalancesHandler() {
        return balancesHandler;
    }

    public Economy getEconomy() {
        return vaultHook.getEconomy();
    }

    public static cBanking getInstance() {
        return plugin;
    }
}
