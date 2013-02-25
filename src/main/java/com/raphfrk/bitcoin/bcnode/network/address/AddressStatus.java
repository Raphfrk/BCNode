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

import com.raphfrk.bitcoin.bcnode.config.Config;
import com.raphfrk.bitcoin.bcnode.network.elements.NetworkAddress;
import com.raphfrk.bitcoin.bcnode.util.StringGenerator;


public class AddressStatus implements Comparable<AddressStatus> {
	
	public final static int PEER_NOTIFY = 1;
	public final static int CONNECT_SUCCESS = 2;
	public final static int CONNECT_FAIL = 3;
	public final static int CONNECT_ATTEMPT = 4;
	
	public final static int NETWORK_PENALTY = Config.NETWORK_NOTIFY_PENALTY.get();
	public static int FAIL_TIMEOUT = Config.FAIL_RETRY_TIMEOUT.get();
	public static int ATTEMPT_TIMEOUT = 60;
	
	private final int lastSuccess;
	private final int lastFail;
	private final int lastNetwork;
	private final int lastAttempt;
	private final NetworkAddress addr;
	
	public AddressStatus(NetworkAddress addr) {
		this(addr, 0, 0, 0, 0);
	}
	
	protected AddressStatus(NetworkAddress addr, int lastSuccess, int lastAttempt, int lastFail, int lastNetwork) {
		this.lastSuccess = lastSuccess;
		this.lastFail = lastFail;
		this.lastNetwork = lastNetwork;
		this.lastAttempt = lastAttempt;
		this.addr = addr;
	}
	
	public AddressStatus update(int timestamp, int cause) {
		switch (cause) {
			case PEER_NOTIFY: return stampNetworkTime(timestamp);
			case CONNECT_SUCCESS: return stampSuccessTime();
			case CONNECT_FAIL: return stampFailTime();
			case CONNECT_ATTEMPT: return stampAttemptTime();
			default: return this;
		}
	}
	
	private AddressStatus stampSuccessTime() {
		return new AddressStatus(addr, getCurrentTime(), lastAttempt, lastFail, lastNetwork);
	}
	
	private AddressStatus stampAttemptTime() {
		return new AddressStatus(addr, lastSuccess, getCurrentTime(), lastFail, lastNetwork);
	}
	
	private AddressStatus stampFailTime() {
		return new AddressStatus(addr, lastSuccess, lastAttempt, getCurrentTime(), lastNetwork);
	}
	
	private AddressStatus stampNetworkTime(int lastNetwork) {
		int effectiveCurrentTime = getCurrentTime() - NETWORK_PENALTY;
		if (lastNetwork > effectiveCurrentTime) {
			lastNetwork = effectiveCurrentTime;
		}
		if (lastNetwork < 0) {
			lastNetwork = 0;
		}
		if (this.lastNetwork < lastNetwork) {
			return new AddressStatus(addr, lastSuccess, lastAttempt, lastFail, lastNetwork);
		} else {
			return this;
		}
	}
	
	/**
	 * Gets the last time a successful connection was established
	 * 
	 * @return the last success timestamp, or zero if none
	 */
	public int getLastSuccess() {
		return lastSuccess;
	}
	
	/**
	 * Gets the last time a connect attempt was made
	 * 
	 * @return the last failure timestamp, or zero if none
	 */
	public int getLastAttempt() {
		return lastAttempt;
	}
	
	/**
	 * Gets the last time a connect failure occured
	 * 
	 * @return the last failure timestamp, or zero if none
	 */
	public int getLastFail() {
		return lastFail;
	}
	
	/**
	 * Gets the last time the address was seen on the network
	 * 
	 * @return the last network observation timestamp, or zero if none
	 */
	public int getLastNetwork() {
		return lastNetwork;
	}
	
	/**
	 * Gets the address associated with this status
	 * 
	 * @return
	 */
	public NetworkAddress getAddress() {
		return addr;
	}
	
	/**
	 * Gets the current time as a timestamp
	 * 
	 * @return
	 */
	public static int getCurrentTime() {
		int time = (int) (System.currentTimeMillis() / 1000L);
		if (time < 0) {
			return 0;
		} else if (time < Integer.MAX_VALUE) {
			return (int) time;
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	private int getCompareKey() {
		int lastGood = Math.max(getLastNetwork(), getLastSuccess());
		return Math.min(lastGood, getLastFail() + FAIL_TIMEOUT);
	}

	@Override
	public int compareTo(AddressStatus o) {
		int diff = o.getCompareKey() - getCompareKey();
		if (diff != 0) {
			return diff;
		}
		diff = getLastSuccess() - o.getLastSuccess();
		if (diff != 0) {
			return diff;
		}
		diff = getLastFail() - o.getLastFail();
		if (diff != 0) {
			return diff;
		}
		diff = getLastAttempt() - o.getLastAttempt();
		if (diff != 0) {
			return diff;
		}
		diff = getLastNetwork() - o.getLastNetwork();
		if (diff != 0) {
			return diff;
		}
		return addr.compareTo(o.addr);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof AddressStatus)) {
			return false;
		} else {
			return compareTo((AddressStatus) o) == 0;
		}
	}
	
	@Override
	public String toString() {
		return new StringGenerator()
			.add("Address", addr.toString())
			.add("Last Success", getLastSuccess())
			.add("Last Attempt", getLastAttempt())
			.add("Last Failure", getLastFail())
			.add("Last Network", getLastNetwork())
			.done();	
	}

}
