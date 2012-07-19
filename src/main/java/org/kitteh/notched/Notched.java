package org.kitteh.notched;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public class Notched extends JavaPlugin implements Listener {

    private int maxSize;
    private int defaultSize;
    private HashMap<String, Integer> kaboom;

    private boolean complicatedMode;
    private final HashMap<String, Integer> complicatedNodes = new HashMap<String, Integer>();;

    private final String PERM_RELOAD = "notched.reload";
    private final String PERM_USE = "notched.use";
    private final String PERM_UNLIMITED = "notched.unlimited";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ((args.length > 0) && args[0].equalsIgnoreCase("reload") && sender.hasPermission(this.PERM_RELOAD)) {
            final boolean newComplicated = this.loadConfig();
            sender.sendMessage("Reloaded. Max explosion force " + this.maxSize + ", default " + this.defaultSize);
            if (newComplicated) {
                sender.sendMessage("You'll want to modify complicated.yml and reload");
            }
            return true;
        }
        if (sender instanceof Player) {
            if (sender.hasPermission(this.PERM_USE)) {
                int size = 0;
                if (args.length == 0) { // /notched
                    size = this.getDefaultSize(sender);
                } else {
                    if (args[0].equalsIgnoreCase("off")) { // /notched off
                        this.kaboom.remove(sender.getName());
                        sender.sendMessage(ChatColor.YELLOW + "Explosive arrows disabled");
                        return true;
                    } else { // notched <num>
                        try {
                            size = Integer.parseInt(args[0]);
                        } catch (final NumberFormatException exception) {
                            return false;
                        }
                        final int max = this.getMaxSize(sender);
                        if (!sender.hasPermission(this.PERM_UNLIMITED) && (size > max)) {
                            size = max;
                            sender.sendMessage(ChatColor.YELLOW + "Setting your requested explosion size a bit lower.");
                        }
                        if (size < 0) {
                            size = 0;
                        }
                    }
                }
                this.kaboom.put(sender.getName(), size);
                sender.sendMessage(ChatColor.YELLOW + "Your arrows now explode at force " + size);
            }
        }
        return true;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        if (this.loadConfig()) {
            this.getLogger().info("Created new file complicated.yml");
            this.getLogger().info("Reload this plugin (/notched reload) when done");
        }
        this.getLogger().info("Enabled with max explosion force of " + this.maxSize + " and default of " + this.defaultSize);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if ((event.getEntity() instanceof Arrow)) {
            final Arrow arrow = (Arrow) event.getEntity();
            if ((arrow.getShooter() instanceof Player)) {
                final Player player = (Player) arrow.getShooter();
                if (!player.hasPermission(this.PERM_USE) || !this.kaboom.containsKey(player.getName())) {
                    return;
                }
                final float force = this.kaboom.get(player.getName());
                event.getEntity().getWorld().createExplosion(arrow.getLocation(), force);
                event.getEntity().remove();
            }
        }
    }

    private int getComplicated(CommandSender sender) {
        int result = -1;
        for (final String perm : this.complicatedNodes.keySet()) {
            if (sender.hasPermission(perm)) {
                int amt = this.complicatedNodes.get(perm);
                if(amt > result) {
                    result = amt;
                }
            }
        }
        return result;
    }

    private int getDefaultSize(CommandSender sender) {
        if (this.complicatedMode) {
            final int comp = this.getComplicated(sender);
            if (comp <= 0) {
                return comp;
            }
        }
        return this.defaultSize;
    }

    private int getMaxSize(CommandSender sender) {
        if (this.complicatedMode) {
            final int comp = this.getComplicated(sender);
            if (comp >= 0) {
                return comp;
            }
        }
        return this.maxSize;
    }

    private boolean loadConfig() {
        boolean newComplicated = false;
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
        }
        this.reloadConfig();
        this.getConfig().options().copyDefaults(true);
        this.maxSize = this.getConfig().getInt("max-size", 4);
        this.defaultSize = this.getConfig().getInt("default-size", 4);
        this.complicatedMode = this.getConfig().getBoolean("complications.enabled", false);
        if (this.complicatedMode) {
            final File comp = new File(this.getDataFolder(), "complicated.yml");
            if (!comp.exists()) {
                final InputStream source = this.getResource("complicated.yml");
                try {
                    final OutputStream output = new FileOutputStream(comp);
                    int len;
                    final byte[] buf = new byte[1024];
                    while ((len = source.read(buf)) > 0) {
                        output.write(buf, 0, len);
                    }
                    output.close();
                } catch (final Exception ex) {
                    this.getLogger().log(Level.WARNING, "Could not save default config to " + comp, ex);
                }
                try {
                    source.close();
                } catch (final Exception e) {
                    //Meh
                }
                newComplicated = true;
            }
            if (!this.complicatedNodes.isEmpty()) {
                for (final String perm : this.complicatedNodes.keySet()) {
                    this.getServer().getPluginManager().removePermission(perm);
                }
            }
            this.complicatedNodes.clear();
            final YamlConfiguration compConf = YamlConfiguration.loadConfiguration(comp);
            for (final String key : compConf.getKeys(false)) {
                final String perm = "notched.complicated." + key;
                this.complicatedNodes.put(perm, compConf.getInt(key, 0));
                this.getServer().getPluginManager().addPermission(new Permission(perm, PermissionDefault.FALSE));
            }
        }
        this.saveConfig();
        this.kaboom = new HashMap<String, Integer>();
        if (this.maxSize < 4) {
            this.defaultSize = this.maxSize;
        }
        return newComplicated;
    }

}
