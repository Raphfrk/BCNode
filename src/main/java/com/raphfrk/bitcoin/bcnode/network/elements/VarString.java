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
package com.raphfrk.bitcoin.bcnode.network.elements;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class VarString implements MessageElement<VarString> {
	
	private final String value;
	
	public VarString(int version, ByteBuffer buf) throws IOException {
		value = get(version, buf);
	}
	
	public VarString(String string) {
		this.value = string;
	}

	@Override
	public void put(int version, ByteBuffer buf) {
		put(version, buf, value);
	}

	@Override
	public int getLength(int version) {
		return value.length() + VarInt.getLength(version, value.length());
	}
	
	public static String get(int version, ByteBuffer buf) throws IOException {
		long length = VarInt.get(version, buf);
		if (length < 0 || length > Integer.MAX_VALUE || length > buf.remaining()) {
			throw new IOException("Length field in VarString exceeds buffer remaining, " + length);
		}
		char[] cb = new char[(int) length];
		for (int i = 0; i < (int) length; i++) {
			cb[i] = (char) (buf.get() & 0xFF);
		}
		return new String(cb);
	}
	
	public static void put(int version, ByteBuffer buf, String value) {
		long length = value.length();
		VarInt.put(version, buf, length);
		char[] cb = value.toCharArray();
		for (int i = 0; i < cb.length; i++) {
			buf.put((byte) cb[i]);
		}
	}
	
	public String toString() {
		return new StringGenerator(true)
			.add("Value", value)
			.done();
	}

}
