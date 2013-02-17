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
package com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.VersionMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.protocol.BitcoinProtocol;
import com.raphfrk.bitcoin.bcnode.network.message.Message;
import com.raphfrk.bitcoin.bcnode.network.p2p.P2PManager;
import com.raphfrk.bitcoin.bcnode.network.p2p.Peer;

public class BitcoinPeer extends Peer<BitcoinProtocol> {

	public BitcoinPeer(InetSocketAddress addr, P2PManager manager) throws IOException {
		super(addr, manager);
	}
	
	public BitcoinPeer(SocketChannel channel, P2PManager manager) throws IOException {
		super(channel, manager);
	}

	@Override
	public boolean onConnect() {
		long timestamp = System.currentTimeMillis() / 1000L;
		long localPeerId = getId();
		BitcoinProtocol protocol = (BitcoinProtocol) getManager().getProtocol();
		
		VersionMessage versionMessage = new VersionMessage(protocol, 0L, timestamp, null, null, localPeerId, getProtocol().getClientName(), 0);
		super.sendMessage(versionMessage);
		return true;
	}

	@Override
	public void onClosed(CloseReason reason) {
		System.out.println("onClosed called: " + reason);
	}

	@Override
	public void onReceived(Message<?> message) {
		System.out.println("Message received, " + message);
	}

}
