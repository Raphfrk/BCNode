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

/*
 * LICENSE NOTE
 * 
 * The code in this class is based on code taken from the Spout server.
 * 
 * SpoutLLC <http://www.spout.org/>
 * 
 * The code is based on code taken from ConsoleManager
 * https://github.com/SpoutDev/Spout/blob/4ed28c7e43b160e2d48525ab07360ed36c514672/src/main/java/org/spout/engine/util/ConsoleManager.java
 * 
 * This code existed at the following location on April 12th 2012 and 
 * therefore must have been published no later than that date.
 * https://github.com/Raphfrk/CraftProxyLib/commit/96c03d5365a016031e02082ea39f5ed8f435b30a
 * 
 * Under the SpoutDev license, the code was released under the MIT license on October 9th 2012, or earlier, under the 180 day rule.
 */
package com.raphfrk.bitcoin.bcnode.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.raphfrk.bitcoin.bcnode.config.Config;

public class LogManager {

	private static Logger logger = Logger.getLogger("BCNode");
	private static String logFile = "logs" + File.separator + "log-%D.txt";

	public static void init() {
		if (Config.LOG_TO_FILE.get()) {
			if (new File(logFile).getParentFile() != null) {
				new File(logFile).getParentFile().mkdirs();
			}
			RotatingFileOutputStream fileOutputStream = new RotatingFileOutputStream(System.out, logFile);
			PrintStream stream = new PrintStream(fileOutputStream, true);
			System.setOut(stream);
			System.setErr(stream);
		}
		Formatter formatter = new DateOutputFormatter(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
		logger.getParent().getHandlers()[0].setFormatter(formatter);
	}

	public static void log(String message) {
		if (logger != null) {
			logger.info(message);
		}
	}

	public static Logger getLogger() {
		return logger;
	}
	
	private static class RotatingFileOutputStream extends OutputStream {
		private final SimpleDateFormat date;
		private final String logFile;
		private String filename;
		private FileOutputStream outputStream;
		private final PrintStream console;

		public RotatingFileOutputStream(PrintStream console, String logFile) {
			this.logFile = logFile;
			this.console = console;
			date = new SimpleDateFormat("yyyy-MM-dd");
			filename = calculateFilename();
			try {
				outputStream = new FileOutputStream(filename, true);
			} catch (FileNotFoundException ex) {
				getLogger().log(Level.SEVERE, "Unable to open {0} for writing: {1}", new Object[] {filename, ex.getMessage()});
				ex.printStackTrace();
			}
		}

		private String calculateFilename() {
			return logFile.replace("%D", date.format(new Date()));
		}

		@Override
		public synchronized void write(int b) throws IOException {
			console.write(b);
			outputStream.write(b);
		}
		
		@Override
		public synchronized void write(byte[] b) throws IOException {
			console.write(b);
			outputStream.write(b);
		}
		
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			console.write(b, off, len);
			outputStream.write(b, off, len);
		}
		
		@Override
		public synchronized void flush() throws IOException {
			console.flush();
			if (!filename.equals(calculateFilename())) {
				filename = calculateFilename();
				getLogger().log(Level.INFO, "Log rotating to {0}...", filename);
				try {
					outputStream = new FileOutputStream(filename, true);
				} catch (FileNotFoundException ex) {
					getLogger().log(Level.SEVERE, "Unable to open {0} for writing: {1}", new Object[] {filename, ex.getMessage()});
					ex.printStackTrace();
				}
			}
			outputStream.flush();
		}
	}
	
	private static class DateOutputFormatter extends Formatter {
		private final SimpleDateFormat date;

		public DateOutputFormatter(SimpleDateFormat date) {
			this.date = date;
		}

		@Override
		public String format(LogRecord record) {
			StringBuilder builder = new StringBuilder();

			builder.append(date.format(record.getMillis()));
			builder.append(" [");
			builder.append(record.getLevel().getLocalizedName().toUpperCase());
			builder.append("] ");
			builder.append(formatMessage(record));
			builder.append('\n');

			if (record.getThrown() != null) {
				StringWriter writer = new StringWriter();
				record.getThrown().printStackTrace(new PrintWriter(writer));
				builder.append(writer.toString());
			}

			return builder.toString();
		}
	}

}
