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
package com.raphfrk.bitcoin.bcnode.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

public class LockFile {
	
	private final File file;
	private FileLock fileLock;
	
	public LockFile(File file) {
		this.file = file;
	}
	
	public synchronized boolean lock() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			fileLock = fos.getChannel().tryLock();
			return fileLock != null;
		} catch (IOException e) {
			throw new IOException("Unable to lock lock file");
		} finally {
			if (fos != null) {
				/*try {
					fos.close();
				} catch (IOException e) {
				}*/
			} 
		}
	}
	
	public synchronized void unlock() {
		if (fileLock != null) {
			try {
				fileLock.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
