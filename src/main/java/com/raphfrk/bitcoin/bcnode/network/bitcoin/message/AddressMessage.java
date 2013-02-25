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
import java.nio.ByteBuffer;

import com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p.BitcoinPeer;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.protocol.BitcoinProtocol;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.network.elements.VarInt;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class AddressMessage extends BitcoinMessage<AddressMessage> {
	
	private final NetworkAddress[] addresses;

	public AddressMessage(Protocol<BitcoinPeer> protocol, NetworkAddress[] addresses) {
		super(protocol);
		this.addresses = addresses;
	}
	
	public AddressMessage(int version, BitcoinProtocol protocol, int magic, ByteBuffer buf) throws IOException {
		super(protocol);
		long addressCount = VarInt.get(version, buf);
		if (addressCount > 1000 || addressCount < 0) {
			throw new IOException("Address count exceeded maximum value");
		}
		this.addresses = new NetworkAddress[(int) addressCount];
		for (int i = 0; i < addressCount; i++) {
			addresses[i] = new NetworkAddress(version, buf, false);
		}
	}
	
	public int getAddressCount() {
		return addresses.length;
	}
	
	public NetworkAddress getAddresses(int i) {
		return addresses[i];
	}

	@Override
	public String getCommand() {
		return "addr";
	}

	@Override
	public void put(int version, ByteBuffer buf) {
		VarInt.put(version, buf, addresses.length);
		for (int i = 0; i < addresses.length; i++) {
			addresses[i].put(version, buf);
		}
	}

	@Override
	public int getLength(int version) {
		int length = VarInt.getLength(version, addresses.length);
		if (addresses.length > 0) {
			length += addresses.length * addresses[0].getLength(version);
		}
		return length;
	}

	@Override
	protected String getPayloadString() {
		if (addresses.length == 1) {
			return new StringGenerator()
			.add("Address", addresses[0].toString())
			.done();
		} else {
			return new StringGenerator()
			.add("Address Count", addresses.length)
			.done();
		}
		
	}

}
