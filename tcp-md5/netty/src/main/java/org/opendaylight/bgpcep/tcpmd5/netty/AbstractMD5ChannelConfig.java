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
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;

import java.io.IOException;
import java.nio.channels.NetworkChannel;
import java.util.Map;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.bgpcep.tcpmd5.MD5SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Abstract base class for implementing ChannelConfig classes. This code is abstracted
 * for reuse between the SocketChannelConfig and ServerSocketChannelConfig.
 *
 * @param <C> Base configuration type
 */
abstract class AbstractMD5ChannelConfig<C extends ChannelConfig> implements ChannelConfig {
	private static final Logger LOG = LoggerFactory.getLogger(ProxyMD5ServerSocketChannelConfig.class);
	private final NetworkChannel channel;
	protected final C config;

	protected AbstractMD5ChannelConfig(final C config, final NetworkChannel channel) {
		this.config = Preconditions.checkNotNull(config);
		this.channel = Preconditions.checkNotNull(channel);
	}

	protected final KeyMapping getKeys() {
		try {
			return channel.getOption(MD5SocketOptions.TCP_MD5SIG);
		} catch (IOException e) {
			LOG.info("Failed to get channel MD5 signature key", e);
			return null;
		}
	}

	protected final void setKeys(final KeyMapping keys) {
		try {
			channel.setOption(MD5SocketOptions.TCP_MD5SIG, keys);
		} catch (IOException e) {
			throw new ChannelException("Failed to set channel MD5 signature key", e);
		}
	}

	@Override
	public Map<ChannelOption<?>, Object> getOptions() {
		final Map<ChannelOption<?>, Object> opts = config.getOptions();
		final KeyMapping keys = getKeys();
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

		return config.setOptions(options) && setAllOptions;
	}

	@Override
	public <T> T getOption(final ChannelOption<T> option) {
		if (option == MD5ChannelOption.TCP_MD5SIG) {
			@SuppressWarnings("unchecked")
			final T ret = (T) getKeys();
			return ret;
		}

		return config.getOption(option);
	}

	@Override
	public <T> boolean setOption(final ChannelOption<T> option, final T value) {
		if (option.equals(MD5ChannelOption.TCP_MD5SIG)) {
			setKeys((KeyMapping) value);
			return true;
		}

		return config.setOption(option, value);
	}

	@Override
	public final ByteBufAllocator getAllocator() {
		return config.getAllocator();
	}

	@Override
	public final int getConnectTimeoutMillis() {
		return config.getConnectTimeoutMillis();
	}

	@Override
	public final int getMaxMessagesPerRead() {
		return config.getMaxMessagesPerRead();
	}

	@Override
	public final MessageSizeEstimator getMessageSizeEstimator() {
		return config.getMessageSizeEstimator();
	}

	@Override
	public final RecvByteBufAllocator getRecvByteBufAllocator() {
		return config.getRecvByteBufAllocator();
	}

	@Override
	public final int getWriteBufferHighWaterMark() {
		return config.getWriteBufferHighWaterMark();
	}

	@Override
	public final int getWriteBufferLowWaterMark() {
		return config.getWriteBufferLowWaterMark();
	}

	@Override
	public final int getWriteSpinCount() {
		return config.getWriteSpinCount();
	}

	@Override
	public final boolean isAutoRead() {
		return config.isAutoRead();
	}
}
