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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.raphfrk.bitcoin.bcnode.network.elements.MessageElement;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;

public abstract class Message<T extends Message<?>> implements MessageElement<T> {
	
	private final static AtomicInteger idCounter = new AtomicInteger();
	
	private final int magic;
	private final Protocol<?> protocol;
	
	protected Message(Protocol<?> protocol) {
		this.magic = protocol.getMagicValue();
		this.protocol = protocol;
	}
	
	/**
	 * Gets the network protocol associated with this message
	 * 
	 * @return
	 */
	public Protocol<?> getProtocol() {
		return protocol;
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
	
	protected static int getUniqueId() {
		return idCounter.getAndIncrement();
	}

}
