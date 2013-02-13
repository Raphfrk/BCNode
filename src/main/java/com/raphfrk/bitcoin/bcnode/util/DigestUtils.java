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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class DigestUtils {
	
	public static byte[] SHA256(ByteBuffer buf, int off, int length) {
		int limit = buf.limit();
		int position = buf.position();
		if (off + length > limit) {
			return null;
		}
		buf.position(off);
		buf.limit(off + length);
		try {
			return SHA256(buf.slice(), 1);
		} finally {
			buf.limit(limit);
			buf.position(position);
		}
	}
	
	public static byte[] doubleSHA256(ByteBuffer buf, int off, int length) {
		int limit = buf.limit();
		int position = buf.position();
		if (off + length > limit) {
			return null;
		}
		buf.position(off);
		buf.limit(off + length);
		try {
			return SHA256(buf.slice(), 2);
		} finally {
			buf.limit(limit);
			buf.position(position);
		}	
	}
	
	public static byte[] SHA256(ByteBuffer buf, int n) {
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256", "BC");
			d.reset();
			d.update(buf);
			byte[] message = d.digest();
			for (int i = 1; i < n; i++) {
				d.reset();
				message = d.digest(message);
			}
			return message;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Bouncy castle should support SHA-256", e);
		} catch (NoSuchProviderException e) {
			throw new IllegalStateException("Bouncy castle should be registered as a provider", e);
		}
	}

}
