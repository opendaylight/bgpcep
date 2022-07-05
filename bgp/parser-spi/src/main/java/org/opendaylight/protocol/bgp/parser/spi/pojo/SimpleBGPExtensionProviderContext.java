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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.CNextHop;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class SimpleBGPExtensionProviderContext extends SimpleBGPExtensionConsumerContext
        implements BGPExtensionProviderContext {

    public static final int DEFAULT_MAXIMUM_CACHED_OBJECTS = 100000;

    private final AtomicReference<Cache<Object, Object>> cacheRef;
    private final ReferenceCache referenceCache = new ReferenceCache() {
        @Override
        public <T> T getSharedReference(final T object) {
            final Cache<Object, Object> cache = cacheRef.get();

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
        cacheRef = new AtomicReference<>(cache);
    }

    @Override
    public Registration registerAddressFamily(final AddressFamily afi, final int number) {
        return this.getAddressFamilyRegistry().registerAddressFamily(afi, number);
    }

    @Override
    public Registration registerAttributeParser(final int attributeType, final AttributeParser parser) {
        return this.getAttributeRegistry().registerAttributeParser(attributeType, parser);
    }

    @Override
    public Registration registerAttributeSerializer(final Class<? extends DataObject> attributeClass,
            final AttributeSerializer serializer) {
        return this.getAttributeRegistry().registerAttributeSerializer(attributeClass, serializer);
    }

    @Override
    public Registration registerCapabilityParser(final int capabilityType, final CapabilityParser parser) {
        return this.getCapabilityRegistry().registerCapabilityParser(capabilityType, parser);
    }

    @Override
    public Registration registerCapabilitySerializer(final Class<? extends DataObject> capabilityClass,
            final CapabilitySerializer serializer) {
        return this.getCapabilityRegistry().registerCapabilitySerializer(capabilityClass, serializer);
    }

    @Override
    public Registration registerMessageParser(final int messageType, final MessageParser parser) {
        return this.getMessageRegistry().registerMessageParser(messageType, parser);
    }

    @Override
    public <T extends Notification<T> & DataObject> Registration registerMessageSerializer(final Class<T> messageClass,
            final MessageSerializer serializer) {
        return this.getMessageRegistry().registerMessageSerializer(messageClass, serializer);
    }

    @Override
    public Registration registerNlriParser(final AddressFamily afi, final SubsequentAddressFamily safi,
            final NlriParser parser, final NextHopParserSerializer nextHopParserSerializer,
            final Class<? extends CNextHop> cnextHopClass, final Class<? extends CNextHop>... cnextHopClassList) {
        return this.getNlriRegistry().registerNlriParser(afi, safi, parser, nextHopParserSerializer, cnextHopClass,
            cnextHopClassList);
    }

    @Override
    public Registration registerNlriSerializer(final Class<? extends DataObject> nlriClass,
            final NlriSerializer serializer) {
        return this.getNlriRegistry().registerNlriSerializer(nlriClass, serializer);
    }

    @Override
    public Registration registerParameterParser(final int parameterType, final ParameterParser parser) {
        return this.getParameterRegistry().registerParameterParser(parameterType, parser);
    }

    @Override
    public Registration registerParameterSerializer(final Class<? extends BgpParameters> paramClass,
            final ParameterSerializer serializer) {
        return this.getParameterRegistry().registerParameterSerializer(paramClass, serializer);
    }

    @Override
    public Registration registerSubsequentAddressFamily(final SubsequentAddressFamily safi, final int number) {
        return this.getSubsequentAddressFamilyRegistry().registerSubsequentAddressFamily(safi, number);
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    @Override
    public Registration registerExtendedCommunitySerializer(
            final Class<? extends ExtendedCommunity> extendedCommunityClass,
            final ExtendedCommunitySerializer serializer) {
        return this.getExtendedCommunityRegistry().registerExtendedCommunitySerializer(extendedCommunityClass,
            serializer);
    }

    @Override
    public Registration registerExtendedCommunityParser(final int type, final int subtype,
            final ExtendedCommunityParser parser) {
        return this.getExtendedCommunityRegistry().registerExtendedCommunityParser(type, subtype, parser);
    }

    @Override
    public Registration registerBgpPrefixSidTlvParser(final int tlvType, final BgpPrefixSidTlvParser parser) {
        return this.getBgpPrefixSidTlvRegistry().registerBgpPrefixSidTlvParser(tlvType, parser);
    }

    @Override
    public Registration registerBgpPrefixSidTlvSerializer(final Class<? extends BgpPrefixSidTlv> tlvClass,
            final BgpPrefixSidTlvSerializer serializer) {
        return this.getBgpPrefixSidTlvRegistry().registerBgpPrefixSidTlvSerializer(tlvClass, serializer);
    }
}
