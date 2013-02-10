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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.raphfrk.bitcoin.bcnode.network.message.Message;
import com.raphfrk.bitcoin.bcnode.network.streams.ExtendedDataInputStream;
import com.raphfrk.bitcoin.bcnode.network.streams.ExtendedDataOutputStream;
import com.raphfrk.bitcoin.bcnode.util.ParseUtils;

public class BCNode {
	public static void main( String[] args ) throws NoSuchAlgorithmException, NoSuchProviderException, UnknownHostException, IOException {
		Security.addProvider(new BouncyCastleProvider());
		
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
		
		ExtendedDataInputStream dis = new ExtendedDataInputStream(new ByteArrayInputStream(testMessage));
		Message m = Message.decodeMessage(0xF9BEB4D9 , dis);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ExtendedDataOutputStream dos = new ExtendedDataOutputStream(bos);
		Message.encodeMessage(m, dos);
		dos.flush();
		byte[] encoded = bos.toByteArray();
		System.out.println("Input:   " + ParseUtils.bytesToHexString(testMessage));
		System.out.println("Encoded: " + ParseUtils.bytesToHexString(encoded));		
	}
}
