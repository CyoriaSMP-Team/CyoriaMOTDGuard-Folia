package cyoria.motd;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.network.StatusClient;
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
        StatusClient client = event.getClient();
        int version = client.getProtocolVersion();
        if (version < 0) {
            if (!warned) {warned=true; getLogger().warning("Status client protocol unavailable");}
            return;
        }
        if (seen.add(version)) getLogger().info("Observed status protocol via Paper: " + version);
        if (version == 774) return;

        Object channel = findChannel(client);
        if (channel == null) {
            if (!warned) {warned=true; getLogger().warning("Status client channel unavailable for protocol " + version + "; cannot remove Banner MOTD handler");}
            return;
        }
        try {
            ClassLoader loader = channel.getClass().getClassLoader();
            Object pipeline = Class.forName("io.netty.channel.Channel", false, loader)
                    .getMethod("pipeline").invoke(channel);
            Class<?> pipelineType = Class.forName("io.netty.channel.ChannelPipeline", false, loader);
            Object handler = pipelineType.getMethod("get", String.class).invoke(pipeline, "native_motd");
            if (handler != null) pipelineType.getMethod("remove", String.class).invoke(pipeline, "native_motd");
        } catch (ReflectiveOperationException ex) {
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
