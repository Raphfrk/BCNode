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
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public class VerackMessage extends BitcoinMessage<VerackMessage> {
	
	public VerackMessage(Protocol<BitcoinPeer> protocol) {
		super(protocol);
	}
	
	public VerackMessage(Protocol<BitcoinPeer> protocol, int magic, ByteBuffer buf) throws IOException {
		super(protocol);
	}
	
	@Override
	public String getCommand() {
		return "verack";
	}

	@Override
	public void put(int version, ByteBuffer buf) {
	}

	@Override
	public int getLength(int version) {
		return 0;
	}

	@Override
	protected String getPayloadString() {
		return new StringGenerator()
			.done();
	}

}
