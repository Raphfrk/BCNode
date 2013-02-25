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

public class ComparableByteArray implements Comparable<ComparableByteArray> {
	
	private final byte[] arr;
	private final int hash;
	private final boolean scrambled;
	
	public ComparableByteArray(byte[] arr) {
		this(arr, false);
	}
	
	public ComparableByteArray(byte[] arr, boolean scrambleHash) {
		if (arr == null) {
			this.arr = null;
		} else {
			this.arr = new byte[arr.length];
			System.arraycopy(arr, 0, this.arr, 0, arr.length);
		}
		this.scrambled = scrambleHash;
		this.hash = scrambleHash ? hash(arr) : msbHash(arr);
	}
	
	public static int msbHash(byte[] arr) {
		if (arr == null) {
			return -1;
		} else if (arr.length < 4) {
			return hash(arr);
		} else {
			int a = ((0xFF & arr[0]) << 24);
			int b = ((0xFF & arr[1]) << 16);
			int c = ((0xFF & arr[2]) << 8);
			int d = ((0xFF & arr[3]) << 0);
			return a | b | c | d;
		}
	}
	
	public static int hash(byte[] arr) {
		if (arr == null) {
			return 0;
		}
		int h = arr.length;
		for (int i = 0; i < arr.length; i++) {
			h += (h << 5) + arr[i];
		}
		return h;
	}
	
	@Override
	public int hashCode() {
		return hash;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ComparableByteArray)) {
			return false;
		} else {
			return compareTo((ComparableByteArray) other) == 0;
		}
	}

	@Override
	public int compareTo(ComparableByteArray o) {
		if (o == null) {
			return -1;
		}
		int diff = hashCode() - o.hashCode();
		if (diff != 0 && scrambled == o.scrambled) {
			return diff;
		}
		return compareArrays(arr, o.arr);
	}
	
	public static int compareArrays(byte[] a, byte[] b) {
		if (a == null && b == null) {
			return 0;
		}
		int thisLength = a == null ? -1 : a.length;
		int otherLength = b == null ? -1 : b.length;
		int diff = thisLength - otherLength;
		if (diff != 0) {
			return diff;
		}
		for (int i = 0; i < a.length; i++) {
			diff = a[i] - b[i];
			if (diff != 0) {
				return diff;
			}
		}
		return 0;
	}

}
