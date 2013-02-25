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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.address.AddressStatus;
import com.raphfrk.bitcoin.bcnode.network.address.AddressStore;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.CryptUtils;
import com.raphfrk.bitcoin.bcnode.util.LockFile;
import com.raphfrk.bitcoin.bcnode.util.NamedThreadFactory;

public abstract class P2PManager extends Thread {
	
	private final ExecutorService workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new NamedThreadFactory("P2P Manager worker thread", false));
	/**
	 * The peerAddresses map is the canonical peer registry
	 */
	private final ConcurrentHashMap<InetSocketAddress, Peer<?>> peerAddresses = new ConcurrentHashMap<InetSocketAddress, Peer<?>>();
	private final ConcurrentHashMap<Long, Peer<?>> peerIdPeerMap = new ConcurrentHashMap<Long, Peer<?>>();
	private final Set<InetSocketAddress> connectedPeerAddresses = peerAddresses.keySet();
	
	private final ReentrantReadWriteLock selectorGuard = new ReentrantReadWriteLock();
	
	private final AddressStore addressStore;
	
	private final Selector selector;
	
	private final Protocol<?> protocol;
	
	private final File dir;
	
	private final LockFile lock;
	
	private final AtomicReference<InetSocketAddress> localAddress; 
	
	private final int maxConnections;
	private final AtomicInteger activeConnections = new AtomicInteger(0);
	
	private final Queue<Runnable> asyncOpQueue = new ConcurrentLinkedQueue<Runnable>();
	
	public P2PManager(Protocol<?> protocol, int maxConnections) throws IOException {
		this(new File("data"), protocol, maxConnections);
	}
	
	public P2PManager(File dir, Protocol<?> protocol, int maxConnections) throws IOException {
		this.dir = dir;
		dir.mkdir();
		this.lock = new LockFile(new File(dir, "lock"));
		if (!this.lock.lock()) {
			LogManager.log("Unable to establish lock for directory " + dir);
			throw new IOException("Unable to establish lock file for directory " + dir);
		}
		this.protocol = protocol;
		selector = SelectorProvider.provider().openSelector();
		this.maxConnections = maxConnections;
		this.addressStore = new AddressStore(dir, this);
		this.localAddress = new AtomicReference<InetSocketAddress>(null);
		
		attemptConnectToPeers();
		LogManager.log("Starting P2P server for " + protocol);
	}
	
	public Peer<?> connect(InetSocketAddress addr) {
		if (connectedPeerAddresses.contains(addr)) {
			LogManager.log("Ignoring connect as " + addr + " is already connected");
			Thread.dumpStack();
			return null;
		}
		try {
			getAddressStore().notify(addr, AddressStatus.CONNECT_ATTEMPT);
			long id = CryptUtils.getPseudoRandomLong();
			Peer<?> peer = protocol.getPeer(id, addr, this);
			if (peerIdPeerMap.putIfAbsent(id, peer) != null) {
				throw new IllegalStateException("Random number generator returned two equal peer ids");
			}
			if (peerAddresses.putIfAbsent(addr, peer) != null) {
				peer.cancelStart();
				LogManager.log("Ignoring connect as " + addr + " is already connected");
				if (!peerIdPeerMap.remove(id, peer)) {
					throw new IllegalStateException("Unable to remove peer from peerIdPeerMap");
				}
				return null;
			}
			activeConnections.incrementAndGet();
			LogManager.log("Attempting to connect to " + addr);
			peer.start();
			return peer;
		} catch (IOException ioe) {
			return null;
		}
	}
	
	public File getDataDirectory() {
		return dir;
	}
	
	public Protocol<?> getProtocol() {
		return protocol;
	}
	
	public int getMagicValue() {
		return protocol.getMagicValue();
	}
	
	public Peer<?> getPeer(long id) {
		return peerIdPeerMap.get(id);
	}
	
	public Peer<?> getPeer(InetSocketAddress addr) {
		return peerAddresses.get(addr);
	}
	
	public InetSocketAddress getLocalAddress() {
		return localAddress.get();
	}
	
	public AddressStore getAddressStore() {
		return addressStore;
	}
	
	public void notifyNewAddress(NetworkAddress addr) {
		InetSocketAddress socketAddr = addr.getInetSocketAddress();
		if (socketAddr != null && this.activeConnections.get() < this.maxConnections && !this.connectedPeerAddresses.contains(addr)) {
			connect(socketAddr);
		}
	}
	
	public void notifyAsyncOp(Runnable r) {
		asyncOpQueue.add(r);
		selector.wakeup();
	}
	
	protected void removePeer(Peer<?> peer, String reason) {
		boolean removed = peerAddresses.remove(peer.getRemoteAddress(), peer);
		if (!removed) {
			throw new IllegalStateException("Failed to successfully remove peer from peer set");
		}
		if (!peerIdPeerMap.remove(peer.getId(), peer)) {
			throw new IllegalStateException("Failed to successfully remove peer from id to peer set");
		}
		activeConnections.decrementAndGet();
		LogManager.log(reason + " " + peer + " (" + activeConnections.get() + ")");
		attemptConnectToPeers();
	}
	
	private void attemptConnectToPeers() {
		attemptConnectToPeers(maxConnections - activeConnections.get());
	}
	
	private void attemptConnectToPeers(int limit) {
		InetSocketAddress[] socketAddr;
		socketAddr = addressStore.getAddress(connectedPeerAddresses, limit);
		for (int i = 0; i < limit; i++) {
			if (socketAddr[i] != null) {
				connect(socketAddr[i]);
			}
		}
	}
	
	public Selector getSelector() {
		return selector;
	}
	
	public Future<?> submitTask(Runnable task) {
		return workers.submit(task);
	}
	
	protected void onShutdown() {
		addressStore.save();
	}
	
	public void checkThread() {
		if (Thread.currentThread() != this) {
			throw new IllegalStateException("Check thread called from " + Thread.currentThread() + ", checkThread() can only be called from the P2P Manager thread");
		}
	}
	
	public void readLockSelector() {
		this.selectorGuard.readLock().lock();
		this.selector.wakeup();
	}
	
	public void readUnlockSelector() {
		this.selectorGuard.readLock().unlock();
	}
	
	public void selectorGuard() {
		try {
			this.selectorGuard.writeLock().lock();
		} finally {
			this.selectorGuard.writeLock().unlock();
		}
	}
	
	public void run() {
		while (!isInterrupted()) {
			int selectedKeys;
			try {
				selectorGuard();
				selectedKeys = selector.select();
			} catch (IOException e) {
				throw new IllegalStateException("P2P Manager master thread selector thread an exception");
			}
			Runnable r;
			while ((r = asyncOpQueue.poll()) != null) {
				r.run();
			}
			if (selectedKeys > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> itr = keys.iterator();
				while (itr.hasNext()) {
					SelectionKey key = itr.next();
					itr.remove();
					Peer<?> peer = (Peer<?>) key.attachment();
					peer.notifyOps();
				}
			}
		}
		try {
			Peer.shutdownTimer();
		} catch (InterruptedException e) {
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
		onShutdown();
		lock.unlock();
	}
	
}
