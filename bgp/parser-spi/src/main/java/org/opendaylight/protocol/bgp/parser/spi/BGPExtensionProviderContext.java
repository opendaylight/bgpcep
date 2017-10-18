/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Context for registering providers of the various types of extension points BGP provides. These are then consumed by
 * extension consumers. It also provides access to the context-wide object cache, which extension providers can use to
 * increase the in-memory efficiency when the same objects are created over and over again.
 */
public interface BGPExtensionProviderContext extends BGPExtensionConsumerContext {
    AutoCloseable registerAddressFamily(Class<? extends AddressFamily> clazz, int number);

    AutoCloseable registerSubsequentAddressFamily(Class<? extends SubsequentAddressFamily> clazz, int number);

    AutoCloseable registerAttributeParser(int attributeType, AttributeParser parser);

    AutoCloseable registerAttributeSerializer(Class<? extends DataObject> attributeClass, AttributeSerializer serializer);

    AutoCloseable registerCapabilityParser(int capabilityType, CapabilityParser parser);

    AutoCloseable registerCapabilitySerializer(Class<? extends DataObject> capabilityClass, CapabilitySerializer serializer);

    AutoCloseable registerMessageParser(int messageType, MessageParser parser);

    AutoCloseable registerMessageSerializer(Class<? extends Notification> messageClass, MessageSerializer serializer);

    AutoCloseable registerBgpPrefixSidTlvParser(int tlvType, BgpPrefixSidTlvParser parser);

    AutoCloseable registerBgpPrefixSidTlvSerializer(Class<? extends BgpPrefixSidTlv> tlvClass, BgpPrefixSidTlvSerializer serializer);

    AutoCloseable registerNlriParser(Class<? extends AddressFamily> afi, Class<? extends SubsequentAddressFamily> safi,
        NlriParser parser, final NextHopParserSerializer nextHopHandler, final Class<? extends CNextHop> cNextHopClass,
        final Class<? extends CNextHop>... cNextHopClassList);

    AutoCloseable registerNlriSerializer(Class<? extends DataObject> nlriClass, NlriSerializer serializer);

    AutoCloseable registerParameterParser(int parameterType, ParameterParser parser);

    AutoCloseable registerParameterSerializer(Class<? extends BgpParameters> paramClass, ParameterSerializer serializer);

    AutoCloseable registerExtendedCommunitySerializer(final Class<? extends ExtendedCommunity> extendedCommunityClass,
        final ExtendedCommunitySerializer serializer);

    AutoCloseable registerExtendedCommunityParser(final int type, final int subtype, final ExtendedCommunityParser parser);

    /**
     * Get the context-wide cache for a particular object type.
     *
     * @return An object cache instance.
     */
    ReferenceCache getReferenceCache();
}
