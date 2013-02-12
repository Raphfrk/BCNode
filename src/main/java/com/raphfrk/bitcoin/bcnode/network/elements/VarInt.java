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
import java.nio.ByteOrder;

import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class VarInt implements MessageElement<VarInt> {
	
	private final long value;

	public VarInt(int version, ByteBuffer buf) throws IOException {
		this.value = get(version, buf);
	}
	
	public VarInt(byte value) {
		this.value = value & 0xFFL;
	}
	
	public VarInt(short value) {
		this.value = value & 0xFFFFL;
	}
	
	public VarInt(int value) {
		this.value = value & 0xFFFFFFFFL;
	}
	
	public VarInt(long value) {
		this.value = value;
	}
	
	public long getValue() {
		return value;
	}

	@Override
	public void put(int version, ByteBuffer buf) {
		put(version, buf, value);
	}

	@Override
	public int getLength(int version) {
		return getLength(version, value);
	}
	
	public static int getLength(int version, long value) {
		if ((value >>> 32) != 0) {
			return 9;
		} else if ((value >>> 16) != 0) {
			return 5;
		} else if (value >= 0xfd) {
			return 3;
		} else {
			return 1;
		}
	}
	
	public static long get(int version, ByteBuffer buf) throws IOException {
		long value;
		byte b1 = buf.get();
		buf.order(ByteOrder.LITTLE_ENDIAN);
		switch (b1) {
			case (byte) 0xff: value = buf.getLong(); break;
			case (byte) 0xfe: value = buf.getInt() & 0xFFFFFFFFL; break;
			case (byte) 0xfd: value = buf.getShort() & 0xFFFFL; break;
			default: value = b1 & 0xFFL; break;
		}
		buf.order(ByteOrder.BIG_ENDIAN);
		return value;
	}
	
	public static void put(int version, ByteBuffer buf, long value) {
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if ((value >>> 32) != 0) {
			buf.put((byte) 0xff);
			buf.putLong(value);
		} else if ((value >>> 16) != 0) {
			buf.put((byte) 0xfe);
			buf.putInt((int) value);
		} else if (value >= 0xfd) {
			buf.put((byte) 0xfd);
			buf.putShort((short) value);
		} else {
			buf.put((byte) value);
		}
		buf.order(ByteOrder.BIG_ENDIAN);
	}
	
	public String toString() {
		return new StringGenerator(true)
			.add("Value", value)
			.done();
	}

}
