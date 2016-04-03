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

import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.pluginloader.info.PluginListCallback;
import org.inventivetalent.spiget.api.java.Spiget;
import org.inventivetalent.spiget.api.java.SpigetAPI;
import org.inventivetalent.spiget.api.java.exception.ErrorHandler;
import org.inventivetalent.spiget.api.java.type.Resource;
import org.mcstats.MetricsLite;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginLoader extends JavaPlugin {

	SpigetAPI     spigetAPI;
	PluginManager pluginManager;
	File          downloadFolder;

	List<Resource> availablePlugins = new CopyOnWriteArrayList<>();

	public boolean overrideFile  = false;
	public int     maxSearchSize = 20;
	public boolean allowExternal = false;

	@Override
	public void onEnable() {
		getLogger().info("Initializing SpigetAPI...");
		spigetAPI = Spiget.getAPI();
		spigetAPI.getOptions().setUserAgent("PluginLoader/" + getDescription().getVersion());
		spigetAPI.getOptions().setDisableCache(true);
		spigetAPI.getOptions().setErrorHandler(ErrorHandler.VOID/*new ErrorHandler() {
			@Override
			public void handle(SpigetException e) {
				getLogger().log(Level.WARNING, "An exception in the SpigetAPI occurred", e);
			}
		}*/);
		getLogger().info("Done. API-Version: " + Spiget.API_VERSION);

		saveDefaultConfig();
		overrideFile = getConfig().getBoolean("overrideFiles", false);
		maxSearchSize = getConfig().getInt("maxSearchSize", 20);
		allowExternal = getConfig().getBoolean("allowExternal", false);
		if (allowExternal) {
			getLogger().warning("*******************************************************************************************");
			getLogger().warning("* External plugin downloads enabled. Please be careful which plugins you try to download! *");
			getLogger().warning("*******************************************************************************************");
		}
		downloadFolder = createFolder(new File(getDataFolder(), "downloads"));

		pluginManager = new PluginManager(this);
		new CommandHandler(this);

		refreshAvailablePlugins();

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}
		} catch (Exception e) {
		}
	}

	public void refreshAvailablePlugins() {
		getLogger().info("Reloading list of available plugins...");
		pluginManager.getAvailablePlugins(new PluginListCallback() {
			@Override
			public void list(List<Resource> resources) {
				getLogger().info("Found " + resources.size() + " plugins");
				availablePlugins.clear();
				availablePlugins.addAll(resources);
			}
		});
	}

	//Util methods
	File createFile(File file) {
		try {
			if (!file.exists()) {
				createFile(file.getParentFile());
				file.createNewFile();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return file;
	}

	File createFolder(File folder) {
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return folder;
	}

	//https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
	private SecureRandom random = new SecureRandom();

	public String randomString() {
		return new BigInteger(130, random).toString(32);
	}

	public String mergeArray(String[] array, int offset, String separator) {
		String string = "";

		for (int i = offset; i < array.length; i++) {
			string += array[i] + separator;
		}

		string = string.substring(0, string.length() - separator.length());
		return string;
	}

}
