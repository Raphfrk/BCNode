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


public class ConstantHashMap<K, V> {
	
	private int mask;
	private Object[] keys;
	private Object[] values;

	public ConstantHashMap() {
		keys = new Object[16];
		values = new Object[16];
		mask = keys.length - 1;
	}
	
	/**
	 * Gets the value that is mapped to a given key or null if none.  This
	 * method is thread safe for reading, but should not be called during 
	 * initialization.
	 * 
	 * @param key
	 * @param value the value, or null if the key is unmapped
	 */
	@SuppressWarnings("unchecked")
	public V get(K key) {
		if (key == null) {
			throw new NullPointerException("Null keys are not supported");
		}
		int h = key.hashCode() & mask;
		K k = (K) keys[h];
		if (!(key.equals(k))) {
			return null;
		}
		return (V) values[h];
	}
	
	/**
	 * Puts a key, value pair into the map.  This method is not thread safe 
	 * and should only be called during initialization.
	 * 
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		while (!putRaw(key, value)) {
			rehash(keys.length << 1);
		}
	}
	
	private boolean putRaw(K key, V value) {
		int h = key.hashCode() & mask;
		if (keys[h] != null) {
			System.out.println("Not empty");
			return false;
		}
		keys[h] = key;
		values[h] = value;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private boolean rehash(int newSize) {
		newSize = MathUtils.increaseToPow2(newSize);
		if (newSize > 65536) {
			throw new IllegalArgumentException("Array size to large");
		}
		Object[] oldKeys = keys;
		Object[] oldValues = values;
		keys = new Object[newSize];
		values = new Object[newSize];
		mask = newSize - 1;
		for (int i = 0; i < oldKeys.length; i++) {
			if (!putRaw((K) oldKeys[i], (V) oldValues[i])) {
				keys = oldKeys;
				values = oldValues;
				return false;
			}
		}
		return true;
	}
}
