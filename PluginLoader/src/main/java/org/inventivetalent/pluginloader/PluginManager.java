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

import org.bukkit.Bukkit;
import org.inventivetalent.pluginloader.info.*;
import org.inventivetalent.spiget.api.java.SpigetCallback;
import org.inventivetalent.spiget.api.java.type.FullAuthor;
import org.inventivetalent.spiget.api.java.type.FullResource;
import org.inventivetalent.spiget.api.java.type.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PluginManager {

	static final Pattern SPIGOT_DOWNLOAD_PATTERN = Pattern.compile("https?://www\\.spigotmc\\.org/resources/(.*)/download\\?version=(.*)", Pattern.CASE_INSENSITIVE);

	private PluginLoader plugin;

	//	SpigetDownloader spigetDownloader;

	public PluginManager(PluginLoader plugin) {
		this.plugin = plugin;
		//		spigetDownloader = new SpigetDownloader(/*new File(plugin.getDataFolder(), "cookies.json").toString()*/);
	}

	public void getAvailablePlugins(final PluginListCallback callback) {
		if (callback == null) { throw new IllegalArgumentException("callback cannot be null"); }

		plugin.spigetAPI.getResources(10000, new SpigetCallback<List<Resource>>() {
			@Override
			public void call(List<Resource> resources) {
				callback.list(resources);
			}
		});
	}

	public void getPluginInformation(final String id, final PluginInfoCallback callback) {
		if (id == null) { throw new IllegalArgumentException("id cannot be null"); }
		if (callback == null) { throw new IllegalArgumentException("callback cannot be null"); }

		plugin.spigetAPI.getResource(id, new SpigetCallback<FullResource>() {
			@Override
			public void call(final FullResource fullResource) {
				if (fullResource == null) {
					callback.notFound();
					return;
				}
				final PluginInfo info = PluginInfo.fromResource(fullResource);

				plugin.spigetAPI.getAuthor(fullResource.getAuthorId(), new SpigetCallback<FullAuthor>() {
					@Override
					public void call(FullAuthor author) {
						PluginInfo.applyAuthor(info, author);

						callback.found(info);
					}
				});
			}
		});
	}

	public void searchPlugin(final String query, final PluginSearchCallback callback) {
		if (query == null) { throw new IllegalArgumentException("query cannot be null"); }
		if (callback == null) { throw new IllegalArgumentException("callback cannot be null"); }

		plugin.spigetAPI.searchResource(query, null, plugin.maxSearchSize, new SpigetCallback<List<Resource>>() {
			@Override
			public void call(List<Resource> resources) {
				if (resources == null || resources.isEmpty()) {
					callback.notFound();
					return;
				}
				callback.found(resources);
			}
		});
	}

	public void downloadPlugin(final PluginInfo info, final PluginDownloadCallback callback) {
		if (info == null) { throw new IllegalArgumentException("info cannot be null"); }
		if (callback == null) { throw new IllegalArgumentException("callback cannot be null"); }

		if (info.external) {
			if (!plugin.allowExternal) {
				callback.externalDownload(info.download);
				return;
			}
		}
		downloadPluginUrl(info.external,"https://api.spiget.org/v1/resources/" + info.id + "/download?ut="+System.currentTimeMillis(), callback, info.name + "-" + info.id + "_v" + info.version + info.fileType);
	}

	public void downloadPluginUrl(final boolean external,final String url, final PluginDownloadCallback callback, final String fileName) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				try {
					//					URL realUrl = URLHelper.discoverRealSource(new URL(url));
					plugin.getLogger().info("Downloading File '" + url + "'...");

					File tempFile = plugin.createFile(new File(plugin.downloadFolder, plugin.randomString() + ".plugin"));

					callback.downloadStarted();

					ReadableByteChannel channel;
					try {
						//https://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
						//						HttpURLConnection connection = (HttpURLConnection) new URL("https://download.spiget.org/download?url=" + realUrl.toString()).openConnection();
						HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
						connection.setRequestProperty("User-Agent", "PluginLoader/" + plugin.getDescription().getVersion());
						if (connection.getResponseCode() != 200) {
							callback.requestFailed(external,connection.getResponseCode(), connection.getResponseMessage(), url);
						}
						channel = Channels.newChannel(connection.getInputStream());
					} catch (IOException e) {
						e.printStackTrace();
						callback.downloadFailed(external, url);
						return;
					}
					FileOutputStream output = new FileOutputStream(tempFile);
					output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);

					plugin.getLogger().info("Saved temporary file to '" + tempFile.toString() + "'");
					callback.downloadFinished();

					File targetFile = new File(plugin.getDataFolder().getParentFile(), fileName);
					if (targetFile.exists()) {
						if (!plugin.overrideFile) {
							plugin.getLogger().warning("File '" + targetFile.toString() + "' already exists.");
							targetFile = new File(targetFile.getName() + plugin.randomString());
							plugin.getLogger().warning("Please delete it, '" + targetFile.toString() + "' will be used instead.");
						}
					}
					plugin.createFile(targetFile);
					Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					tempFile.deleteOnExit();

					callback.fileSaved(targetFile.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//	public void downloadExternalPluginUrl(final String url, final PluginDownloadCallback callback, final String fileName) {
	//		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
	//			@Override
	//			public void run() {
	//				try {
	//					URL realUrl = URLHelper.discoverRealSource(new URL(url));
	//					plugin.getLogger().info("Downloading EXTERNAL File '" + realUrl + "'...");
	//
	//					File tempFile = plugin.createFile(new File(plugin.downloadFolder, plugin.randomString() + ".plugin"));
	//
	//					callback.downloadStarted();
	//
	//					String crawledUrl;
	//					try {
	//						HttpURLConnection connection = (HttpURLConnection) new URL("https://crawler.spiget.org/?url=" + realUrl.toString()).openConnection();
	//						connection.setRequestProperty("User-Agent", "PluginLoader/" + plugin.getDescription().getVersion());
	//						JsonObject jsonObject = new JsonParser().parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
	//						if (jsonObject.get("success").getAsBoolean()) {
	//							crawledUrl = jsonObject.get("url").getAsString();
	//						} else {
	//							callback.requestFailed(jsonObject.get("reason").getAsString(), url);
	//							return;
	//						}
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//						callback.downloadOffline(true, url);
	//						return;
	//					}
	//
	//					Matcher matcher = SPIGOT_DOWNLOAD_PATTERN.matcher(crawledUrl);
	//					if (matcher.find()) {
	//						//The file is actually hosted on spigotmc.org, so just try to download it
	//						downloadPluginUrl(crawledUrl, callback, fileName);
	//						return;
	//					}
	//					//The file is hosted externally (hopefully without Cloudflare) -> try to download it directly
	//
	//					ReadableByteChannel channel;
	//					try {
	//						//https://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
	//						HttpURLConnection connection = (HttpURLConnection) new URL(crawledUrl).openConnection();
	//						connection.setRequestProperty("User-Agent", "PluginLoader/" + plugin.getDescription().getVersion());
	//						channel = Channels.newChannel(connection.getInputStream());
	//					} catch (IOException e) {
	//						e.printStackTrace();
	//						callback.downloadOffline(true, url);
	//						return;
	//					}
	//					FileOutputStream output = new FileOutputStream(tempFile);
	//					output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
	//
	//					plugin.getLogger().info("Saved temporary file to '" + tempFile.toString() + "'");
	//					callback.downloadFinished();
	//
	//					File targetFile = new File(plugin.getDataFolder().getParentFile(), fileName);
	//					if (targetFile.exists()) {
	//						if (!plugin.overrideFile) {
	//							plugin.getLogger().warning("File '" + targetFile.toString() + "' already exists.");
	//							targetFile = new File(targetFile.getName() + plugin.randomString());
	//							plugin.getLogger().warning("Please delete it, '" + targetFile.toString() + "' will be used instead.");
	//						}
	//					}
	//					plugin.createFile(targetFile);
	//					Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	//					tempFile.deleteOnExit();
	//
	//					callback.fileSaved(targetFile.getName());
	//				} catch (Exception e) {
	//					e.printStackTrace();
	//				}
	//			}
	//		});
	//	}

	public List<String> matchString(String match, String[] choices) {
		return matchString(match, Arrays.asList(choices));
	}

	public List<String> matchString(String search, List<String> choices) {
		List<String> result = new ArrayList<>();

		String lowerSearch = search.toLowerCase();

		for (String string : choices) {
			String lowerString = string.toLowerCase();

			if (lowerString.contains(lowerSearch)) { result.add(string); }
		}

		return result;
	}

}
