/*
 * This file is part of BCNode.
 *
 * Copyright (c) Raphfrk 2013 <www.raphfrk.com/bcnode>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.raphfrk.bitcoin.bcnode.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {
	
	public static final DummyConfigSetup LINE1 = new DummyConfigSetup("# BCNode Configuration File"); 
	public static final DummyConfigSetup LINE2 = new DummyConfigSetup(""); 
	public static final ConfigSetup<Long> MAX_MESSAGE_SIZE = new LongConfigSetup("max_message_size", 10485760L, "Maximum message size in bytes");
	
	protected static Config getInstance() {
		return instance;
	}
	
	public final ConfigSetup<?>[] configs = getConfigs();

	private final HashMap<String, ConfigEntry> map = new HashMap<String, ConfigEntry>();
	private List<ConfigEntry> entries = new ArrayList<ConfigEntry>(0);
	private final File file;
	private boolean fileValid;
	private boolean dirty;

	private static final Config instance = new Config("bcnode.conf");
	
	public Config(String name) {
		file = new File(name);
		load();
	}
	
	protected synchronized String getString(String key) {
		ConfigEntry entry = map.get(sanitize(key));
		if (entry == null) {
			return null;
		}
		return entry.getValueString();
	}
	
	protected synchronized void setString(String key, String value) {
		ConfigEntry entry = map.get(sanitize(key));
		if (entry == null) {
			throw new IllegalStateException("Attempt to set key that was not in map");
		}
		entry.setValueString(value);
		dirty = true;
		save();
	}
	
	private synchronized void load() {
		dirty = false;
		fileValid = true;
		boolean newFile = false;
		map.clear();
		entries.clear();
		int lineCounter = 0;
		FileInputStream fin;
		try {
			fin = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			String s;
			try {
				while ((s = reader.readLine()) != null) {
					String[] split = s.split("=", 2);
					if (s.length() == 0 || s.trim().charAt(0) == '#' || split.length == 1) {
						if (split.length == 1 && s.length() != 0 && s.trim().charAt(0) != '#') {
							s = "# " + s;
							dirty = true;
						}
						entries.add(new ConfigEntry(lineCounter++, null, null, s, null));
					} else {
						String[] split2 = split[1].split("#", 2);
						String description = split2.length == 1 ? "" : split2[1];
						ConfigEntry entry = new ConfigEntry(lineCounter++, sanitize(split[0]), sanitize(split[1]), s, description);
						String key = sanitize(split[0]);
						if (!map.containsKey(key)) {
							map.put(key, entry);
							entries.add(entry);
						} else {
							dirty = true;
						}
					}
				}
				reader.close();
			} catch (IOException e) {
				dirty = false;
				fileValid = false;
			}
		} catch (FileNotFoundException e) {
			newFile = true;
		}
		for (ConfigSetup<?> setup : configs) {
			if (!newFile && setup instanceof DummyConfigSetup) {
				continue;
			}
			if (newFile || !map.containsKey(sanitize(setup.getKey()))) {
				ConfigEntry entry;
				if (!(setup instanceof DummyConfigSetup)) {
					String key = sanitize(setup.getKey());
					String value = sanitize(setup.rawAsString(setup.getDefaultValue()));
					entry = new ConfigEntry(lineCounter++, key, null, null, setup.getDescription());
					entry.setValueString(value);
					map.put(key, entry);
				} else {
					entry = new ConfigEntry(lineCounter++, null, null, setup.asString(null), null);
				}
				entries.add(entry);
				dirty = true;
			}
		}
		save();
	}
	
	public synchronized void save() {
		if (!dirty) {
			return;
		}
		dirty = false;
		if (!fileValid) {
			return;
		}
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open file for writing, " + e.getMessage());
			return;
		}
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
		try {
			for (ConfigEntry entry : entries) {
				writer.write(entry.getOriginalLine());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
		}
	}
	
	private ConfigSetup<?>[] getConfigs() {
		Field[] fields = getClass().getDeclaredFields();
		List<ConfigSetup<?>> configs = new ArrayList<ConfigSetup<?>>();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				field.setAccessible(true);
				try {
					Object o = field.get(null);
					if (o instanceof ConfigSetup) {
						configs.add((ConfigSetup<?>) o);
					}
				} catch (IllegalArgumentException e) {
					continue;
				} catch (IllegalAccessException e) {
					continue;
				}
				
			}
		}
		return configs.toArray(new ConfigSetup<?>[0]);
	}
	
	private static String sanitize(String s) {
		return s.trim().toLowerCase();
	}
	
	private static class ConfigEntry implements Comparable<ConfigEntry> {
		
		private final int lineNumber;
		private final String key;
		private final String description;
		private String value;
		private String originalLine;
		
		public ConfigEntry(int lineNumber, String key, String value, String originalLine, String description) {
			this.lineNumber = lineNumber;
			this.key = key;
			this.value = value;
			this.originalLine = originalLine;
			this.description = description;
		}
		
		public int compareTo(ConfigEntry o) {
			return lineNumber - o.lineNumber;
		}
		
		public String getOriginalLine() {
			return originalLine;
		}
		
		public String getKey() {
			return key;
		}
		
		public synchronized String getValueString() {
			return value;
		}
		
		public synchronized void setValueString(String value) {
			this.value = value;
			this.originalLine = sanitize(getKey()) + "=" + sanitize(value) + " # " + description.trim();
		}
	}
	
}
