/*
 * Copyright (c) 2013 Robert Varga. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.SocketChannelConfig;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;

/**
 * {@link SocketChannelConfig} augmented with TCP MD5 Signature option support.
 */
public interface MD5SocketChannelConfig extends SocketChannelConfig {
	KeyMapping getMD5SignatureKeys();

	MD5SocketChannelConfig setMD5SignatureKeys(KeyMapping keys);

	@Override
	MD5SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay);

	@Override
	MD5SocketChannelConfig setSoLinger(int soLinger);

	@Override
	MD5SocketChannelConfig setSendBufferSize(int sendBufferSize);

	@Override
	MD5SocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

	@Override
	MD5SocketChannelConfig setKeepAlive(boolean keepAlive);

	@Override
	MD5SocketChannelConfig setTrafficClass(int trafficClass);

	@Override
	MD5SocketChannelConfig setReuseAddress(boolean reuseAddress);

	@Override
	MD5SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

	@Override
	MD5SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure);

	@Override
	MD5SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

	@Override
	MD5SocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

	@Override
	MD5SocketChannelConfig setWriteSpinCount(int writeSpinCount);

	@Override
	MD5SocketChannelConfig setAllocator(ByteBufAllocator allocator);

	@Override
	MD5SocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

	@Override
	MD5SocketChannelConfig setAutoRead(boolean autoRead);

	@Override
	MD5SocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);
}
