package kz.hxncus.mc.fastpluginconfigurer.listener;

import kz.hxncus.mc.fastpluginconfigurer.FastPlayer;
import kz.hxncus.mc.fastpluginconfigurer.FastPluginConfigurer;
import kz.hxncus.mc.fastpluginconfigurer.util.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PlayerListener implements Listener {
    private final FastPluginConfigurer plugin;

    public PlayerListener(FastPluginConfigurer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerWriteConfigValue(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FastPlayer fastPlayer = FastPluginConfigurer.getFastPlayer(player.getUniqueId());
        String message = event.getMessage();
        if (!fastPlayer.isChatSetKey()) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            resetChatSetting(fastPlayer, player);
        }
        String path = fastPlayer.getPath();
        if (path == null) {
            player.sendMessage("Invalid path.");
            return;
        }
        String dataFolderPath = fastPlayer.getDataFolderPath();
        if (StringUtils.isEmpty(dataFolderPath)) {
            player.sendMessage("Invalid dataFolderPath.");
            return;
        }
        File file = new File(dataFolderPath + "/config.yml");
        if (!file.exists()) {
            try {
                new File(dataFolderPath).mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(path, convert(message));
        FileUtils.reload(config, file);

        resetChatSetting(fastPlayer, player);
        event.setCancelled(true);
    }

    private void resetChatSetting(FastPlayer fastPlayer, Player player) {
        fastPlayer.setChatSetKey(false);
        fastPlayer.getChatTask().cancel();

        String pluginName = fastPlayer.getLastPluginName();
        if (pluginName != null) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.dispatchCommand(player, "fpc config " + pluginName));
        }
    }

    @EventHandler
    public void onPlayerWriteConfigKey(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FastPlayer fastPlayer = FastPluginConfigurer.getFastPlayer(player.getUniqueId());
        String message = event.getMessage();
        if (!fastPlayer.isChatAddKey() || message.equalsIgnoreCase("cancel")) {
            fastPlayer.setChatAddKey(false);
            return;
        }
        String path = fastPlayer.getPath();
        if (path == null) {
            player.sendMessage("Invalid path.");
            return;
        }
        String dataFolderPath = fastPlayer.getDataFolderPath();
        if (StringUtils.isEmpty(dataFolderPath)) {
            player.sendMessage("Invalid dataFolderPath.");
            return;
        }
        File file = new File(dataFolderPath + "/config.yml");
        if (!file.exists()) {
            try {
                new File(dataFolderPath).mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String fullPath = path.isEmpty() ? event.getMessage() : path + "." + event.getMessage();
        config.set(fullPath, "");
        FileUtils.reload(config, file);

        fastPlayer.setChatAddKey(false);
        fastPlayer.getChatTask().cancel();

        String pluginName = fastPlayer.getLastPluginName();
        if (pluginName != null) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.dispatchCommand(player, "fpc config " + pluginName));
        }
        event.setCancelled(true);
    }

    private Object convert(String message) {
        if (message.equals("null")) {
            return null;
        }
        if (message.startsWith("{") && message.endsWith("}")) {
            Map<String, Object> objectMap = new HashMap<>();
            for (String messages : message.substring(1, message.length() - 1).split(", ")) {
                String[] splitted = messages.split(":");
                objectMap.put(splitted[0], convert(splitted[1]));
            }
            return objectMap;
        }
        if (message.startsWith("[") && message.endsWith("]")) {
            ArrayList<Object> objects = new ArrayList<>();
            for (String messages : message.substring(1, message.length() - 1).split(", ")) {
                objects.add(convert(messages));
            }
            return objects;
        }
        if ((message.startsWith("\"") && message.endsWith("\"")) || (message.startsWith("'") && message.endsWith("'"))) {
            return message.substring(1, message.length() - 1);
        }
        if (NumberUtils.isNumber(message)) {
            return NumberUtils.createNumber(message);
        }
        if (message.equalsIgnoreCase("true")) {
            return true;
        }
        if (message.equalsIgnoreCase("false")) {
            return false;
        }
        return message;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FastPluginConfigurer.removePlayer(event.getPlayer().getUniqueId());
    }
}
