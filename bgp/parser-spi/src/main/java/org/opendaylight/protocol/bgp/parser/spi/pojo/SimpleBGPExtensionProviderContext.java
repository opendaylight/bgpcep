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
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.bgp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.LabelParser;
import org.opendaylight.protocol.bgp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.bgp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.RsvpTeObjectParser;
import org.opendaylight.protocol.bgp.parser.spi.RsvpTeObjectSerializer;
import org.opendaylight.protocol.bgp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.bgp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
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
    private final int maximumCachedObjects;

    public SimpleBGPExtensionProviderContext() {
        this(DEFAULT_MAXIMUM_CACHED_OBJECTS);
    }

    public SimpleBGPExtensionProviderContext(final int maximumCachedObjects) {
        this.maximumCachedObjects = maximumCachedObjects;

        final Cache<Object, Object> cache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        this.cacheRef = new AtomicReference<Cache<Object, Object>>(cache);
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
                                            final NlriParser parser) {
        return this.getNlriRegistry().registerNlriParser(afi, safi, parser);
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
    public void registerRsvpObjectParser(final int classNum, final int cType, final RsvpTeObjectParser parser) {
        this.getRsvpRegistry().registerRsvpObjectParser(classNum, cType, parser);
    }

    @Override
    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final int cType, final RsvpTeObjectSerializer serializer) {
        this.getRsvpRegistry().registerRsvpObjectSerializer(objectClass, cType, serializer);
    }

    @Override
    public AutoCloseable registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass, final XROSubobjectSerializer serializer) {
        return this.getXROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
        return this.getXROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerRROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.SubobjectType> subobjectClass, final RROSubobjectSerializer serializer) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerEROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType> subobjectClass, final EROSubobjectSerializer serializer) {
        return this.getEROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
        return this.getEROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
        return this.getLabelHandlerRegistry().registerLabelSerializer(labelClass, serializer);
    }

    @Override
    public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
        return this.getLabelHandlerRegistry().registerLabelParser(cType, parser);
    }

    @Override
    public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
        return this.getSubsequentAddressFamilyRegistry().registerSubsequentAddressFamily(clazz, number);
    }

    @Override
    public ReferenceCache getReferenceCache() {
        return this.referenceCache;
    }

    public final synchronized int getMaximumCachedObjects() {
        return this.maximumCachedObjects;
    }

    public final synchronized void setMaximumCachedObjects(final int maximumCachedObjects) {
        Preconditions.checkArgument(maximumCachedObjects >= 0);

        Cache<Object, Object> newCache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        newCache.putAll(this.cacheRef.get().asMap());
        this.cacheRef.set(newCache);
    }
}
