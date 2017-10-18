/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NextHopParserSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
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

public class SimpleBGPExtensionProviderContext extends SimpleBGPExtensionConsumerContext implements BGPExtensionProviderContext {

    public static final int DEFAULT_MAXIMUM_CACHED_OBJECTS = 100000;

    private final AtomicReference<Cache<Object, Object>> cacheRef;
    private final ReferenceCache referenceCache = new ReferenceCache() {
        @Override
        public <T> T getSharedReference(final T object) {
            final Cache<Object, Object> cache = SimpleBGPExtensionProviderContext.this.cacheRef.get();

            @SuppressWarnings("unchecked")
            final T ret = (T) cache.getIfPresent(object);
            if (ret == null) {
                cache.put(object, object);
                return object;
            }

            return ret;
        }
    };

    public SimpleBGPExtensionProviderContext() {
        this(DEFAULT_MAXIMUM_CACHED_OBJECTS);
    }

    public SimpleBGPExtensionProviderContext(final int maximumCachedObjects) {
        final Cache<Object, Object> cache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        this.cacheRef = new AtomicReference<>(cache);
    }

    @Override
    public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
        return this.getAddressFamilyRegistry().registerAddressFamily(clazz, number);
    }

    @Override
    public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
        return this.getAttributeRegistry().registerAttributeParser(attributeType, parser);
    }

    @Override
    public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> attributeClass, final AttributeSerializer serializer) {
        return this.getAttributeRegistry().registerAttributeSerializer(attributeClass, serializer);
    }

    @Override
    public AutoCloseable registerCapabilityParser(final int capabilityType, final CapabilityParser parser) {
        return this.getCapabilityRegistry().registerCapabilityParser(capabilityType, parser);
    }

    @Override
    public AutoCloseable registerCapabilitySerializer(final Class<? extends DataObject> capabilityClass, final CapabilitySerializer serializer) {
        return this.getCapabilityRegistry().registerCapabilitySerializer(capabilityClass, serializer);
    }

    @Override
    public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
        return this.getMessageRegistry().registerMessageParser(messageType, parser);
    }

    @Override
    public AutoCloseable registerMessageSerializer(final Class<? extends Notification> messageClass, final MessageSerializer serializer) {
        return this.getMessageRegistry().registerMessageSerializer(messageClass, serializer);
    }

    @Override
    public AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
        final NlriParser parser, final NextHopParserSerializer nextHopParserSerializer, final Class<? extends
            CNextHop> cNextHopClass, final Class<? extends CNextHop>... cNextHopClassList) {
        return this.getNlriRegistry().registerNlriParser(afi, safi, parser, nextHopParserSerializer, cNextHopClass, cNextHopClassList);
    }

    @Override
    public AutoCloseable registerNlriSerializer(final Class<? extends DataObject> nlriClass, final NlriSerializer serializer) {
        return this.getNlriRegistry().registerNlriSerializer(nlriClass, serializer);
    }

    @Override
    public AutoCloseable registerParameterParser(final int parameterType, final ParameterParser parser) {
        return this.getParameterRegistry().registerParameterParser(parameterType, parser);
    }

    @Override
    public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
        return this.getParameterRegistry().registerParameterSerializer(paramClass, serializer);
    }

    @Override
    public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
        return this.getSubsequentAddressFamilyRegistry().registerSubsequentAddressFamily(clazz, number);
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return this.referenceCache;
    }

    @Override
    public AutoCloseable registerExtendedCommunitySerializer(final Class<? extends ExtendedCommunity> extendedCommunityClass,
            final ExtendedCommunitySerializer serializer) {
        return this.getExtendedCommunityRegistry().registerExtendedCommunitySerializer(extendedCommunityClass, serializer);
    }

    @Override
    public AutoCloseable registerExtendedCommunityParser(final int type, final int subtype, final ExtendedCommunityParser parser) {
        return this.getExtendedCommunityRegistry().registerExtendedCommunityParser(type, subtype, parser);
    }

    @Override
    public AutoCloseable registerBgpPrefixSidTlvParser(final int tlvType, final BgpPrefixSidTlvParser parser) {
        return this.getBgpPrefixSidTlvRegistry().registerBgpPrefixSidTlvParser(tlvType, parser);
    }

    @Override
    public AutoCloseable registerBgpPrefixSidTlvSerializer(final Class<? extends BgpPrefixSidTlv> tlvClass, final BgpPrefixSidTlvSerializer serializer) {
        return this.getBgpPrefixSidTlvRegistry().registerBgpPrefixSidTlvSerializer(tlvClass, serializer);
    }
}
