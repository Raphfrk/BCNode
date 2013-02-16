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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.raphfrk.bitcoin.bcnode.config.Config;
import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.message.Message;
import com.raphfrk.bitcoin.bcnode.network.message.handler.HandshakeMessageHandler;
import com.raphfrk.bitcoin.bcnode.network.message.handler.MessageHandler;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;

public abstract class Peer<T extends Protocol<?>> {
	
	private static int localBufferSize = Config.PEER_BUFFER_SIZE.get();
	
	private final SocketChannel channel;
	private final P2PManager manager;
	private final Runnable channelConnectRunnable = new ChannelConnectRunnable();
	private final Runnable channelReadRunnable = new ChannelReadRunnable();
	private final Runnable channelWriteRunnable = new ChannelWriteRunnable();
	private final ByteBuffer localReadBuffer;
	private ByteBuffer readBuffer;
	private final ConcurrentLinkedQueue<Message<?>> sendQueue = new ConcurrentLinkedQueue<Message<?>>();
	private final ByteBuffer localWriteBuffer;
	private ByteBuffer writeBuffer;
	private final AtomicBoolean closed;
	private int version;
	private final int magicValue;
	private final T protocol;
	private final AtomicBoolean keyRegistered;
	private final AtomicInteger keyOps;
	private final AtomicBoolean reading;
	private final AtomicBoolean writing;
	private final AtomicBoolean connected;
	private SelectionKey key;
	
	public Peer(SocketChannel channel, P2PManager manager) throws IOException {
		this(channel, null, manager);
	}
	
	public Peer(InetSocketAddress addr, P2PManager manager) throws IOException {
		this(null, addr, manager);
	}
	
