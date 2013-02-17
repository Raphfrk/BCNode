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
package com.raphfrk.bitcoin.bcnode.network.bitcoin.message.handler;

import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.VerackMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.VersionMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p.BitcoinPeer;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.protocol.BitcoinProtocol;
import com.raphfrk.bitcoin.bcnode.network.message.handler.HandshakeMessageHandler;

public class VersionMessageHandler implements HandshakeMessageHandler<VersionMessage, BitcoinPeer, BitcoinProtocol> {

	@Override
	public boolean handle(VersionMessage message, BitcoinPeer peer) {
		long remoteId = message.getNonce();
		if (peer.getManager().getPeer(remoteId) != null) {
			LogManager.log("Disconnecting: Connected to self");
			return false;
		}
		int remoteVersion = message.getVersion();
		int localVersion = peer.getVersion();
		if (localVersion != 0) {
			LogManager.log("Disconnecting: Two version messages received from peer");
			return false;
		}
		if (remoteVersion == 10300) {
			remoteVersion = 300;
		}
		if (remoteVersion < 209) {
			LogManager.log("Disconnecting: Peer uses obsolete protocol version version, " + remoteVersion);
			return false;
		}
		localVersion = Math.min(BitcoinProtocol.PROTOCOL_VERSION, remoteVersion);
		LogManager.log("Setting connection version to " + localVersion);
		peer.setPeerProtocolVersion(localVersion);
		peer.sendMessage(new VerackMessage(peer.getProtocol()));
		return true;
	}

}
