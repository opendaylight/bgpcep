package parser.spi.pojo;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import parser.spi.EROSubobjectParser;
import parser.spi.EROSubobjectSerializer;
import parser.spi.LabelParser;
import parser.spi.LabelSerializer;
import parser.spi.RROSubobjectParser;
import parser.spi.RROSubobjectSerializer;
import parser.spi.RSVPExtensionProviderContext;
import parser.spi.RSVPTeObjectParser;
import parser.spi.RSVPTeObjectSerializer;
import parser.spi.XROSubobjectParser;
import parser.spi.XROSubobjectSerializer;

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
        this.cacheRef = new AtomicReference<Cache<Object, Object>>(cache);
    }

    @Override
    public void registerRsvpObjectParser(final int classNum, final int cType, final RSVPTeObjectParser parser) {
        this.getRsvpRegistry().registerRsvpObjectParser(classNum, cType, parser);
    }

    @Override
    public void registerRsvpObjectSerializer(final Class<? extends RsvpTeObject> objectClass, final int cType, final RSVPTeObjectSerializer serializer) {
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
}
