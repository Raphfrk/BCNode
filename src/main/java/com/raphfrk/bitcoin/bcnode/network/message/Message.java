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
package com.raphfrk.bitcoin.bcnode.network.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Set;

import com.raphfrk.bitcoin.bcnode.network.elements.MessageElement;
import com.raphfrk.bitcoin.bcnode.util.ByteBufferUtils;
import com.raphfrk.bitcoin.bcnode.util.DigestUtils;
import com.raphfrk.bitcoin.bcnode.util.ParseUtils;

public abstract class Message<T extends Message<?>> implements MessageElement<T> {
	
	public static final int PROTOCOL_VERSION = 60002;
	
	public static final int MAGIC_MAIN_NETWORK = 0xF9BEB4D9;
	
	public static final int SUCCESS = -1;
	
	private final int magic;
	
	protected Message(int magic) {
		this.magic = magic;
	}
	
	/**
	 * Gets the network magic value associated with this message.  
	 * 
	 * @return
	 */
	public int getMagicValue() {
		return magic;
	}
	
	/**
	 * Gets the command string associated with this message
	 * 
	 * @return
	 */
	public abstract String getCommand();
	
	/**
	 * Gets the name of the message
	 * 
	 * @return
	 */
	public String getName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * Serializes the message payload into the buffer
	 * 
	 * @param version the protocol version
	 * @param buf
	 */
	public abstract void put(int version, ByteBuffer buf);
	
	/**
	 * Gets the length of the message payload
	 * 
	 * @param version the protocol version
	 * @return
	 */
	public abstract int getLength(int version);
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" {");
		sb.append(getPayloadString());
		sb.append("}");
		return sb.toString();
	}
	
	protected abstract String getPayloadString();
	
	/**
	 * Decodes a message from the given ByteBuffer.  The decoder scans until the magic value is reached.  
	 * Unless a full message is read, only garbage data before the magic value will be consumed. 
	 * 
	 * @param version the protocol version
	 * @param expectedMagicValue the expected magic value for the network
	 * @param in the ByteBuffer
	 * @return
	 * @throws IOException
	 */
	public static Message<?> decodeMessage(int version, int expectedMagicValue, ByteBuffer in) {
		return decodeMessage(version, expectedMagicValue, in, null);
	}
	
	/**
	 * Decodes a message from the given ByteBuffer.  The decoder scans until the magic value is reached.  
	 * Unless a full message is read, only garbage data before the magic value will be consumed. 
	 * 
	 * @param version the protocol version
	 * @param expectedMagicValue the expected magic value for the network
	 * @param in the ByteBuffer
	 * @param commandSet a set containing acceptable commands, or null for no restriction
	 * @return
	 * @throws IOException
	 */
	public static Message<?> decodeMessage(int version, int expectedMagicValue, ByteBuffer in, Set<String> commandSet) {
		int magic = scanForMagicValue(expectedMagicValue, in);
		
		if (magic != expectedMagicValue) {
			return null;
		}
		
		if (in.remaining() < 20) {
			return null;
		}
		
		int base = in.position();

		in.order(ByteOrder.LITTLE_ENDIAN);
		int length = in.getInt(base + 16);
		in.order(ByteOrder.BIG_ENDIAN);
		
		if (length < 0 || in.remaining() < 24 + length) {
			return null;
		}
		
		byte[] commandBytes = new byte[12];
		in.position(base + 4);
		in.get(commandBytes);
		String command = ParseUtils.commandBytesToString(commandBytes);
		
		if (commandSet != null && !commandSet.contains(command)) {
			return null;
		}

		MessageDecoder decoder = getDecoder(command);
		
		byte[] checksum = new byte[4];
		in.position(base + 20);
		in.get(checksum);
		
		byte[] sha256 = DigestUtils.doubleSHA256(in, base + 24, length);
		
		ByteBufferUtils.equals(in, base + 20, ByteBuffer.wrap(sha256), 0, 4);
		
		in.position(base + 24);

		try {
			Message<?> m = decoder.decodeMessage(version, magic, command, length, in);
			in.position(base + 24 + length);
			return m;
		} catch (IOException e) {
			in.position(base + 4);
			throw new IllegalStateException("Channels should always have sufficent data for message read", e);
		}
	}
	
	/**
	 * Encodes a message to the given output stream, using the current protocol.
	 * 
	 * @param message the Message
	 * @param out the ByteBuffer
	 * @throws IOException
	 * @return the required buffer size, or SUCCESS on success
	 */
	public static int encodeMessage(Message<?> message, ByteBuffer out) {
		return encodeMessage(PROTOCOL_VERSION, message, out);
	}
	
	/**
	 * Encodes a message to the given output stream.
	 * 
	 * @param version the protocol version
	 * @param message the Message
	 * @param out the ByteBuffer
	 * @throws IOException
	 * @return the required buffer size, or SUCCESS on success
	 */
	public static int encodeMessage(int version, Message<?> message, ByteBuffer out) {
		
		int length = message.getLength(version);
		
		if (length + 24 > out.remaining()) {
			return length + 24;
		}
		
		int base = out.position();
		
		out.putInt(message.getMagicValue());
		out.put(ParseUtils.stringToCommandBytes(message.getCommand()));
		
		out.order(ByteOrder.LITTLE_ENDIAN);
		out.putInt(length);
		out.order(ByteOrder.BIG_ENDIAN);
		
		out.position(base + 24);
		message.put(version, out);

		byte[] sha256 = DigestUtils.doubleSHA256(out, base + 24, length);
		out.position(base + 20);
		out.put(sha256, 0, 4);
		
		out.position(base + 24 + length);
		
		return SUCCESS;
	}
	
	/**
	 * Gets the name of the client
	 * 
	 * @return
	 */
	public static String getClientName() {
		// TODO - should be determined from pom.xml
		return "BCNode";
	}
	
	/**
	 * Scans until the magic value is reached.  If there is less than 4 bytes in the buffer, it will return -1.<br>
	 * <br>
	 * The buffer will be positioned at the start of the magic value if one is found.  Otherwise, it will be read until there is fewer then 4 bytes remaining.
	 * 
	 * @param expectedMagicValue
	 * @param in
	 * @return
	 */
	private static int scanForMagicValue(int expectedMagicValue, ByteBuffer in) {
		int base = in.position();
		int skipped = -1;
		int magic = ~expectedMagicValue;
		while (magic != expectedMagicValue) {
			skipped++;
			if (base + skipped + 4 > in.limit()) {
				in.position(Math.max(base, in.limit() - 3));
				return ~expectedMagicValue;
			}
			magic = in.getInt(base + skipped);
		}
		in.position(base + skipped);
		return magic;
	}
	
	private static final HashMap<String, MessageDecoder> decoders;
	private static final MessageDecoder unknownDecoder;
	
	static {
		decoders = new HashMap<String, MessageDecoder>();
		unknownDecoder = new MessageDecoder() {
			public UnknownMessage decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new UnknownMessage(magic, command, length, in);
			}
		};
		registerMessageDecoder("version", new MessageDecoder() {
			@Override
			public Message<?> decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new VersionMessage(magic, version, in);
			}
		});
	}
	
	private static void registerMessageDecoder(String command, MessageDecoder decoder) {
		decoders.put(command, decoder);
	}
	
	private static MessageDecoder getDecoder(String command) {
		MessageDecoder d = decoders.get(command);
		if (d != null) {
			return d;
		}
		return unknownDecoder;
	}
	
	private interface MessageDecoder {
		
		public Message<?> decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException;
		
	}

}
