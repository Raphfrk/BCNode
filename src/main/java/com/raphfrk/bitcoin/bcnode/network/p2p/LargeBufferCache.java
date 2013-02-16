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
package com.raphfrk.bitcoin.bcnode.network.p2p;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.raphfrk.bitcoin.bcnode.config.Config;

public class LargeBufferCache {
	
	private final static long maxMessageSize = Config.MAX_MESSAGE_SIZE.get();
	
	private final static ConcurrentLinkedQueue<Reference<ByteBuffer>> cache = new ConcurrentLinkedQueue<Reference<ByteBuffer>>();
	
	public static ByteBuffer getBuffer() {
		Reference<ByteBuffer> ref;
		while ((ref = cache.poll()) != null) {
			ByteBuffer buffer = ref.get();
			if (buffer != null) {
				buffer.clear();
				return buffer;
			}
		}
		return ByteBuffer.allocateDirect((int) maxMessageSize);
	}
	
	public static void returnBuffer(ByteBuffer buf) {
		if (buf.limit() == maxMessageSize) {
			cache.add(new SoftReference<ByteBuffer>(buf));
		}
	}
	
}
