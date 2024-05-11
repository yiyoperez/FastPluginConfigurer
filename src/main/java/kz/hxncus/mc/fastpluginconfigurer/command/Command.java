package kz.hxncus.mc.fastpluginconfigurer.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public interface Command extends CommandExecutor, TabCompleter {
    void execute(CommandSender sender, org.bukkit.command.Command command, String label, String[] args);
    List<String> complete(CommandSender sender, org.bukkit.command.Command command, String[] args);
}
