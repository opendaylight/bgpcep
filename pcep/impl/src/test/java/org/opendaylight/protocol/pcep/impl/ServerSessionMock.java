/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;

import java.net.SocketAddress;

import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject.Reason;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

public class ServerSessionMock extends PCEPSessionImpl {

	private final MockPCE client;

	public ServerSessionMock(final PCEPSessionListener listener, final PCEPSessionListener client) {
		super(new HashedWheelTimer(), listener, 5, new Channel() {

			@Override
			public int compareTo(final Channel arg0) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public ChannelPromise voidPromise() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelPipeline pipeline() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture newSucceededFuture() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelPromise newPromise() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelProgressivePromise newProgressivePromise() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture newFailedFuture(final Throwable cause) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ByteBufAllocator alloc() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture writeAndFlush(final Object msg, final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture writeAndFlush(final Object msg) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture write(final Object msg, final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture write(final Object msg) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture disconnect(final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture disconnect() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture deregister(final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture deregister() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture connect(final SocketAddress remoteAddress,
					final SocketAddress localAddress, final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture connect(final SocketAddress remoteAddress,
					final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture connect(final SocketAddress remoteAddress,
					final SocketAddress localAddress) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture connect(final SocketAddress remoteAddress) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture close(final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture close() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture bind(final SocketAddress localAddress) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Attribute<T> attr(final AttributeKey<T> key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Unsafe unsafe() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SocketAddress remoteAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Channel read() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Channel parent() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelMetadata metadata() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SocketAddress localAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isWritable() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isRegistered() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isOpen() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean isActive() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Channel flush() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EventLoop eventLoop() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelConfig config() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ChannelFuture closeFuture() {
				// TODO Auto-generated method stub
				return null;
			}
		}, new PCEPOpenObject(4, 9, 2), new PCEPOpenObject(4, 9, 2));
		this.client = (MockPCE) client;
	}

	@Override
	public void sendMessage(final PCEPMessage msg) {
		this.lastMessageSentAt = System.nanoTime();
		this.client.onMessage(this, msg);
	}

	@Override
	public void close() {
		this.client.onSessionTerminated(this, new PCEPCloseTermination(Reason.UNKNOWN));
	}
}
