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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

import com.raphfrk.bitcoin.bcnode.config.Config;
import com.raphfrk.bitcoin.bcnode.network.streams.ExtendedDataInputStream;
import com.raphfrk.bitcoin.bcnode.network.streams.ExtendedDataOutputStream;
import com.raphfrk.bitcoin.bcnode.util.DigestUtils;
import com.raphfrk.bitcoin.bcnode.util.ParseUtils;

public abstract class Message {
	
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
	 * Serializes the message payload
	 * 
	 * @return
	 */
	public abstract byte[] serialize();
	
	/**
	 * Decodes a message from the given input stream.  The decoder scans until the magic value is reached.
	 * 
	 * @param expectedMagicValue the expected magic value for the network
	 * @param in the InputStream
	 * @return
	 * @throws IOException
	 */
	public static Message decodeMessage(int expectedMagicValue, ExtendedDataInputStream in) throws IOException {
		return decodeMessage(expectedMagicValue, in, null);
	}
	
	/**
	 * Decodes a message from the given input stream.  The decoder scans until the magic value is reached.
	 * 
	 * @param expectedMagicValue the expected magic value for the network
	 * @param in the InputStream
	 * @param commandSet a set containing acceptable commands, or null for no restriction
	 * @return
	 * @throws IOException
	 */
	public static Message decodeMessage(int expectedMagicValue, ExtendedDataInputStream in, Set<String> commandSet) throws IOException {
		int magic = scanForMagicValue(expectedMagicValue, in);
		
		byte[] commandBytes = new byte[12];
		in.readFully(commandBytes);
		String command = ParseUtils.commandBytesToString(commandBytes);
		
		if (commandSet != null && !commandSet.contains(command)) {
			return null;
		}

		MessageDecoder decoder = getDecoder(command);
		
		long length = in.readIntLE() & 0xFFFFFFFFL;
		if (length > Config.MAX_MESSAGE_SIZE.get() || length > Integer.MAX_VALUE) {
			return null;
		}

		byte[] arr = new byte[4];
		in.readFully(arr);

		byte[] data = new byte[(int) length];
		in.readFully(data);
		
		byte[] sha256 = DigestUtils.doubleSHA256(data);
		if (sha256[0] != arr[0] || sha256[1] != arr[1] || sha256[2] != arr[2] || sha256[3] != arr[3]) {
			return null;
		}

		return decoder.decodeMessage(magic, command, data);
	}
	
	/**
	 * Encodes a message to the given output stream.
	 * 
	 * @param message the Message
	 * @param out the OutputStream
	 * @throws IOException
	 */
	public static void encodeMessage(Message message, ExtendedDataOutputStream out) throws IOException {
		out.writeInt(message.getMagicValue());
		out.write(ParseUtils.stringToCommandBytes(message.getCommand()));
		
		byte[] data = message.serialize();
		
		out.writeIntLE(data.length);
		
		byte[] sha256 = DigestUtils.doubleSHA256(data);
		out.write(sha256, 0, 4);
		out.write(data);
	}
	
	private static int scanForMagicValue(int expectedMagicValue, InputStream in) throws IOException {
		int magic = 0;
		while (magic != expectedMagicValue) {
			int i = in.read();
			if (i == -1) {
				throw new EOFException("End of stream reached while scanning for magic value");
			}
			magic = (magic << 8) | (i & 0xFF);
		}
		return magic;
	}
	
	private static final HashMap<String, MessageDecoder> decoders;
	private static final MessageDecoder unknownDecoder;
	
	static {
		decoders = new HashMap<String, MessageDecoder>();
		unknownDecoder = new MessageDecoder() {
			public Message decodeMessage(int magic, String command, byte[] data) throws IOException {
				return new UnknownMessage(magic, command, data);
			}
		};
	}
	
	private void registerMessageDecoder(String command, MessageDecoder decoder) {
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
		
		public Message decodeMessage(int magic, String command, byte[] data) throws IOException;
		
	}

}
