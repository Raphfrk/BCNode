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

public class BooleanConfigSetup extends ConfigSetup<Boolean> {

	private Boolean value;
	
	public BooleanConfigSetup(String key, Boolean value, String description) {
		super(key, value, description);
		this.value = null;
	}
	
	@Override
	public synchronized Boolean get() {
		if (value == null) {
			try {
				value = Boolean.parseBoolean(super.getString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
				value = getDefaultValue();
			}
		}
		return value;
	}

	@Override
	public synchronized void set(Boolean value) {
		System.out.println("Setting value: " + value);
		this.value = value;
		super.setString(asString(value));
	}

	@Override
	protected String asString(Boolean value) {
		return Boolean.toString(value);
	}
	
	
	
}
