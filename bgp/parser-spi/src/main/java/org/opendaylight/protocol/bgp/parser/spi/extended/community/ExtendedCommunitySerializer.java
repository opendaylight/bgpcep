/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

/**
 * The Extended Community serializer (ExtendedCommunity -> ByteBuf).
 *
 */
public interface ExtendedCommunitySerializer {

    /**
     * Serializes Extended Community value to the buffer.
     * @param extendedCommunity ExtendedCommuity to be encoded.
     * @param byteAggregator The output buffer where the extended community is written.
     */
    void serializeExtendedCommunity(ExtendedCommunity extendedCommunity, ByteBuf byteAggregator);

    /**
     * Provides a type of the extended community for which the serializer is registered.
     * @param isTransitive Transitivity of the extended community.
     * @return A type of the extended community.
     */
    int getType(boolean isTransitive);

    /**
     * Provides a sub-type of the extended community for which the serializer is registered.
     * @return A sub-type of the extended community.
     */
    int getSubType();

}
