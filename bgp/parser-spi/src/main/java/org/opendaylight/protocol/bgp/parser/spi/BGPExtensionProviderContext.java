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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Context for registering providers of the various types of extension points BGP provides. These are then consumed by
 * extension consumers. It also provides access to the context-wide object cache, which extension providers can use to
 * increase the in-memory efficiency when the same objects are created over and over again.
 */
public interface BGPExtensionProviderContext extends BGPExtensionConsumerContext {
    Registration registerAddressFamily(AddressFamily afi, int number);

    Registration registerSubsequentAddressFamily(SubsequentAddressFamily safi, int number);

    Registration registerAttributeParser(int attributeType, AttributeParser parser);

    Registration registerAttributeSerializer(Class<? extends DataObject> attributeClass,
            AttributeSerializer serializer);

    Registration registerCapabilityParser(int capabilityType, CapabilityParser parser);

    Registration registerCapabilitySerializer(Class<? extends DataObject> capabilityClass,
            CapabilitySerializer serializer);

    Registration registerMessageParser(int messageType, MessageParser parser);

    <T extends Notification<T> & DataObject> Registration registerMessageSerializer(Class<T> messageClass,
        MessageSerializer serializer);

    Registration registerBgpPrefixSidTlvParser(int tlvType, BgpPrefixSidTlvParser parser);

    Registration registerBgpPrefixSidTlvSerializer(Class<? extends BgpPrefixSidTlv> tlvClass,
            BgpPrefixSidTlvSerializer serializer);

    Registration registerNlriParser(AddressFamily afi, SubsequentAddressFamily safi, NlriParser parser,
        NextHopParserSerializer nextHopHandler, Class<? extends CNextHop> cnextHopClass,
        Class<? extends CNextHop>... cnextHopClassList);

    Registration registerNlriSerializer(Class<? extends DataObject> nlriClass, NlriSerializer serializer);

    Registration registerParameterParser(int parameterType, ParameterParser parser);

    Registration registerParameterSerializer(Class<? extends BgpParameters> paramClass,
            ParameterSerializer serializer);

    Registration registerExtendedCommunitySerializer(Class<? extends ExtendedCommunity> extendedCommunityClass,
        ExtendedCommunitySerializer serializer);

    Registration registerExtendedCommunityParser(int type, int subtype, ExtendedCommunityParser parser);

    /**
     * Get the context-wide cache for a particular object type.
     *
     * @return An object cache instance.
     */
    ReferenceCache getReferenceCache();
}
