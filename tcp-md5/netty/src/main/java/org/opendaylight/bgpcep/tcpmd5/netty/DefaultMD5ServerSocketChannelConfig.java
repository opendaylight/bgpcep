/*
 * Copyright (c) 2014 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;

import java.util.Map;

import org.opendaylight.bgpcep.tcpmd5.KeyAccessFactory;
import org.opendaylight.bgpcep.tcpmd5.KeyMapping;

public class DefaultMD5ServerSocketChannelConfig extends DefaultServerSocketChannelConfig implements MD5ServerSocketChannelConfig {
	private final NettyKeyAccess keyAccess;

	public DefaultMD5ServerSocketChannelConfig(final MD5NioServerSocketChannel channel, final KeyAccessFactory keyAccessFactory) {
		super(channel, channel.javaChannel().socket());
		this.keyAccess = NettyKeyAccess.create(keyAccessFactory, channel.javaChannel());
	}

	@Override
	public KeyMapping getMD5SignatureKeys() {
		return keyAccess.getKeys();
	}

	@Override
	public MD5ServerSocketChannelConfig setMD5SignatureKeys(final KeyMapping keys) {
		keyAccess.setKeys(keys);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setBacklog(final int backlog) {
		super.setBacklog(backlog);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setReuseAddress(final boolean reuseAddress) {
		super.setReuseAddress(reuseAddress);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setReceiveBufferSize(final int receiveBufferSize) {
		super.setReceiveBufferSize(receiveBufferSize);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		super.setPerformancePreferences(connectionTime, latency, bandwidth);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setConnectTimeoutMillis(final int connectTimeoutMillis) {
		super.setConnectTimeoutMillis(connectTimeoutMillis);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setMaxMessagesPerRead(final int maxMessagesPerRead) {
		super.setMaxMessagesPerRead(maxMessagesPerRead);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setWriteSpinCount(final int writeSpinCount) {
		super.setWriteSpinCount(writeSpinCount);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setAllocator(final ByteBufAllocator allocator) {
		super.setAllocator(allocator);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setRecvByteBufAllocator(final RecvByteBufAllocator allocator) {
		super.setRecvByteBufAllocator(allocator);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setAutoRead(final boolean autoRead) {
		super.setAutoRead(autoRead);
		return this;
	}

	@Override
	public MD5ServerSocketChannelConfig setMessageSizeEstimator(final MessageSizeEstimator estimator) {
		super.setMessageSizeEstimator(estimator);
		return this;
	}

	@Override
	public Map<ChannelOption<?>, Object> getOptions() {
		final Map<ChannelOption<?>, Object> opts = super.getOptions();
		final KeyMapping keys = keyAccess.getKeys();
		if (keys != null) {
			opts.put(MD5ChannelOption.TCP_MD5SIG, keys);
		}

		return opts;
	}

	@Override
	public boolean setOptions(final Map<ChannelOption<?>, ?> options) {
		boolean setAllOptions = true;
		if (options.containsKey(MD5ChannelOption.TCP_MD5SIG)) {
			setAllOptions = setOption(MD5ChannelOption.TCP_MD5SIG, (KeyMapping)options.get(MD5ChannelOption.TCP_MD5SIG));
		}

		return super.setOptions(options) && setAllOptions;
	}

	@Override
	public <T> T getOption(final ChannelOption<T> option) {
		if (option == MD5ChannelOption.TCP_MD5SIG) {
			@SuppressWarnings("unchecked")
			final T ret = (T) keyAccess.getKeys();
			return ret;
		}

		return super.getOption(option);
	}

	@Override
	public <T> boolean setOption(final ChannelOption<T> option, final T value) {
		if (option == MD5ChannelOption.TCP_MD5SIG) {
			keyAccess.setKeys((KeyMapping) value);
			return true;
		}

		return super.setOption(option, value);
	}
}
