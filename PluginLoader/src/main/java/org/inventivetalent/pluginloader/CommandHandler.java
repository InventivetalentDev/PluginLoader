/*
 * Copyright 2015-2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package org.inventivetalent.pluginloader;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.inventivetalent.pluginloader.info.PluginDownloadCallback;
import org.inventivetalent.pluginloader.info.PluginInfo;
import org.inventivetalent.pluginloader.info.PluginInfoCallback;
import org.inventivetalent.pluginloader.info.PluginSearchCallback;
import org.inventivetalent.spiget.api.java.type.Resource;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandHandler implements TabCompleter, CommandExecutor {

	public static final Pattern PATTERN_REGULAR_CHARS = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

	private PluginLoader plugin;

	public CommandHandler(PluginLoader plugin) {
		this.plugin = plugin;
		this.plugin.getCommand("pluginloader").setExecutor(this);
		this.plugin.getCommand("pluginloader").setTabCompleter(this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String s, final String[] args) {
		if (args.length == 0) {
			if (sender.hasPermission("pluginloader.install")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aDownload & install a new Plugin");
				sender.sendMessage("§e/pll install §7<Plugin Name or Plugin ID>");
			}
			if (sender.hasPermission("pluginloader.install.url")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aDownload and install a new Plugin from a direct download URL");
				sender.sendMessage("§e/pll installurl §7<Plugin download URL>");
			}
			if (sender.hasPermission("pluginloader.load")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aLoad a plugin file");
				sender.sendMessage("§e/pll load §7<Plugin Name>");
			}
			if (sender.hasPermission("pluginloader.enable")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aEnable a plugin");
				sender.sendMessage("§e/pll enable §7<Plugin Name>");
			}
			if (sender.hasPermission("pluginloader.disable")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aDisable a plugin");
				sender.sendMessage("§e/pll disable §7<Plugin Name>");
			}
			if (sender.hasPermission("pluginloader.search")) {
				sender.sendMessage(" ");
				sender.sendMessage("§aSearch a plugin by its name or tag");
				sender.sendMessage("§e/pll search §7<Search Query>");
			}
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "install": {
				if (!sender.hasPermission("pluginloader.install")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the plugin Name or ID");
					return false;
				}

				final String pluginName = plugin.mergeArray(args, 1, " ");

				plugin.pluginManager.getPluginInformation(pluginName, new PluginInfoCallback() {
					@Override
					public void notFound() {
						sender.sendMessage("§cPlugin '§f" + pluginName + "§c' not found.");
					}

					@Override
					public void found(PluginInfo info) {
						sender.sendMessage("§aFound plugin '§d" + info.name + "§a' (#" + info.id + ") by " + info.authorName);

						plugin.pluginManager.downloadPlugin(info, new PluginDownloadCallback() {
							@Override
							public void externalDownload(String link) {
								sender.sendMessage("§cSorry, you can't directly download this plugin.");
								sender.sendMessage("§ePlease use this link to download it manually:");
								sender.sendMessage("§7" + link);
							}

							@Override
							public void downloadStarted() {
								sender.sendMessage("§7Downloading...");
							}

							@Override
							public void downloadFinished() {
								sender.sendMessage("§aDownload finished.");
							}

							@Override
							public void downloadFailed(boolean external, String link) {
								sender.sendMessage("§cFailed to download the resource.");
								sender.sendMessage("§ePlease try again later, or download it manually:");
								sender.sendMessage("§7" + link);
							}

							@Override
							public void requestFailed(boolean external, int errorCode, String errorMsg, String link) {
								if (external && errorCode == 404) {
									sender.sendMessage("§cThis external resource doesn't support Spiget.");
								}

								sender.sendMessage("§cThe download request failed" + (errorMsg != null && errorMsg.length() > 0 ? ": " + errorMsg : " (Error #" + errorCode + ")."));
							}

							@Override
							public void fileSaved(String file) {
								sender.sendMessage("§aFile saved.");
								if (sender instanceof Player) {
									TextComponent textComponent = new TextComponent("§aPlease use §e/pll load " + file + "§a to load it.");
									textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pll load " + file));
									textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { new TextComponent("§eClick to load §7" + file) }));
									((Player) sender).spigot().sendMessage(textComponent);
								} else {
									sender.sendMessage("§aPlease use §e/pll load " + file + "§a to load it.");
								}
							}
						});
					}
				});
				return true;
			}
			case "installurl": {
				if (!sender.hasPermission("pluginloader.install.url")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the download URL");
					return false;
				}

				String url = plugin.mergeArray(args, 1, " ");

				try {
					plugin.pluginManager.downloadPluginUrl(false/* We don't know if it's external... */, url, new PluginDownloadCallback() {
						@Override
						public void externalDownload(String link) {
						}

						@Override
						public void downloadStarted() {
							sender.sendMessage("§7Downloading...");
						}

						@Override
						public void downloadFinished() {
							sender.sendMessage("§aDownload finished.");
						}

						@Override
						public void downloadFailed(boolean external, String link) {
						}

						@Override
						public void requestFailed(boolean external, int errorCode, String errorMsg, String link) {
						}

						@Override
						public void fileSaved(String file) {
							sender.sendMessage("§aFile saved.");
							if (sender instanceof Player) {
								TextComponent textComponent = new TextComponent("§aPlease use §e/pll load " + file + "§a to load it.");
								textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pll load " + file));
								textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { new TextComponent("§eClick to load §7" + file) }));
								((Player) sender).spigot().sendMessage(textComponent);
							} else {
								sender.sendMessage("§aPlease use §e/pll load " + file + "§a to load it.");
							}
						}
					}, URLEncoder.encode(url, "UTF-8"));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
			case "load": {
				if (!sender.hasPermission("pluginloader.load")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the file name");
					return false;
				}

				String pluginName = plugin.mergeArray(args, 1, " ");

				File pluginFolder = plugin.getDataFolder().getParentFile();
				if (pluginFolder == null || !pluginFolder.exists()) {
					sender.sendMessage("§cPlugin folder not found");
					return false;
				}

				List<String> foundFiles = new ArrayList<>();
				foundFiles.addAll(plugin.pluginManager.matchString(pluginName, pluginFolder.list()));

				File[] files = pluginFolder.listFiles();
				if (files == null || files.length == 0) {
					sender.sendMessage("§cNo plugin files found");
					return false;
				}
				for (File f : files) {
					if (f.getName().endsWith(".jar")) {
						try {
							PluginDescriptionFile desc = plugin.getPluginLoader().getPluginDescription(f);
							if (!plugin.pluginManager.matchString(pluginName, new String[] { desc.getName() }).isEmpty()) {
								if (!foundFiles.contains(f.getName())) { foundFiles.add(f.getName()); }
							}
						} catch (InvalidDescriptionException e) {
							e.printStackTrace();
							sender.sendMessage("§cInvalid plugin file: " + e.getMessage());
							return false;
						}
					}
				}

				if (foundFiles.isEmpty()) {
					sender.sendMessage("§cNo matching files found");
					return false;
				}
				if (foundFiles.size() > 1) {
					sender.sendMessage("§eFound multiple matching files:");
					for (String file : foundFiles) {
						sender.sendMessage("§e" + file);
					}
					return false;
				}

				Plugin loadedPlugin;
				try {
					loadedPlugin = Bukkit.getPluginManager().loadPlugin(new File(pluginFolder, foundFiles.get(0)));
				} catch (InvalidPluginException e) {
					e.printStackTrace();
					sender.sendMessage("§cInvalid plugin: " + e.getMessage());
					return false;
				} catch (InvalidDescriptionException e) {
					e.printStackTrace();
					sender.sendMessage("§cInvalid plugin description: " + e.getMessage());
					return false;
				}

				loadedPlugin.onLoad();
				Bukkit.getPluginManager().enablePlugin(loadedPlugin);

				sender.sendMessage("§aLoaded '§f" + pluginName + "§a'");
				return true;
			}
			case "enable": {
				if (!sender.hasPermission("pluginloader.enable")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the plugin");
					return false;
				}

				String pluginName = plugin.mergeArray(args, 1, " ");

				List<String> choices = new ArrayList<>();
				for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
					choices.add(plugin.getName());
				List<String> result = plugin.pluginManager.matchString(pluginName, choices);

				if (result.isEmpty()) {
					sender.sendMessage("§cPlugin '" + pluginName + "' not found");
					return false;
				}
				if (result.size() > 1) {
					sender.sendMessage("§eFound multiple plugins:");
					for (String res : result) {
						sender.sendMessage("§e" + res);
					}
					return false;
				}
				String plugin = result.get(0);

				if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
					sender.sendMessage("§cPlugin '§f" + plugin + "§c' is already enabled");
					return false;
				}
				Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(plugin));
				sender.sendMessage("§aEnabled '§f" + plugin + "§a'");
				return true;
			}
			case "disable": {
				if (!sender.hasPermission("pluginloader.disable")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the plugin");
					return false;
				}

				String pluginName = plugin.mergeArray(args, 1, " ");

				List<String> choices = new ArrayList<>();
				for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
					choices.add(plugin.getName());
				List<String> result = plugin.pluginManager.matchString(pluginName, choices);

				if (result.isEmpty()) {
					sender.sendMessage("§cPlugin '" + pluginName + "' not found");
					return false;
				}
				if (result.size() > 1) {
					sender.sendMessage("§eFound multiple plugins:");
					for (String res : result) {
						sender.sendMessage("§e" + res);
					}
					return false;
				}
				String plugin = result.get(0);

				if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
					sender.sendMessage("§cPlugin '§f" + plugin + "§c' is not enabled");
					return false;
				}
				Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin(plugin));
				sender.sendMessage("§aDisabled '§f" + plugin + "§a'");
				break;
			}
			case "search": {
				if (!sender.hasPermission("pluginloader.search")) {
					sender.sendMessage("§cNo permission");
					return false;
				}
				if (args.length <= 1) {
					sender.sendMessage("§cPlease specify the search query");
					return false;
				}

				final String pluginName = plugin.mergeArray(args, 1, " ");

				plugin.pluginManager.searchPlugin(pluginName, new PluginSearchCallback() {
					@Override
					public void notFound() {
						sender.sendMessage("§cNo plugin matching '" + pluginName + "' found");
					}

					@Override
					public void found(List<Resource> resources) {
						sender.sendMessage("§aFound " + resources.size() + " plugins:");

						for (Resource resource : resources) {
							if (sender instanceof Player) {
								TextComponent textComponent = new TextComponent("§e" + resource.getName() + " §7(#" + resource.getId() + ")");
								textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pluginloader install " + resource.getId()));
								textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { new TextComponent("§eClick to install " + resource.getName()) }));
								((Player) sender).spigot().sendMessage(textComponent);
							} else {
								sender.sendMessage("§e" + resource.getName() + " §7(#" + resource.getId() + ")");
							}

						}
					}
				});
				return true;
			}
			default: {
				sender.sendMessage("§cUnknown command");
				break;
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
		List<String> list = new ArrayList<>();

		if (args.length == 1) {
			if (sender.hasPermission("pluginloader.install")) {
				list.add("install");
			}
			if (sender.hasPermission("pluginloader.install.url")) {
				list.add("installurl");
			}
			if (sender.hasPermission("pluginloader.load")) {
				list.add("load");
			}
			if (sender.hasPermission("pluginloader.enable")) {
				list.add("enable");
			}
			if (sender.hasPermission("pluginloader.disable")) {
				list.add("disable");
			}
			if (sender.hasPermission("pluginloader.search")) {
				list.add("search");
			}
		}
		if (args.length > 1) {
			switch (args[0].toLowerCase()) {
				case "enable": {
					for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
						if (!plugin.isEnabled()) { list.add(plugin.getName()); }
					}
					break;
				}
				case "disable": {
					for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
						if (plugin.isEnabled()) { list.add(plugin.getName()); }
					}
					break;
				}
				case "load": {
					File pluginFolder = plugin.getDataFolder().getParentFile();
					if (pluginFolder != null && pluginFolder.exists()) {
						File[] files = pluginFolder.listFiles();
						if (files != null) {
							for (File file : files) {
								if (!file.isDirectory()) { list.add(file.getName()); }
							}
						}
					}
					break;
				}
				case "install": {
					if (args[1].length() > 1) {
						for (Resource resource : plugin.availablePlugins) {
							if (!PATTERN_REGULAR_CHARS.matcher(resource.getName()).find()) {
								list.add(resource.getName());
							}
						}
					}
					break;
				}
				default:
					break;
			}
		}

		return TabCompletionHelper.getPossibleCompletionsForGivenArgs(args, list.toArray(new String[list.size()]));
	}
}
