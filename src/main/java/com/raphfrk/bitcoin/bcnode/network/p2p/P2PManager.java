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
package com.raphfrk.bitcoin.bcnode.network.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.NamedThreadFactory;

public class P2PManager extends Thread {
	
	private final ExecutorService workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new NamedThreadFactory("P2P Manager worker thread", false));

	private final ConcurrentLinkedQueue<Peer<?>> closeQueue = new ConcurrentLinkedQueue<Peer<?>>();

	private final ConcurrentLinkedQueue<Peer<?>> keyDirtyQueue = new ConcurrentLinkedQueue<Peer<?>>();
	
	private final ConcurrentHashMap<Long, Peer<?>> peers = new ConcurrentHashMap<Long, Peer<?>>();
	
	private final Selector selector;
	
	private final Protocol<?> protocol;
	
	public P2PManager(Protocol<?> protocol) throws IOException {
		this.protocol = protocol;
		selector = SelectorProvider.provider().openSelector();
		LogManager.log("Starting P2P server for " + protocol);
	}
	
	public Peer<?> connect(InetSocketAddress addr) throws IOException {
		Peer<?> peer = protocol.getPeer(addr, this);
		return peer;
	}
	
	public Protocol<?> getProtocol() {
		return protocol;
	}
	
	public int getMagicValue() {
		return protocol.getMagicValue();
	}
	
	public Peer<?> getPeer(long id) {
		return peers.get(id);
	}
	
	protected void addPeer(Peer<?> peer) {
		Peer<?> oldPeer = peers.put(peer.getId(), peer);
		if (oldPeer != null) {
			throw new IllegalStateException("Peer already in peer set");
		}
		LogManager.log("Added peer to peer set, " + peer);
	}
	
	protected void removePeer(Peer<?> peer) {
		boolean removed = peers.remove(peer.getId(), peer);
		if (!removed) {
			throw new IllegalStateException("Failed to successfully remove peer from peer set");
		}
		LogManager.log("Removed peer from peer set, " + peer);
		queueForClose(peer);
		selector.wakeup();
	}
	
	protected void queueForClose(Peer<?> peer) {
		closeQueue.add(peer);
		selector.wakeup();
	}
	
	protected void keyDirtyNotify(Peer<?> peer) {
		keyDirtyQueue.add(peer);
		selector.wakeup();
	}
	
	public Selector getSelector() {
		return selector;
	}
	
	public Future<?> submitTask(Runnable task) {
		return workers.submit(task);
	}
	
	public void checkThread() {
		if (Thread.currentThread() != this) {
			throw new IllegalStateException("Check thread called from " + Thread.currentThread() + ", checkThread() can only be called from the P2P Manager thread");
		}
	}
	
	public void run() {
		while (!isInterrupted()) {
			int selectedKeys;
			try {
				selectedKeys = selector.select();
			} catch (IOException e) {
				throw new IllegalStateException("P2P Manager master thread selector thread an exception");
			}
			if (selectedKeys > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> itr = keys.iterator();
				while (itr.hasNext()) {
					SelectionKey key = itr.next();
					itr.remove();
					Peer<?> peer = (Peer<?>) key.attachment();
					try {
						if (key.isConnectable()) {
							peer.notifyConnect();
						}
						if (key.isReadable()) {
							peer.notifyReadable();
							continue;
						}
						if (key.isWritable()) {
							peer.notifyWritable();
						}
					} catch (CancelledKeyException c) {
						System.out.println(c.getMessage());
						c.printStackTrace();
					}
				}
			}
			Peer<?> peer;
			while ((peer = keyDirtyQueue.poll()) != null) {
				peer.refreshKeyOp();
			}
			while ((peer = closeQueue.poll()) != null) {
				peer.closeChannelFinal();
			}
		}
		workers.shutdown();
		try {
			int delay = 1;
			while (!workers.awaitTermination(delay, TimeUnit.SECONDS)) {
				LogManager.log("Worker executor service shutdown failure");
				delay *= 2;
			}
		} catch (InterruptedException e) {
		}
	}
	
}