	@SuppressWarnings("unchecked")
	public Peer(SocketChannel channel, InetSocketAddress addr, P2PManager manager) throws IOException {
		this.keyRegistered = new AtomicBoolean(false);
		this.localReadBuffer = ByteBuffer.allocateDirect(localBufferSize);
		this.readBuffer = localReadBuffer;
		this.localWriteBuffer = ByteBuffer.allocateDirect(localBufferSize);
		this.writeBuffer = this.localWriteBuffer;
		this.manager = manager;
		this.magicValue = manager.getMagicValue();
		this.protocol = (T) manager.getProtocol();
		this.version = 0;
		this.keyOps = new AtomicInteger(0);
		this.reading = new AtomicBoolean(false);
		this.writing = new AtomicBoolean(false);
		this.closed = new AtomicBoolean(false);
		this.connected = new AtomicBoolean(false);
		if (channel != null) {
			this.channel = channel;
			this.channel.configureBlocking(false);
		} else {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);
			this.channel.connect(addr);
		}
		setKeyOp(SelectionKey.OP_CONNECT);
		this.manager.addPeer(this);
	}

	public void setPeerProtocolVersion(int version) {
		this.version = version;
	}
	
	public void notifyConnect() {
		updateKeyOps(0, SelectionKey.OP_CONNECT, false);
		manager.submitTask(channelConnectRunnable);
	}
	
	public void notifyReadable() {
		if (isKeyOpSet(SelectionKey.OP_READ) && connected.get()) {
			if (reading.compareAndSet(false, true)) {
				updateKeyOps(0, SelectionKey.OP_READ, false);
				manager.submitTask(channelReadRunnable);
			}
		}
	}
	
	public void notifyWritable() {
		notifyWritable(false);
	}
	
	public void notifyWritable(boolean force) {
		if ((isKeyOpSet(SelectionKey.OP_WRITE) || force) && connected.get()) {
			if (writing.compareAndSet(false, true)) {
				updateKeyOps(0, SelectionKey.OP_WRITE, false);
				manager.submitTask(channelWriteRunnable);
			}
		}
	}
	
	public void sendMessage(Message<?> message) {
		sendQueue.add(message);
		notifyWritable(true);
	}
	
	public P2PManager getManager() {
		return manager;
	}
	
	public T getProtocol() {
		return protocol;
	}
	
	public int getVersion() {
		return version;
	}
	
	private void clearKeyOp(int op) {
		updateKeyOps(0, op, true);
	}
	
	private void setKeyOp(int op) {
		updateKeyOps(op, 0, true);
	}
	
	private void updateKeyOps(int opSet, int opClear, boolean queue) {
		if ((opSet & opClear) != 0) {
			throw new IllegalArgumentException("The bits referred to by opSet and opClear may not overlap");
		}
		boolean success = false;
		while (!success) {
			int oldValue = keyOps.get();
			int newValue = (oldValue | (opSet)) & (~opClear);
			success = keyOps.compareAndSet(oldValue, newValue);
			if (success & oldValue != newValue) {
				if (queue) {
					manager.keyDirtyNotify(this);
				} else {
					refreshKeyOp();
				}
			}
		}
	}

	public boolean isKeyOpSet(int op) {
		return (op & keyOps.get()) != 0;
	}
	
	public void refreshKeyOp() {
		manager.checkThread();
		if (this.keyRegistered.compareAndSet(false, true)) {
			if (key != null) {
				throw new IllegalStateException("Selection key may only be set once");
			}
			try {
				key = channel.register(manager.getSelector(), keyOps.get(), this);
			} catch (ClosedChannelException e) {
				closeChannel(CloseReason.KEY_REGISTRATION);
			}	
		}
		if (key == null) {
			throw new IllegalStateException("Selection key refreshed before it was set");
		}
		key.interestOps(keyOps.get());
	}

	protected void closeChannelFinal() {
		manager.checkThread();
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
			}
		}
	}
	
	private void closeChannel(CloseReason reason) {
		if (closed.compareAndSet(false, true)) {
			onClosed(reason);
			manager.removePeer(this);
		}
	}
	
	private class ChannelReadRunnable extends ExceptionCatchRunnable {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void wrappedRun() {
			boolean eof = false;
			try {
				eof |= drainChannel(readBuffer);

				boolean messageAvailable = protocol.containsFullMessage(version, magicValue, readBuffer);
				if (readBuffer.remaining() == 0 && !messageAvailable) {
					eof |= expandBuffer() || drainChannel(readBuffer);
				}
				readBuffer.flip();
				// TODO - need to make it so these can be set by the version message
				Message message;
				do {
					message = protocol.decodeMessage(version, magicValue, readBuffer);
					if (message != null) {
						System.out.println("Message received, " + message);
						MessageHandler handler = protocol.getHandler(message.getCommand());
						if (version == 0 && !(handler instanceof HandshakeMessageHandler)) {
							closeChannel(CloseReason.HANDSHAKE);
							return;
						}
						if (handler != null) {
							if (!handler.handle(message, Peer.this)) {
								closeChannel(CloseReason.HANDLER);
							}
						}
					}
				} while (message != null);
				
				compactAndTrimBuffer();
			} catch (IOException ioe) {
				closeChannel(CloseReason.READ);
			} finally {
				if (eof && readBuffer.position() == 0) {
					closeChannel(CloseReason.READ);
				} else {
					setKeyOp(SelectionKey.OP_READ);
					reading.compareAndSet(true, false);
				}
			}
		}
		
		private boolean drainChannel(ByteBuffer buffer) throws IOException {
			int read;
			while ((read = channel.read(buffer)) > 0) {
			}
			return read == -1;
		}
		
		private boolean expandBuffer() throws IOException {
			boolean eof = false;
			if (readBuffer != localReadBuffer) {
				LogManager.log("Peer channel read buffer full due to message exceeding allowable size");
				eof = true;
			} else {
				localReadBuffer.flip();
				readBuffer = LargeBufferCache.getBuffer();
				readBuffer.put(localReadBuffer);
			}
			return eof;
		}
		
		private void compactAndTrimBuffer() {
			if (readBuffer != localReadBuffer && readBuffer.remaining() < localBufferSize) {
				localReadBuffer.clear();
				localReadBuffer.put(readBuffer);
				readBuffer = localReadBuffer;
			} else {
				readBuffer.compact();
			}
		}
		
	}
	
	public abstract boolean onConnect();

	public abstract void onClosed(CloseReason reason);
	
	public abstract void onReceived(Message<?> message);
	
	private class ChannelWriteRunnable extends ExceptionCatchRunnable {

		@Override
		public void wrappedRun() {
			try {
				flushMessageQueue(writeBuffer);
				
				writeBuffer.flip();
				
				if (!attemptWrite()) {
					return;
				}
				
				if (!flushMessageQueue(writeBuffer)) {
					if (writeBuffer == localWriteBuffer) {
						expandBuffer();

						if (!flushMessageQueue(writeBuffer)) {
							LogManager.log("Peer channel read buffer full due to message exceeding allowable size");
							closeChannel(CloseReason.WRITE);
							return;
						}

						if (!attemptWrite()) {
							return;
						}
					}
				}

				compactAndTrimBuffer();

			} catch (IOException ioe) {
				closeChannel(CloseReason.WRITE);
			} finally {
				boolean remaining = writeBuffer.remaining() > 0;
				writing.compareAndSet(true, false);
				if (remaining || !sendQueue.isEmpty()) {
					setKeyOp(SelectionKey.OP_WRITE);
				}
			}
		}

		private boolean flushMessageQueue(ByteBuffer buffer) {
			boolean success = true;
			Message<?> message;
			while ((message = sendQueue.peek()) != null) {
				int length = message.getLength(version);
				if (buffer.remaining() < 24 + length) {
					success = false;
					break;
				}
				message = sendQueue.poll();
				if (protocol.encodeMessage(version, message, buffer) == Protocol.SUCCESS) {
					System.out.println("Encoded " + message);
					success = true;
				} else {
					closeChannel(CloseReason.WRITE);
					throw new IllegalStateException("Message length calculation error");
				}
			}
			return success;
		}
		
		private boolean attemptWrite() throws IOException {
			try {
				if (writeBuffer.remaining() > 0) {
					channel.write(writeBuffer);
					if (writeBuffer.remaining() > 0) {
						setKeyOp(SelectionKey.OP_WRITE);
						return false;
					}
				}
			} finally {
				writeBuffer.compact();
			}
			return true;
		}
		
		private void expandBuffer() {
			if (writeBuffer != localWriteBuffer) {
				closeChannel(CloseReason.WRITE);
				throw new IllegalStateException("Only the localWriteBuffer can be expanded");
			}
			writeBuffer = LargeBufferCache.getBuffer();
			localWriteBuffer.flip();
			writeBuffer.put(localWriteBuffer);
		}
		
		private void compactAndTrimBuffer() {
			if (writeBuffer != localWriteBuffer && writeBuffer.remaining() < localBufferSize) {
				localWriteBuffer.clear();
				localWriteBuffer.put(writeBuffer);
				writeBuffer = localWriteBuffer;
			} else {
				writeBuffer.compact();
			}
		}
	}
	
	private class ChannelConnectRunnable extends ExceptionCatchRunnable {
		public void wrappedRun() {
			try {
				if (!channel.finishConnect()) {
					closeChannel(CloseReason.CONNECT);
				}
			} catch (IOException e) {
				closeChannel(CloseReason.CONNECT);
				return;
			}
			if (!connected.compareAndSet(false, true)) {
				throw new IllegalStateException("Peer channel reported connected more than once");
			}
			if (onConnect()) {
				updateKeyOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE, SelectionKey.OP_CONNECT, true);
			} else {
				closeChannel(CloseReason.CONNECT);
			}
			
		}
	}
	
	private abstract class ExceptionCatchRunnable implements Runnable {

		@Override
		public final void run() {
			try {
				wrappedRun();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public abstract void wrappedRun();
		
	}
	
	protected enum CloseReason {
		CONNECT, READ, WRITE, KEY_REGISTRATION, HANDSHAKE, HANDLER
	}
	
}
