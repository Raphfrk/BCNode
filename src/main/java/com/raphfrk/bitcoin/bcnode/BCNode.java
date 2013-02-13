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
package com.raphfrk.bitcoin.bcnode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.raphfrk.bitcoin.bcnode.network.message.Message;
import com.raphfrk.bitcoin.bcnode.network.message.VersionMessage;
import com.raphfrk.bitcoin.bcnode.util.ParseUtils;

public class BCNode {
	public static void main( String[] args ) throws NoSuchAlgorithmException, NoSuchProviderException, UnknownHostException, IOException {
		Security.addProvider(new BouncyCastleProvider());

		SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 8333));

		ByteBuffer buf = ByteBuffer.allocate(1024);

		int timestamp = (int) (System.currentTimeMillis() / 1000);

		VersionMessage versionMessage = new VersionMessage(Message.MAGIC_MAIN_NETWORK, Message.PROTOCOL_VERSION, VersionMessage.NODE_NETWORK, timestamp, null, null, 0x123456789ABCDEFL, Message.getClientName(), 200000);

		Message.encodeMessage(versionMessage, buf);

		buf.flip();

		// Test message from wiki
		byte[] testMessage = ParseUtils.hexStringToBytes(
				"f9beb4d976657273696f6e0000000000" + 
				"64000000358d493262ea000001000000" + 
				"0000000011b2d0500000000001000000" + 
				"0000000000000000000000000000ffff" + 
				"00000000000000000000000000000000" + 
				"0000000000000000ffff000000000000" + 
				"3b2eb35d8ce617650f2f5361746f7368" + 
				"693a302e372e322fc03e0300");

		//buf = ByteBuffer.wrap(testMessage);

		channel.write(buf);

		buf.position(0);
		buf.limit(buf.capacity());

		Message.encodeMessage(versionMessage, buf);

		buf.flip();

		buf.position(0);
		buf.limit(buf.capacity());

		channel.read(buf);

		buf.flip();

		Message<?> received = Message.decodeMessage(Message.PROTOCOL_VERSION, Message.MAGIC_MAIN_NETWORK , buf);

		System.out.println("Received message: " + received);
	}
}
