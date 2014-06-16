/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class AggregatorAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 7;

    private final ReferenceCache refCache;

    public AggregatorAttributeParser(final ReferenceCache refCache) {
        this.refCache = Preconditions.checkNotNull(refCache);
    }

    /**
     * Parse AGGREGATOR from bytes
     *
     * @param buffer byte buffer to be parsed
     * @return {@link Aggregator} BGP Aggregator
     */
    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
        final AsNumber asNumber = this.refCache.getSharedReference(new AsNumber(buffer.readUnsignedInt()));
        final Ipv4Address address = Ipv4Util.addressForBytes(ByteArray.readAllBytes(buffer));
        builder.setAggregator(new AggregatorBuilder().setAsNumber(asNumber).setNetworkAddress(address).build());
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        Aggregator aggregator = pathAttributes.getAggregator();
        if (aggregator == null) {
            return;
        }
        Preconditions.checkArgument(aggregator.getAsNumber() != null, "Missing AS number that formed the aggregate route (encoded as 2 octets).");
        byteAggregator.writeInt(aggregator.getAsNumber().getValue().shortValue());
        byteAggregator.writeBytes(Ipv4Util.bytesForAddress(aggregator.getNetworkAddress()));
    }
}
