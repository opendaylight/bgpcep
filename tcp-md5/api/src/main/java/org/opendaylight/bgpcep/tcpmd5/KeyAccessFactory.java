/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.tcpmd5;

import java.nio.channels.Channel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface exposed by classes which can create KeyAccess instances for individual channels.
 */
public interface KeyAccessFactory {
    /**
     * Obtain a KeyAccess instance for a channel.
     *
     * @param channel Channel instance, may not be null
     * @return a KeyAccess instance, or null if the channel is not supported by this factory.
     * @throws NullPointerException if channel is null
     */
    @Nullable
    KeyAccess getKeyAccess(@Nonnull Channel channel);

    /**
     * Check whether a particular channel class is supported by this factory.
     *
     * @param clazz Channel class, may not be null
     * @return true if the class is supported, i.e. getKeyAccess() can be expected to succeed.
     * @throws NullPointerException if clazz is null
     */
    boolean canHandleChannelClass(@Nonnull Class<? extends Channel> clazz);
}
