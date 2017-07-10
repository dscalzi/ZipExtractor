/*
 * ZipExtractor
 * Copyright (C) 2017 Daniel D. Scalzi
 * See License.txt for license information.
 */
package com.dscalzi.zipextractor.util;

import java.io.File;

import org.bukkit.command.CommandSender;

public class WarnData {

	private final CommandSender sender;
	private final File src;
	private final File dest;
	private final PageList<String> files;
	
	public WarnData(CommandSender sender, File src, File dest, PageList<String> files) {
		this.sender = sender;
		this.src = src;
		this.dest = dest;
		this.files = files;
	}

	public CommandSender getSender() {
		return sender;
	}

	public File getSrc() {
		return src;
	}

	public File getDest() {
		return dest;
	}

	public PageList<String> getFiles() {
		return files;
	}
	
}
