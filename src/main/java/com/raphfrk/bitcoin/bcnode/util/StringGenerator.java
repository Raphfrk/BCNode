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

public class StringGenerator {

	private StringBuilder sb = new StringBuilder();
	private boolean first = true;
	private final boolean brackets;
	
	public StringGenerator() {
		this(false);
	}
	
	public StringGenerator(boolean brackets) {
		this.brackets = brackets;
	}
	
	public StringGenerator add(String name, String value) {
		if (first) {
			first = false;
			if (brackets) {
				sb.append('{');
			}
		} else {
			sb.append(", ");
		}
		sb.append(name);
		sb.append(": ");
		sb.append(value);
		return this;
	}
	
	public StringGenerator add(String name, boolean value) {
		return add(name, Boolean.toString(value));
	}
	
	public StringGenerator add(String name, byte value) {
		return add(name, Byte.toString(value));
	}
	
	public StringGenerator add(String name, short value) {
		return add(name, Short.toString(value));
	}
	
	public StringGenerator add(String name, int value) {
		return add(name, value, 10);
	}
	
	public StringGenerator add(String name, int value, int radix) {
		return add(name, Integer.toString(value, radix));
	}
	
	public StringGenerator add(String name, long value) {
		return add(name, value, 10);
	}
	
	public StringGenerator add(String name, long value, int radix) {
		return add(name, Long.toString(value, radix));
	}
	
	public StringGenerator add(String name, byte[] value) {
		return add(name, ParseUtils.bytesToHexString(value));
	}

	public String done() {
		if (brackets) {
			sb.append('}');
		}
		return sb.toString();
	}
	
}
