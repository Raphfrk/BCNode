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
package com.raphfrk.bitcoin.bcnode.network.bitcoin.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.AddressMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.GetAddressMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.VerackMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.VersionMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.handler.AddressMessageHandler;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.handler.VerackMessageHandler;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.handler.VersionMessageHandler;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p.BitcoinPeer;
import com.raphfrk.bitcoin.bcnode.network.p2p.P2PManager;
import com.raphfrk.bitcoin.bcnode.network.protocol.MessageDecoder;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;

public class BitcoinProtocol extends Protocol<BitcoinPeer> {
	
	public static final int PROTOCOL_VERSION = 60002;
	public static final int MAGIC_MAIN_NETWORK = 0xF9BEB4D9;
	
	public BitcoinProtocol() {
		registerMessageDecoders();
		registerMessageHandlers();
	}
	
	@Override
	public BitcoinPeer getPeer(long id, InetSocketAddress addr, P2PManager manager) throws IOException {
		return new BitcoinPeer(id, addr, manager);
	}

	@Override
	public BitcoinPeer getPeer(long id, SocketChannel channel, P2PManager manager) throws IOException {
		return new BitcoinPeer(id, channel, manager);
	}

	@Override
	public int getVersion() {
		return PROTOCOL_VERSION;
	}

	@Override
	public int getMagicValue() {
		return MAGIC_MAIN_NETWORK;
	}

	private void registerMessageDecoders() {
		super.registerMessageDecoder("version", new MessageDecoder() {
			public VersionMessage decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new VersionMessage(BitcoinProtocol.this, magic, in);
			}
		});
		super.registerMessageDecoder("verack", new MessageDecoder() {
			public VerackMessage decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new VerackMessage(BitcoinProtocol.this, magic, in);
			}
		});
		super.registerMessageDecoder("getaddr", new MessageDecoder() {
			public GetAddressMessage decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new GetAddressMessage(BitcoinProtocol.this, magic, in);
			}
		});
		super.registerMessageDecoder("addr", new MessageDecoder() {
			public AddressMessage decodeMessage(int version, int magic, String command, int length, ByteBuffer in) throws IOException {
				return new AddressMessage(version, BitcoinProtocol.this, magic, in);
			}
		});
	}
	
	private void registerMessageHandlers() {
		super.registerMessageHandler("version", new VersionMessageHandler());
		super.registerMessageHandler("verack", new VerackMessageHandler());
		super.registerMessageHandler("addr", new AddressMessageHandler());
	}
	
}
