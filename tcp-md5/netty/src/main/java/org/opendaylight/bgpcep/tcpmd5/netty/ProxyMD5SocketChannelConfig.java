/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.SocketChannelConfig;

import org.opendaylight.bgpcep.tcpmd5.MD5SocketChannel;

/**
 * Proxy implementation working on top an existing SocketChannelConfig.
 */
final class ProxyMD5SocketChannelConfig extends AbstractMD5ChannelConfig<SocketChannelConfig> implements MD5SocketChannelConfig {
	public ProxyMD5SocketChannelConfig(final SocketChannelConfig config, final MD5SocketChannel channel) {
		super(config, channel);
	}

	@Override
	public boolean isTcpNoDelay() {
		return config.isTcpNoDelay();
	}

	@Override
	public int getSoLinger() {
		return config.getSoLinger();
	}

	@Override
	public int getSendBufferSize() {
		return config.getSendBufferSize();
	}

	@Override
	public int getReceiveBufferSize() {
		return config.getReceiveBufferSize();
	}

	@Override
	public boolean isKeepAlive() {
		return config.isKeepAlive();
	}

	@Override
	public int getTrafficClass() {
		return config.getTrafficClass();
	}

	@Override
	public boolean isReuseAddress() {
		return config.isReuseAddress();
	}

	@Override
	public boolean isAllowHalfClosure() {
		return config.isAllowHalfClosure();
	}

	@Override
	public ChannelConfig setWriteBufferHighWaterMark(final int writeBufferHighWaterMark) {
		config.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
		return this;
	}

	@Override
	public ChannelConfig setWriteBufferLowWaterMark(final int writeBufferLowWaterMark) {
		config.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
		return this;
	}

	@Override
	public byte[] getMD5SignatureKey() {
		return getKey();
	}

	@Override
	public MD5SocketChannelConfig setMD5SignatureKey(final byte[] key) {
		setKey(key);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setTcpNoDelay(final boolean tcpNoDelay) {
		config.setTcpNoDelay(tcpNoDelay);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setSoLinger(final int soLinger) {
		config.setSoLinger(soLinger);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setSendBufferSize(final int sendBufferSize) {
		config.setSendBufferSize(sendBufferSize);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setReceiveBufferSize(final int receiveBufferSize) {
		config.setReceiveBufferSize(receiveBufferSize);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setKeepAlive(final boolean keepAlive) {
		config.setKeepAlive(keepAlive);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setTrafficClass(final int trafficClass) {
		config.setTrafficClass(trafficClass);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setReuseAddress(final boolean reuseAddress) {
		config.setReuseAddress(reuseAddress);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		config.setPerformancePreferences(connectionTime, latency, bandwidth);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setAllowHalfClosure(final boolean allowHalfClosure) {
		config.setAllowHalfClosure(allowHalfClosure);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setConnectTimeoutMillis(final int connectTimeoutMillis) {
		config.setConnectTimeoutMillis(connectTimeoutMillis);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setMaxMessagesPerRead(final int maxMessagesPerRead) {
		config.setMaxMessagesPerRead(maxMessagesPerRead);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setWriteSpinCount(final int writeSpinCount) {
		config.setWriteSpinCount(writeSpinCount);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setAllocator(final ByteBufAllocator allocator) {
		config.setAllocator(allocator);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setRecvByteBufAllocator(final RecvByteBufAllocator allocator) {
		config.setRecvByteBufAllocator(allocator);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setAutoRead(final boolean autoRead) {
		config.setAutoRead(autoRead);
		return this;
	}

	@Override
	public MD5SocketChannelConfig setMessageSizeEstimator(final MessageSizeEstimator estimator) {
		config.setMessageSizeEstimator(estimator);
		return this;
	}
}
