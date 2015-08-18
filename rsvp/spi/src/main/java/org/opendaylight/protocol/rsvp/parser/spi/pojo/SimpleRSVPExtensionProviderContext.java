/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

public class SimpleRSVPExtensionProviderContext extends SimpleRSVPExtensionConsumerContext implements RSVPExtensionProviderContext {

    public static final int DEFAULT_MAXIMUM_CACHED_OBJECTS = 100000;

    private final AtomicReference<Cache<Object, Object>> cacheRef;
    private final ReferenceCache referenceCache = new ReferenceCache() {
        @Override
        public <T> T getSharedReference(final T object) {
            final Cache<Object, Object> cache = SimpleRSVPExtensionProviderContext.this.cacheRef.get();

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

    public SimpleRSVPExtensionProviderContext() {
        this(DEFAULT_MAXIMUM_CACHED_OBJECTS);
    }

    public SimpleRSVPExtensionProviderContext(final int maximumCachedObjects) {
        this.maximumCachedObjects = maximumCachedObjects;

        final Cache<Object, Object> cache = CacheBuilder.newBuilder().maximumSize(maximumCachedObjects).build();
        this.cacheRef = new AtomicReference<Cache<Object,Object>>(cache);
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

    @Override
    public ReferenceCache getReferenceCache() {
        return this.referenceCache;
    }


    @Override
    public void registerRsvpObjectParser(final int classNum, final int cType, final RSVPTeObjectParser parser) {
        this.getRsvpRegistry().registerRsvpObjectParser(classNum, cType, parser);
    }

    @Override
    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final RSVPTeObjectSerializer serializer) {
        this.getRsvpRegistry().registerRsvpObjectSerializer(objectClass, serializer);
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
    public AutoCloseable registerRROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType> subobjectClass, final RROSubobjectSerializer serializer) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectSerializer(subobjectClass, serializer);
    }

    @Override
    public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
        return this.getRROSubobjectHandlerRegistry().registerSubobjectParser(subobjectType, parser);
    }

    @Override
    public AutoCloseable registerEROSubobjectSerializer(final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType> subobjectClass, final EROSubobjectSerializer serializer) {
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
}
