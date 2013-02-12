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
