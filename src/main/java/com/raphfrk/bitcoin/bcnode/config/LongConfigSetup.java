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

import java.nio.ByteBuffer;

public class LongConfigSetup extends ConfigSetup<Long> {

	private Long value;
	
	public LongConfigSetup(String key, Long value, String description) {
		super(key, value, description);
		this.value = value;
	}
	
	@Override
	public synchronized Long get() {
		if (value == null) {
			try {
				value = Long.parseLong(super.getString());
			} catch (NumberFormatException e) {
				value = getDefaultValue();
			}
		}
		return value;
	}

	@Override
	public synchronized void set(Long value) {
		this.value = value;
		super.setString(asString(value));
	}

	@Override
	protected String asString(Long value) {
		return Long.toString(value);
	}	
}
