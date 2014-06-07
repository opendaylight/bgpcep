/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleBGPExtensionProviderContext extends SimpleBGPExtensionConsumerContext implements BGPExtensionProviderContext {
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
    private final int maximumCachedObjects;

    public SimpleBGPExtensionProviderContext() {
        this(DEFAULT_MAXIMUM_CACHED_OBJECTS);
    }

    public SimpleBGPExtensionProviderContext(final int maximumCachedObjects) {
        this.maximumCachedObjects = maximumCachedObjects;

        final Cache<Object, Object> cache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        cacheRef = new AtomicReference<Cache<Object, Object>>(cache);
    }

    @Override
    public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
        return afiReg.registerAddressFamily(clazz, number);
    }

    @Override
    public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
        return attrReg.registerAttributeParser(attributeType, parser);
    }

    @Override
    public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> attributeClass, final AttributeSerializer serializer) {
        return attrReg.registerAttributeSerializer(attributeClass, serializer);
    }

    @Override
    public AutoCloseable registerCapabilityParser(final int capabilityType, final CapabilityParser parser) {
        return capReg.registerCapabilityParser(capabilityType, parser);
    }

    @Override
    public AutoCloseable registerCapabilitySerializer(final Class<? extends CParameters> capabilityClass,
            final CapabilitySerializer serializer) {
        return capReg.registerCapabilitySerializer(capabilityClass, serializer);
    }

    @Override
    public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
        return msgReg.registerMessageParser(messageType, parser);
    }

    @Override
    public AutoCloseable registerMessageSerializer(final Class<? extends Notification> messageClass, final MessageSerializer serializer) {
        return msgReg.registerMessageSerializer(messageClass, serializer);
    }

    @Override
    public AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
            final NlriParser parser) {
        return nlriReg.registerNlriParser(afi, safi, parser);
    }

    @Override
    public AutoCloseable registerNlriSerializer(final Class<? extends DataObject> nlriClass, final NlriSerializer serializer) {
        throw new UnsupportedOperationException("NLRI serialization not implemented");
    }

    @Override
    public AutoCloseable registerParameterParser(final int parameterType, final ParameterParser parser) {
        return paramReg.registerParameterParser(parameterType, parser);
    }

    @Override
    public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
        return paramReg.registerParameterSerializer(paramClass, serializer);
    }

    @Override
    public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
        return safiReg.registerSubsequentAddressFamily(clazz, number);
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return referenceCache;
    }

    public final synchronized int getMaximumCachedObjects() {
        return maximumCachedObjects;
    }

    public final synchronized void setMaximumCachedObjects(final int maximumCachedObjects) {
        Preconditions.checkArgument(maximumCachedObjects >= 0);

        Cache<Object, Object> newCache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        newCache.putAll(cacheRef.get().asMap());
        cacheRef.set(newCache);
    }
}
