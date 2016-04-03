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

package org.inventivetalent.pluginloader.info;

import org.inventivetalent.spiget.api.java.type.FullAuthor;
import org.inventivetalent.spiget.api.java.type.FullResource;

public class PluginInfo {

	public int     id;
	public String  name;
	public String  version;
	public String  download;
	public boolean external;

	public String fileSize;
	public String fileType;

	public int    authorId;
	public String authorName;

	public boolean canDownload() {
		return !external && download != null && !download.isEmpty();
	}

	public String fileInfo() {
		return fileSize + " " + fileType;
	}

	public static PluginInfo fromResource(FullResource resource) {
		PluginInfo info = new PluginInfo();
		info.id = resource.getId();
		info.name = resource.getName();
		info.version = resource.getVersion();
		info.download = resource.getDownload();
		info.external = resource.isExternal();
		info.fileSize = resource.getFileSize();
		info.fileType = resource.getFileType();

		info.authorId = resource.getAuthorId();

		return info;
	}

	public static PluginInfo applyAuthor(PluginInfo original, FullAuthor author) {
		original.authorId = author.getId();
		original.authorName = author.getUsername();

		return original;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		PluginInfo info = (PluginInfo) o;

		if (id != info.id) { return false; }
		if (external != info.external) { return false; }
		if (authorId != info.authorId) { return false; }
		if (name != null ? !name.equals(info.name) : info.name != null) { return false; }
		if (version != null ? !version.equals(info.version) : info.version != null) { return false; }
		if (download != null ? !download.equals(info.download) : info.download != null) { return false; }
		if (fileSize != null ? !fileSize.equals(info.fileSize) : info.fileSize != null) { return false; }
		if (fileType != null ? !fileType.equals(info.fileType) : info.fileType != null) { return false; }
		return !(authorName != null ? !authorName.equals(info.authorName) : info.authorName != null);

	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (version != null ? version.hashCode() : 0);
		result = 31 * result + (download != null ? download.hashCode() : 0);
		result = 31 * result + (external ? 1 : 0);
		result = 31 * result + (fileSize != null ? fileSize.hashCode() : 0);
		result = 31 * result + (fileType != null ? fileType.hashCode() : 0);
		result = 31 * result + authorId;
		result = 31 * result + (authorName != null ? authorName.hashCode() : 0);
		return result;
	}
}
