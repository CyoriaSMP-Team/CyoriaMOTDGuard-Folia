package cyoria.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Restrict BannerMOTD's Netty status decorator by original handshake protocol. */
public final class CyoriaMOTDGuard extends JavaPlugin implements Listener {
    private final Set<Integer> seen = ConcurrentHashMap.newKeySet();
    private volatile boolean warned = false;

    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Status routing guard ready: 1.21.11=Banner; all others=MiniMOTD");
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPing(PaperServerListPingEvent event) {
        Object channel = findChannel(event.getClient());
        if (channel == null) {
            if (!warned) {warned=true; getLogger().warning("Status client channel unavailable: cannot determine version");}
            return;
        }
        try {
            User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
            int version = user == null || user.getClientVersion() == null ? -1 : user.getClientVersion().getProtocolVersion();
            if (seen.add(version)) getLogger().info("Observed status protocol via PacketEvents: " + version);
            if (version == 774) return;
            ChannelPipeline pipeline = ((Channel) channel).pipeline();
            ChannelHandler handler = pipeline.get("native_motd");
            if (handler != null) pipeline.remove("native_motd");
        } catch (Exception ex) {
            if (!warned) {warned=true; getLogger().warning("Status router could not inspect Netty: " + ex.getClass().getSimpleName());}
        }
    }

    private static Object findChannel(Object client) {
        try {
            Field net = findField(client.getClass(),"networkManager");
            if (net==null) return null;
            net.setAccessible(true);
            Object connection=net.get(client);
            Class<?> base=connection.getClass();
            while (base!=null) {
                for (Field field:base.getDeclaredFields()) {
                    for(Class<?> t=field.getType();t!=null;t=t.getSuperclass()) {
                        if("io.netty.channel.AbstractChannel".equals(t.getName()) || "io.netty.channel.Channel".equals(t.getName())) {
                            field.setAccessible(true);
                            Object channel=field.get(connection);
                            if(channel!=null) return channel;
                        }
                    }
                    for(Class<?> t:field.getType().getInterfaces()) {
                        if("io.netty.channel.Channel".equals(t.getName())) {
                            field.setAccessible(true);
                            Object channel=field.get(connection);
                            if(channel!=null) return channel;
                        }
                    }
                }
                base=base.getSuperclass();
            }
        } catch(Exception ignored) {}
        return null;
    }
    private static Field findField(Class<?> cls,String name) {
        while(cls!=null) {
            try {return cls.getDeclaredField(name);} catch(NoSuchFieldException ignored) {}
            cls=cls.getSuperclass();
        }
        return null;
    }
}
