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
package com.raphfrk.bitcoin.bcnode.network.bitcoin.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p.BitcoinPeer;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.network.elements.VarString;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class VersionMessage extends BitcoinMessage<VersionMessage> {
	
	public static final long NODE_NETWORK = 1;
	
	private final int version;
	private final long services;
	private final long timestamp;
	private final NetworkAddress remoteAddress;
	private final NetworkAddress localAddress;
	private final long nonce;
	private final VarString userAgent;
	private final int startHeight;

	public VersionMessage(Protocol<BitcoinPeer> protocol, long services, long timestamp, InetSocketAddress remoteAddress, InetSocketAddress localAddress, long nonce, String userAgent, int startHeight) {
		super(protocol);
		this.version = protocol.getVersion();
		this.services = services;
		this.timestamp = timestamp;
		this.remoteAddress = new NetworkAddress(true, services, remoteAddress);
		this.localAddress = new NetworkAddress(true, services, localAddress);
		this.nonce = nonce;
		this.userAgent = new VarString(userAgent);
		this.startHeight = startHeight;
	}
	
	public VersionMessage(Protocol<BitcoinPeer> protocol, int magic, ByteBuffer buf) throws IOException {
		super(protocol);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		this.version = buf.getInt();
		this.services = buf.getLong();
		this.timestamp = buf.getLong();
		buf.order(ByteOrder.BIG_ENDIAN);
		remoteAddress = new NetworkAddress(version, buf, true);
		if (version > 106) {
			localAddress = new NetworkAddress(version, buf, true);
			nonce = buf.getLong();
			userAgent = new VarString(version, buf);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			startHeight = buf.getInt();
			buf.order(ByteOrder.BIG_ENDIAN);
		} else {
			localAddress = new NetworkAddress(true, 0, null);
			nonce = 0;
			userAgent = new VarString("");
			startHeight = 0;
		}
	}

	public int getVersion() {
		return version;
	}
	
	public long getServices() {
		return services;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public NetworkAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	public NetworkAddress getLocalAddress() {
		return localAddress;
	}
	
	public long getNonce() {
		return nonce;
	}
	
	public int getStartHeight() {
		return startHeight;
	}

	@Override
	public String getCommand() {
		return "version";
	}

	@Override
	public void put(int version, ByteBuffer buf) {
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(this.version);
		buf.putLong(services);
		buf.putLong(timestamp);
		remoteAddress.put(this.version, buf);
		buf.order(ByteOrder.BIG_ENDIAN);
		if (this.version > 106) {
			localAddress.put(this.version, buf);
			buf.putLong(nonce);
			userAgent.put(this.version, buf);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			buf.putInt(startHeight);
			buf.order(ByteOrder.BIG_ENDIAN);
		}
	}

	@Override
	public int getLength(int version) {
		int length = 4 + 8 + 8 + remoteAddress.getLength(this.version);
		if (this.version > 106) {
			length += localAddress.getLength(this.version) + 8 + userAgent.getLength(this.version) + 4;
		}
		return length;
	}

	@Override
	protected String getPayloadString() {
		return new StringGenerator()
			.add("Version", version)
			.add("Services", services)
			.add("Timestamp", timestamp)
			.add("RemoteAddress", remoteAddress.toString())
			.add("LocalAddress", localAddress.toString())
			.add("Nonce", nonce)
			.add("UserAgent", userAgent.toString())
			.add("StartHeight", startHeight)
			.done();
	}

}
