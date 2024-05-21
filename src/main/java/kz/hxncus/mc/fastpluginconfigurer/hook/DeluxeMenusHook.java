package kz.hxncus.mc.fastpluginconfigurer.hook;

import com.extendedclip.deluxemenus.menu.Menu;
import com.extendedclip.deluxemenus.menu.MenuHolder;
import com.extendedclip.deluxemenus.menu.MenuItem;
import kz.hxncus.mc.fastpluginconfigurer.Constants;
import kz.hxncus.mc.fastpluginconfigurer.FastPluginConfigurer;
import kz.hxncus.mc.fastpluginconfigurer.converter.Convertible;
import kz.hxncus.mc.fastpluginconfigurer.language.Messages;
import kz.hxncus.mc.fastpluginconfigurer.util.FileUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DeluxeMenusHook implements Convertible {
    public final FastPluginConfigurer plugin;

    public DeluxeMenusHook(FastPluginConfigurer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void fileToInventory(Player player, String fileName) {
        Block targetBlock = player.getTargetBlockExact(5);
        BlockState state = targetBlock == null ? null : targetBlock.getState();
        if (!(state instanceof Chest)) {
            player.sendMessage(Messages.MUST_LOOKING_AT_DOUBLE_CHEST.getMessage());
            return;
        }
        Menu menu = Menu.getMenu(fileName);
        if (menu == null) {
            Messages.MENU_NOT_FOUND.sendMessage(player, fileName);
        } else {
            storeConfigItemsInInventory(player, ((Chest) state).getInventory(), menu);
        }
    }

    private void storeConfigItemsInInventory(Player player, Inventory chestInventory, Menu menu) {
        chestInventory.clear();
        MenuHolder holder = new MenuHolder(player);
        for (Map.Entry<Integer, TreeMap<Integer, MenuItem>> entry : menu.getMenuItems().entrySet()) {
            for (MenuItem item : entry.getValue().values()) {
                chestInventory.setItem(item.getSlot(), item.getItemStack(holder));
            }
        }
        player.openInventory(chestInventory);
        Messages.SUCCESSFULLY_STORED_ITEMS_TO_CHEST.sendMessage(player);
    }

    @Override
    public void inventoryToFile(Player player, String fileName) {
        File file = new File(plugin.getDirectoryManager().getConverterDirectory(), fileName.endsWith(Constants.YML_EXPANSION) ? fileName : fileName + Constants.YML_EXPANSION);
        if (file.exists()) {
            Messages.FILE_ALREADY_EXISTS.sendMessage(player, fileName);
            return;
        }
        Block targetBlock = player.getTargetBlockExact(5);
        BlockState state = targetBlock == null ? null : targetBlock.getState();
        if (state instanceof Chest) {
            Inventory chestInventory = ((Chest) state).getInventory();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configureInventory(fileName, config, chestInventory);
            int count = 0;
            for (int i = 0; i < chestInventory.getSize(); i++) {
                ItemStack item = chestInventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                storeItemInConfig(item, config, count++, i);
            }
            FileUtil.reload(config, file);
            Messages.CHEST_SUCCESSFULLY_STORED_INTO_FILE.sendMessage(player, fileName);
            return;
        }
        player.sendMessage(Messages.MUST_LOOKING_AT_DOUBLE_CHEST.getMessage());
    }

    @Override
    public List<String> getAllFileNames() {
        return Menu.getAllMenus().stream().map(Menu::getMenuName).collect(Collectors.toList());
    }

    private void configureInventory(String fileName, FileConfiguration config, Inventory chestInventory) {
        config.set("menu_title", fileName);
        config.set("register_command", true);
        config.set("open_command", List.of(fileName));
        config.set("size", chestInventory.getSize());
    }

    private void storeItemInConfig(ItemStack item, FileConfiguration config, int count, int index) {
        String path = String.format("items.%s.", count);
        config.set(path + "material", item.getType().name());
        if (item.getData().getData() != 0) {
            config.set(path + "data", item.getData().getData());
        }
        config.set(path + "amount", item.getAmount());
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta.hasDisplayName()) {
            config.set(path + "display_name", itemMeta.getDisplayName());
        }
        if (itemMeta.hasLore()) {
            config.set(path + "lore", itemMeta.getLore());
        }
        config.set(path + "slot", index);
        if (itemMeta.hasEnchants()) {
            config.set(path + "enchantments", itemMeta.getEnchants().entrySet().stream().map(entry -> entry.getKey().getKey().getKey() + ";" + entry.getValue()).collect(Collectors.toList()));
        }
    }
}
