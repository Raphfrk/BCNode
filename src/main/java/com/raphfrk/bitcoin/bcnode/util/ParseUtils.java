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

public class ParseUtils {
	
	private static final int[] charToInt;
	private static final char[][] byteToCharArray;
	
	static {
		charToInt = new int[256];
		for (int i = '0'; i <= '9'; i++) {
			charToInt[i] = i - '0';
		}
		for (int i = 'a'; i <= 'f'; i++) {
			charToInt[i] = i - 'a' + 10;
		}
		for (int i = 'A'; i <= 'F'; i++) {
			charToInt[i] = i - 'A' + 10;
		}
		byteToCharArray = new char[256][2];
		for (int i = 0; i < 256; i++) {
			byteToCharArray[i][0] = intToHexChar((i >> 4) & 0x0F);
			byteToCharArray[i][1] = intToHexChar((i >> 0) & 0x0F);
		}
	}

	public static byte[] hexStringToBytes(String s) {
		if (s == null) {
			throw new NullPointerException("Cannot parse null string");
		}
		if (s.length() < 0 || s.length() % 2 != 0) {
			throw new IllegalArgumentException("Hex strings must have even lengths");
		}
		char[] chars = s.toCharArray();
		byte[] array = new byte[s.length() >> 1];
		for (int i = 0; i < array.length; i++) {
			int j = i << 1;
			array[i] = (byte) ((hexCharToInt(chars[j]) << 4) + hexCharToInt(chars[j + 1]));
		}
		return array;
	}
	
	public static String bytesToHexString(byte[] arr) {
		if (arr == null) {
			throw new NullPointerException("Cannot encode null byte array");
		}
		char[] chars = new char[arr.length << 1];
		for (int i = 0; i < arr.length; i++) {
			int j = i << 1;
			char[] hexChars = byteToHexChars(arr[i]);
			chars[j] = hexChars[0];
			chars[j + 1] = hexChars[1];
		}
		return new String(chars);
	}
	
	private static int hexCharToInt(char c) {
		return charToInt[c & 0xFF];
	}
	
	private static char[] byteToHexChars(byte i) {
		return byteToCharArray[i & 0xFF];
	}
	
	private static char intToHexChar(int i) {
		if (i < 0) {
			return '?';
		} else if (i < 10) {
			return (char) ('0' + i);
		} else if (i < 16) {
			return (char) ('a' + i - 10);
		} else {
			return '?';
		}
	}
	
}
