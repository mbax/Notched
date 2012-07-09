package org.kitteh.notched;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Notched extends JavaPlugin implements Listener {

    private int maxSize;
    private int defaultSize;
    private HashMap<String, Integer> kaboom;
    private final String PERM_RELOAD = "notched.reload";
    private final String PERM_USE = "notched.use";
    private final String PERM_UNLIMITED = "notched.unlimited";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission(this.PERM_RELOAD)) {
                this.loadConfig();
                sender.sendMessage("Reloaded. Max explosion force " + this.maxSize + ", default " + this.defaultSize);
                return true;
            }
        }
        if (sender instanceof Player) {
            if (sender.hasPermission(this.PERM_USE)) {
                int size = 0;
                if (args.length == 0) {
                    size = this.defaultSize;
                } else {
                    if (args[0].equalsIgnoreCase("off")) {
                        this.kaboom.remove(sender.getName());
                        sender.sendMessage(ChatColor.YELLOW + "Explosive arrows disabled");
                        return true;
                    } else {
                        try {
                            size = Integer.parseInt(args[0]);
                        } catch (final NumberFormatException exception) {
                            return false;
                        }
                        if (!sender.hasPermission(this.PERM_UNLIMITED) && (size > this.maxSize)) {
                            size = this.maxSize;
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
        this.loadConfig();
        this.getLogger().info("Enabled with max explosion force of " + this.maxSize + " and default of " + this.defaultSize);
    }

    private void loadConfig() {
        this.reloadConfig();
        this.getConfig().options().copyDefaults(true);
        this.maxSize = this.getConfig().getInt("max-size", 4);
        this.defaultSize = this.getConfig().getInt("default-size", 4);
        this.saveConfig();
        this.kaboom = new HashMap<String, Integer>();
        if (this.maxSize < 4) {
            this.defaultSize = this.maxSize;
        }
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
}
