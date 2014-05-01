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
import io.netty.channel.socket.ServerSocketChannelConfig;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.bgpcep.tcpmd5.nio.MD5ServerSocketChannel;

/**
 * Proxy implementation working on top an existing ServerSocketChannelConfig.
 */
final class ProxyMD5ServerSocketChannelConfig extends AbstractMD5ChannelConfig<ServerSocketChannelConfig> implements MD5ServerSocketChannelConfig {
	public ProxyMD5ServerSocketChannelConfig(final ServerSocketChannelConfig config, final MD5ServerSocketChannel channel) {
		super(config, channel);
	}

	@Override
	public int getBacklog() {
		return config.getBacklog();
	}

	@Override
	public boolean isReuseAddress() {
		return config.isReuseAddress();
	}

	@Override
	public int getReceiveBufferSize() {
		return config.getReceiveBufferSize();
	}

	@Override
	public ChannelConfig setWriteBufferHighWaterMark(final int writeBufferHighWaterMark) {
		config.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
		return this;
	}

	@Override
	public KeyMapping getMD5SignatureKeys() {
		return getKeys();
	}

	@Override
	public MD5ServerSocketChannelConfig setMD5SignatureKeys(final KeyMapping keys) {
		setKeys(keys);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setBacklog(final int backlog) {
		config.setBacklog(backlog);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setReuseAddress(final boolean reuseAddress) {
		config.setReuseAddress(reuseAddress);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setReceiveBufferSize(final int receiveBufferSize) {
		config.setReceiveBufferSize(receiveBufferSize);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		config.setPerformancePreferences(connectionTime, latency, bandwidth);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setConnectTimeoutMillis(final int connectTimeoutMillis) {
		config.setConnectTimeoutMillis(connectTimeoutMillis);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setMaxMessagesPerRead(final int maxMessagesPerRead) {
		config.setMaxMessagesPerRead(maxMessagesPerRead);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setWriteSpinCount(final int writeSpinCount) {
		config.setWriteSpinCount(writeSpinCount);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setAllocator(final ByteBufAllocator allocator) {
		config.setAllocator(allocator);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setRecvByteBufAllocator(final RecvByteBufAllocator allocator) {
		config.setRecvByteBufAllocator(allocator);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setAutoRead(final boolean autoRead) {
		config.setAutoRead(autoRead);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setMessageSizeEstimator(final MessageSizeEstimator estimator) {
		config.setMessageSizeEstimator(estimator);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setWriteBufferLowWaterMark(final int writeBufferLowWaterMark) {
		config.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
		return this;
	}

	@Override
	@Deprecated
	public ServerSocketChannelConfig setAutoClose(final boolean autoClose) {
		config.setAutoClose(autoClose);
		return this;
	}

	@Override
	@Deprecated
	public boolean isAutoClose() {
		return config.isAutoClose();
	}
}
