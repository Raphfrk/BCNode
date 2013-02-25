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
package com.raphfrk.bitcoin.bcnode.network.address;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.network.p2p.P2PManager;

public class AddressStore {
	
	public static int MAX_ADDRESSES = 4096;
	
	private final File file;
	
	private final ConcurrentHashMap<InetSocketAddress, AddressStatus> knownAddresses = new ConcurrentHashMap<InetSocketAddress, AddressStatus>();
	
	private final P2PManager manager;
	
	private final AtomicBoolean queueSave = new AtomicBoolean();
	
	public AddressStore(File dir, P2PManager manager) {
		this.manager = manager;
		this.file = new File(dir, "peer.dat");
		load();
	}
	
	public boolean notify(InetSocketAddress addr, int cause) {
		return notify(new NetworkAddress(true, 0L, addr), cause);
	}
	
	public boolean notify(NetworkAddress addr, int cause) {
		InetSocketAddress socketAddr = addr.getInetSocketAddress();
		boolean success = false;
		boolean newAddress = false;
		while (!success) {
			newAddress = false;
			AddressStatus oldStatus = knownAddresses.get(socketAddr);
			AddressStatus newStatus = (oldStatus == null ? new AddressStatus(addr) : oldStatus).update(addr.getTimestamp(), cause);
			if (oldStatus == null) {
				newAddress = true;
				success = knownAddresses.putIfAbsent(socketAddr, newStatus) == null;
			} else if (oldStatus.equals(newStatus)) {
				return false;
			} else {
				success = knownAddresses.replace(socketAddr, oldStatus, newStatus);
			}
		}
		if (newAddress) {
			manager.notifyNewAddress(addr);
		}
		return true;
	}
	
	public boolean remove(NetworkAddress addr) {
		return knownAddresses.remove(addr.getInetSocketAddress()) != null;
	}
	
	public boolean contains(NetworkAddress addr) {
		return knownAddresses.containsKey(addr.getInetSocketAddress());
	}
	
	public AddressStatus getStatus(NetworkAddress addr) {
		return knownAddresses.get(addr.getInetSocketAddress());
	}
	
	public int addressCount() {
		return knownAddresses.size();
	}
	
	private final Object addressSync = new Object();
	
	public InetSocketAddress[] getAddress(Set<InetSocketAddress> connected, int limit) {
		synchronized (addressSync) {
			if (limit < 0) {
				return new InetSocketAddress[0];
			}
			InetSocketAddress[] addrs = new InetSocketAddress[limit];
			int[] bestTimestamp = new int[limit];

			Iterator<Entry<InetSocketAddress, AddressStatus>> itr = knownAddresses.entrySet().iterator();

			long currentTime = AddressStatus.getCurrentTime();

			int i = 0;
			int maxLoops = 5;
			while (itr.hasNext() && maxLoops > 0) {
				Entry<InetSocketAddress, AddressStatus> entry = itr.next();
				if (connected.contains(entry.getKey())) {
					continue;
				}
				AddressStatus status = entry.getValue();
				if (status.getLastFail() + AddressStatus.FAIL_TIMEOUT > currentTime) {
					continue;
				}
				if (status.getLastAttempt() + AddressStatus.ATTEMPT_TIMEOUT > currentTime) {
					continue;
				}
				if (status.getLastSuccess() > bestTimestamp[i]) {
					addrs[i] = entry.getValue().getAddress().getInetSocketAddress();
					bestTimestamp[i] = status.getLastSuccess();
				}
				if (status.getLastNetwork() > bestTimestamp[i]) {
					addrs[i] = entry.getValue().getAddress().getInetSocketAddress();
					bestTimestamp[i] = status.getLastNetwork();
				}
				i++;
				if (i == limit) {
					i = 0;
					maxLoops--;
				}
			}

			for (i = 0; i < limit; i++) {
				if (addrs[i] != null) {
					notify(addrs[i], AddressStatus.CONNECT_ATTEMPT);
				}
			}

			return addrs;
		}
	}
	
	public void save() {
		if (queueSave.compareAndSet(false, true)) {
			new SaveThread().start();
		}
	}
	
	public void load() {
		try {
			FileInputStream fis = new FileInputStream(file);
			
			FileChannel channel = fis.getChannel();
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
			
			knownAddresses.clear();
			
			boolean eof = false;
			
			while (!eof) {
				if (channel.read(buffer) == -1) {
					eof = true;
				}
				buffer.flip();
				while ((eof && buffer.remaining() > 0) || buffer.remaining() > 128) {
					byte validAddress = buffer.get();
					if (validAddress == 0) {
						break;
					}
					NetworkAddress addr = new NetworkAddress(0, buffer);
					int fail = buffer.getInt();
					int success = buffer.getInt();
					int network = buffer.getInt();
					int attempt = buffer.getInt();
					AddressStatus status = new AddressStatus(addr, success, attempt, fail, network);
					knownAddresses.put(addr.getInetSocketAddress(), status);
					LogManager.log("Loaded: " + addr.getInetSocketAddress() + " " + status);
				}
				buffer.compact();
			}
		} catch (IOException ioe) {
			LogManager.log("Read error when reading from peer.dat");
		}
	}

	private class SaveThread extends Thread {
		public void run() {
			try {
				File dir = new File("peers");	
				if (!dir.exists() && !dir.mkdirs()) {
					LogManager.log("Unable to create peer directory");
					return;
				}
				
				FileOutputStream fos = new FileOutputStream(file);
				
				FileChannel channel = fos.getChannel();
				
				ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
				
				TreeSet<AddressStatus> sorted = new TreeSet<AddressStatus>();
				
				sorted.addAll(knownAddresses.values());
				
				int addresses = 0;
				for (AddressStatus status : sorted) {
					if (!hasSpace(buffer, status)) {
						buffer.flip();
						channel.write(buffer);
						buffer.compact();
					}
					if (hasSpace(buffer, status)) {
						buffer.put((byte) 1);
						status.getAddress().put(0, buffer);
						buffer.putInt(status.getLastFail());
						buffer.putInt(status.getLastSuccess());
						buffer.putInt(status.getLastNetwork());
						buffer.putInt(status.getLastAttempt());
					}
					if (MAX_ADDRESSES <= addresses++) {
						break;
					}
				}
				buffer.flip();
				channel.write(buffer);
				buffer.compact();
				buffer.put((byte) 0);
				buffer.flip();
				channel.write(buffer);
				channel.close();
			} catch (IOException e) {
				LogManager.log("Unable to save peer.dat file");
			} finally {
				queueSave.compareAndSet(true, false);
			}
		}
		
		private boolean hasSpace(ByteBuffer buffer, AddressStatus status) {
			return buffer.remaining() >= (status.getAddress().getLength(0) + 13);
		}

	}
	
}
