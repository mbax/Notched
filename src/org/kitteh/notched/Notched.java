package org.kitteh.notched;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Notched extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if ((event.getEntity() instanceof Arrow)) {
            final Arrow arrow = (Arrow) event.getEntity();
            if ((arrow.getShooter() instanceof Player)) {
                final Player player = (Player) arrow.getShooter();
                float force = 0.0F;
                if (player.hasPermission("notched.destroy")) {
                    force = 4.0F;
                }
                event.getEntity().getWorld().createExplosion(arrow.getLocation(), force);
                event.getEntity().remove();
            }
        }
    }
}
