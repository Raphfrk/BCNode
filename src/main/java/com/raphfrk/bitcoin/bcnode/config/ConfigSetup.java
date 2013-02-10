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

public abstract class ConfigSetup<T> {
	
	private final String description;
	private final String key;
	private final T value;
	
	protected ConfigSetup(String key, T value, String description) {
		this.description = description;
		this.key = key;
		this.value = value;
	}
	
	public abstract T get();
	
	public abstract void set(T value);
	
	@SuppressWarnings("unchecked")
	protected String rawAsString(Object value) {
		return asString((T) value);
	}
	
	protected abstract String asString(T value);
	
	public String getDescription() {
		return description;
	}
	
	public String getKey() {
		return key;
	}
	
	public T getDefaultValue() {
		return value;
	}
	
	protected synchronized String getString() {
		return Config.getInstance().getString(getKey());
	}
	
	protected synchronized void setString(String value) {
		Config.getInstance().setString(getKey(), value);
	}
}
