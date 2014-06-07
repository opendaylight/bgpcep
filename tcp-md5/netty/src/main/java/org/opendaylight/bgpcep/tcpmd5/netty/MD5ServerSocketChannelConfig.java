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
import io.netty.channel.socket.ServerSocketChannelConfig;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;

/**
 * {@link ServerSocketChannelConfig} augmented with TCP MD5 Signature option support.
 */
public interface MD5ServerSocketChannelConfig extends ServerSocketChannelConfig {
    KeyMapping getMD5SignatureKeys();

    MD5ServerSocketChannelConfig setMD5SignatureKeys(KeyMapping keys);

    @Override
    MD5ServerSocketChannelConfig setBacklog(int backlog);

    @Override
    MD5ServerSocketChannelConfig setReuseAddress(boolean reuseAddress);

    @Override
    MD5ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    @Override
    MD5ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    @Override
    MD5ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    MD5ServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    MD5ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    MD5ServerSocketChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    MD5ServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    MD5ServerSocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    MD5ServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);
}
