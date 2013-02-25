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
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.raphfrk.bitcoin.bcnode.config.Config;
import com.raphfrk.bitcoin.bcnode.log.LogManager;
import com.raphfrk.bitcoin.bcnode.network.address.AddressStatus;
import com.raphfrk.bitcoin.bcnode.network.message.Message;
import com.raphfrk.bitcoin.bcnode.network.message.handler.HandshakeMessageHandler;
import com.raphfrk.bitcoin.bcnode.network.message.handler.MessageHandler;
import com.raphfrk.bitcoin.bcnode.network.protocol.Protocol;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;

public abstract class Peer<T extends Protocol<?>> {
	
	private static int localBufferSize = Config.PEER_BUFFER_SIZE.get();
	private static int connectTimeout = Config.CONNECT_TIMEOUT.get();
	private final static ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);
	
	private final SocketChannel channel;
	private final InetSocketAddress remoteAddress;
	private final boolean outgoing;
	private final InetSocketAddress addr;
	private final P2PManager manager;
	private final Runnable channelConnectRunnable = new ChannelConnectRunnable();
	private final Runnable channelReadRunnable = new ChannelReadRunnable();
	private final Runnable channelWriteRunnable = new ChannelWriteRunnable();
	private final Runnable queueRunnable = new QueueRunnable();
	private final Runnable disconnectRunnable = new DisconnectRunnable(true);
	private final Runnable disconnectRunnableWithoutRemoval = new DisconnectRunnable(false);
	private final ConnectTimeoutTask connectTimeoutTask = new ConnectTimeoutTask();
	private final HandshakeTimeoutTask handshakeTimeoutTask = new HandshakeTimeoutTask();
	private final ByteBuffer localReadBuffer;
	private ByteBuffer readBuffer;
	private final ConcurrentLinkedQueue<Message<?>> sendQueue = new ConcurrentLinkedQueue<Message<?>>();
	private final ByteBuffer localWriteBuffer;
	private ByteBuffer writeBuffer;
	private final AtomicBoolean closed;
	private int version;
	private final int magicValue;
	private final T protocol;
	private final long id;
	private final AtomicBoolean connected;
	private final AtomicBoolean writePending = new AtomicBoolean(false);
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicBoolean handshakeComplete = new AtomicBoolean(false);
	private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<Runnable>();
	
	private final SelectionKey key;
	
	public Peer(long id, SocketChannel channel, P2PManager manager) throws IOException {
		this(id, channel, null, manager);
	}
	
	public Peer(long id, InetSocketAddress addr, P2PManager manager) throws IOException {
		this(id, null, addr, manager);
	}
	
	@SuppressWarnings("unchecked")
	public Peer(long id, SocketChannel channel, InetSocketAddress addr, P2PManager manager) throws IOException {
		this.id = id;
		this.localReadBuffer = ByteBuffer.allocateDirect(localBufferSize);
		this.readBuffer = localReadBuffer;
		this.localWriteBuffer = ByteBuffer.allocateDirect(localBufferSize);
		this.writeBuffer = this.localWriteBuffer;
		this.manager = manager;
		this.magicValue = manager.getMagicValue();
		this.protocol = (T) manager.getProtocol();
		this.version = 0;
		this.closed = new AtomicBoolean(false);
		this.connected = new AtomicBoolean(false);
		this.addr = addr;
		if (channel != null) {
			this.channel = channel;
			this.outgoing = false;
		} else {
			this.channel = SocketChannel.open();
			this.outgoing = true;
		}
		this.channel.configureBlocking(false);
		if (addr != null) {
			this.remoteAddress = addr;
		} else {
			this.remoteAddress = (InetSocketAddress) this.channel.getRemoteAddress();
		}
		key = registerWithChannel();
	}
	
	protected void start() throws IOException {
		if (outgoing) {
			this.channel.connect(addr);
		}
		timer.schedule(connectTimeoutTask, connectTimeout, TimeUnit.SECONDS);
		timer.schedule(handshakeTimeoutTask, connectTimeout * 2, TimeUnit.SECONDS);
	}
	
	protected void cancelStart() {
		disconnect(false);
	}
	
	private SelectionKey registerWithChannel() throws ClosedChannelException {
		this.manager.readLockSelector();
		try {
			return this.channel.register(manager.getSelector(), SelectionKey.OP_CONNECT, this);			
		} finally {
			this.manager.readUnlockSelector();
		}
	}
	
	private void setKeyInterestOps(int set) {
		updateKeyInterestOps(set, 0);
	}
	
	public void clearKeyInterestOps() {
		updateKeyInterestOps(0, -1);
	}
	
	private void updateKeyInterestOps(int set, int clear) {
		if ((set & clear) != 0) {
			throw new IllegalArgumentException("Set and clear may not refer to the same bits");
		}
		this.manager.readLockSelector();
		try {
			if (this.key.isValid()) {
				int oldOps = this.key.interestOps();
				this.key.interestOps((oldOps | set) & (~clear));
			}
		} finally {
			this.manager.readUnlockSelector();
		}
	}
	
	public void disconnect() {
		disconnect(true);
	}
	
	private void disconnect(boolean removePeer) {
		if (!removePeer) {
			this.manager.notifyAsyncOp(disconnectRunnableWithoutRemoval);
		} else {
			this.manager.notifyAsyncOp(disconnectRunnable);
		}
	}
	
	public void notifyHandshakeComplete() {
		handshakeComplete.set(true);
	}
	
	public void setPeerProtocolVersion(int version) {
		this.version = version;
	}
	
	public void notifyKeyOpDone() {
		if (writePending.get() || !sendQueue.isEmpty()) {
			setKeyInterestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} else {
			setKeyInterestOps(SelectionKey.OP_READ);
		}
	}
	
	public void notifyOps() {
		manager.checkThread();
		if (key.isValid()) {
			if (key.isConnectable()) {
				notifyConnect();
			} else if (key.isReadable()) {
				notifyReadable();
			} else if (key.isWritable()) {
				notifyWritable();
			} else if (!sendQueue.isEmpty()) {
				notifyKeyOpDone();
			}
		}
	}
	
	public void submitAsyncTask(final Runnable task) {
		manager.notifyAsyncOp(new Runnable() {
			@Override
			public void run() {
				submitTask(task);
			}
		});
		manager.getSelector().wakeup();
	}
	
	public void submitTask(Runnable task) {
		manager.checkThread();
		taskQueue.add(task);
		if (running.compareAndSet(false, true)) {
			clearKeyInterestOps();
			manager.submitTask(queueRunnable);
		}
	}
	
	private void notifyConnect() {
		submitTask(channelConnectRunnable);
	}
	
	private void notifyReadable() {
		if (connected.get()) {
			submitTask(channelReadRunnable);
		} else {
			throw new IllegalStateException("Readable event triggereed before connect");
		}
	}
	
	private void notifyWritable() {
		if (connected.get()) {
			submitTask(channelWriteRunnable);
		} else {
			throw new IllegalStateException("Readable event triggereed before connect");
		}
	}

	/**
	 * Sends a message to this peer
	 * 
	 * @param message
	 */
	public void sendMessage(Message<?> message) {
		sendQueue.add(message);
		submitAsyncTask(channelWriteRunnable);
	}
	
	/**
	 * Gets the manager associated with this peer
	 * 
	 * @return
	 */
	public P2PManager getManager() {
		return manager;
	}
	
	/**
	 * Gets the protocol the peer is using
	 * 
	 * @return
	 */
	public T getProtocol() {
		return protocol;
	}
	
	/**
	 * Gets the version of the connection
	 * 
	 * @return
	 */
	public int getVersion() {
		return version;
	}
	
	/**
	 * Gets the local id assigned to this peer
	 * 
	 * @return
	 */
	public long getId() {
		return id;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public InetSocketAddress getLocalAddress() {
		return manager.getLocalAddress();
	}
	
	public boolean isClosed() {
		return closed.get();
	}

	private boolean closeChannel(CloseReason reason) {
		return closeChannel(reason, true);
	}
	
	private boolean closeChannel(CloseReason reason, boolean removePeer) {
		if (closed.compareAndSet(false, true)) {
			onClosed(reason);
			if (reason == CloseReason.CONNECT || reason == CloseReason.HANDSHAKE) {
				manager.getAddressStore().notify(getRemoteAddress(), AddressStatus.CONNECT_FAIL);
			}
			if (key.isValid()) {
				key.cancel();
			}
			if (removePeer) {
				manager.removePeer(this, reason.getString());
			}
			return true;
		}
		return false;
	}

	public abstract void onConnectFailure();
	
	public abstract boolean onConnect();

	public abstract void onClosed(CloseReason reason);
	
	public abstract void onReceived(Message<?> message);
	
	public static void shutdownTimer() throws InterruptedException {
		timer.shutdownNow();
		if (!timer.awaitTermination(1, TimeUnit.SECONDS)) {
			throw new IllegalStateException();
		}
	}
	
	@Override
	public String toString() {
		return new StringGenerator()
			.add("Peer", getRemoteAddress().toString())
			.done();
			
	}
	
	private class ChannelReadRunnable implements Runnable {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void run() {
			boolean eof = false;
			try {
				
				eof |= drainChannel(readBuffer);

				readBuffer.flip();
				
				boolean messageAvailable = protocol.containsFullMessage(version, magicValue, readBuffer);
				
				readBuffer.compact();

				if (readBuffer.remaining() == 0 && !messageAvailable) {
					eof |= expandBuffer() || drainChannel(readBuffer);
				}
				readBuffer.flip();
				Message message;
				do {
					message = protocol.decodeMessage(version, magicValue, readBuffer);
					if (message != null) {
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
				if (eof) {
					closeChannel(CloseReason.READ_EOF);
				} else {
					Peer.this.notifyKeyOpDone();
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
	
	private class ChannelWriteRunnable implements Runnable {

		@Override
		public void run() {
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
				writePending.set(writeBuffer.remaining() > 0);
				notifyKeyOpDone();
			}
		}

		private boolean flushMessageQueue(ByteBuffer buffer) {
			int messagesWritten = 0;
			boolean queueEmpty = true;
			Message<?> message;
			while ((message = sendQueue.peek()) != null) {
				queueEmpty = false;
				int length = message.getLength(version);
				if (buffer.remaining() < 24 + length) {
					break;
				}
				message = sendQueue.poll();
				if (protocol.encodeMessage(version, message, buffer) == Protocol.SUCCESS) {
					messagesWritten++;
				} else {
					closeChannel(CloseReason.WRITE);
					throw new IllegalStateException("Message length calculation error");
				}
			}
			return queueEmpty || (messagesWritten > 0);
		}

		private boolean attemptWrite() throws IOException {
			try {
				if (writeBuffer.remaining() > 0) {
					channel.write(writeBuffer);
					if (writeBuffer.remaining() > 0) {
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
	
	private class DisconnectRunnable implements Runnable {
		
		private final boolean removePeer;
		
		public DisconnectRunnable(boolean removePeer) {
			this.removePeer = removePeer;
		}
		
		@Override
		public void run() {
			closeChannel(CloseReason.LOCAL_DISCONNECT, removePeer);
		}
	}

	private class ChannelConnectRunnable implements Runnable {
		public void run() {
			if (connected.get()) {
				return;
			}
			try {
				boolean success = channel.finishConnect();
				if (!success) {
					closeChannel(CloseReason.CONNECT);
					return;
				}
			} catch (ClosedChannelException | SocketException e) {
				e.printStackTrace();
				closeChannel(CloseReason.CONNECT);
				return;
			} catch (IOException e) {
				e.printStackTrace();
				closeChannel(CloseReason.CONNECT);
				return;
			}
			connected.compareAndSet(false, true);
			if (onConnect()) {
				LogManager.log("Connection established to " + remoteAddress);
				Peer.this.notifyKeyOpDone();
			} else {
				closeChannel(CloseReason.CONNECT);
			}
		}
	}

	private class ConnectTimeoutTask extends TimerTask {
		@Override
		public void run() {
			submitAsyncTask(channelConnectRunnable);
		}
	}
	
	private class HandshakeTimeoutTask extends TimerTask {
		@Override
		public void run() {
			submitAsyncTask(new Runnable() {
				@Override
				public void run() {
					if (!handshakeComplete.get()) {
						closeChannel(CloseReason.HANDSHAKE);
					}
				}
			});
		}
	}
	
	private class QueueRunnable implements Runnable {

		@Override
		public final void run() {
			try {
				while (!closed.get()) {
					Runnable r;
					while ((r = taskQueue.poll()) != null  && !closed.get()) {
						r.run();
					}
					running.set(false);
					if (taskQueue.isEmpty()) {
						return;
					}
					if (closed.get() || !running.compareAndSet(false, true)) {
						return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	protected enum CloseReason {
		CONNECT, READ, READ_EOF, WRITE, KEY_REGISTRATION, HANDSHAKE, HANDLER, LOCAL_DISCONNECT;
		
		public String getString() {
			switch(this) {
				case CONNECT: return "Unable to connect to";
				case READ: return "Unable to read from";
				case READ_EOF: return "EOF readed for ";
				case WRITE: return "Unable to write to";
				case KEY_REGISTRATION: return "Unable to setup connection with";
				case HANDSHAKE: return "Unable to complete handshake with";
				case HANDLER: return "Message handler broke connection to";
				case LOCAL_DISCONNECT: return "Broke connection to";
				default: return "Connection lost with";
			}
		}
	}
	
}
