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

import com.raphfrk.bitcoin.bcnode.network.address.AddressStatus;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.message.AddressMessage;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.p2p.BitcoinPeer;
import com.raphfrk.bitcoin.bcnode.network.bitcoin.protocol.BitcoinProtocol;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.network.message.handler.HandshakeMessageHandler;

public class AddressMessageHandler implements HandshakeMessageHandler<AddressMessage, BitcoinPeer, BitcoinProtocol> {
	
	@Override
	public boolean handle(AddressMessage message, BitcoinPeer peer) {
		for (int i = 0; i < message.getAddressCount(); i++) {
			NetworkAddress addr = message.getAddresses(i);
			System.out.println("Peer notify: " + message.getAddresses(0));
			peer.getManager().getAddressStore().notify(addr, AddressStatus.PEER_NOTIFY);
		}
		return true;
	}
	
}
