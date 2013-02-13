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
package com.raphfrk.bitcoin.bcnode.network.elements;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.raphfrk.bitcoin.bcnode.util.ParseUtils;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class NetworkAddress implements MessageElement<NetworkAddress> {
	
	private final boolean versionMessage;
	private final int timestamp;
	private final long services;
	private final byte[] addr;
	private final int port;
	
	public NetworkAddress(boolean versionMessage, long services, InetSocketAddress address) {
		this(true, 0, services, address);
		if (!versionMessage) {
			throw new IllegalArgumentException("A timestamp must be provided except for version messages");
		}
	}
	
	public NetworkAddress(int timestamp, long services, InetSocketAddress address) {
		this(false, timestamp, services, address);
	}
	
	private NetworkAddress(boolean versionMessage, int timestamp, long services, InetSocketAddress address) {
		this.versionMessage = versionMessage;
		this.timestamp = timestamp;
		this.services = services;
		this.addr = addrToBytes(address);
		this.port = address == null ? 0 : address.getPort();
	}
	
	public NetworkAddress(int version, ByteBuffer buf) throws IOException {
		this(version, buf, false);
	}
	
	public NetworkAddress(int version, ByteBuffer buf, boolean versionMessage) throws IOException {
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if (version >= 31402 && !versionMessage) {
			timestamp = buf.getInt();
		} else {
			timestamp = 0;
		}
		services = buf.getLong();
		buf.order(ByteOrder.BIG_ENDIAN);
		addr = new byte[16];
		buf.get(addr);
		port = buf.getShort() & 0xFFFF;
		this.versionMessage = versionMessage;
	}
	
	public InetSocketAddress getInetSocketAddress() {
		return new InetSocketAddress(getInetAddress(), getPort());
	}
	
	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			throw new IllegalStateException("The address must always be 16 bytes long, " + addr.length);
		}
	}
	
	public int getPort() {
		return port;
	}

	@Override
	public void put(int version, ByteBuffer buf) {
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if (version >= 31402 && !versionMessage) {
			buf.putInt(timestamp);
		}
		buf.putLong(services);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.put(addr);
		buf.putShort((short) port);
	}

	@Override
	public int getLength(int version) {
		return (version >= 31402 && !versionMessage)? 30 : 26;
	}
	
	public static byte[] addrToBytes(InetSocketAddress socketAddr) {
		byte[] bytes = new byte[16];
		bytes[10] = (byte) 0xFF;
		bytes[11] = (byte) 0xFF;
		
		if (socketAddr == null) {
			return bytes;
		}
		InetAddress inetAddr = socketAddr.getAddress();
		if (inetAddr == null) {
			return bytes;
		}
		byte[] inetBytes = inetAddr.getAddress();
		if (inetBytes.length == 4) {
			bytes[10] = (byte) 0xFF;
			bytes[11] = (byte) 0xFF;
			bytes[12] = (byte) (inetBytes[0] >> 24);
			bytes[13] = (byte) (inetBytes[1] >> 16);
			bytes[14] = (byte) (inetBytes[2] >> 8);
			bytes[15] = (byte) (inetBytes[3] >> 0);
		} else if (inetBytes.length == 16) {
			return inetBytes;
		}
		return bytes;
	}
	
	private String getNetworkAddress() {
		if (!isIP4Address()) {
			return ParseUtils.bytesToHexString(addr);
		}
		byte arr[] = new byte[4];
		for (int i = 12; i < 16; i++) {
			arr[i - 12] = addr[i];
		}
		return ParseUtils.bytesToDotDecimal(arr);
	}
	
	private boolean isIP4Address() {
		for (int i = 0; i < 10; i++) {
			if (addr[i] != 0) {
				return false;
			}
		}
		for (int i = 10; i < 12; i++) {
			if (addr[i] != -1) {
				return false;
			}
		}
		return true;
	}
	
	public String toString() {
		return new StringGenerator(true)
			.add("VersionMessage", versionMessage)
			.add("Timestamp", timestamp)
			.add("Services", services)
			.add("Address", getNetworkAddress())
			.add("Port", port)
			.done();
	}

}
