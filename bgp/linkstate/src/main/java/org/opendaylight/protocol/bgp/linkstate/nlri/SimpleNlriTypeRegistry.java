/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final ConcurrentMap<NlriType, NlriTypeCaseParser> parsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends CRouterIdentifier>, RouterIdTlvSerializer> ridserializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends ObjectType>, NlriTypeCaseSerializer> serializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,NlriTlvObjectParser> tlvparser = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer,NlriTlvObjectSerializer> tlvserializer = new ConcurrentHashMap<>();

    private static final int BGP_ROUTER_ID = 516;
    private static final int MEMBER_AS_NUMBER = 517;

    private final List<Integer> epenodetlvlist = new ArrayList<>();
    private final List<Integer> linktlvlist = new ArrayList<>();
    private final List<Integer> nodetlvlist = new ArrayList<>();
    private final List<Integer> prefixtlvlist = new ArrayList<>();

    private static final SimpleNlriTypeRegistry SINGLETON = new SimpleNlriTypeRegistry();

    private boolean islocal = false;

    private SimpleNlriTypeRegistry () {

    }

    public static SimpleNlriTypeRegistry getInstance() {
        return SINGLETON;
    }

    public synchronized AutoCloseable registerNlriTypeSerializer(final Class<? extends ObjectType> classKey, final NlriTypeCaseSerializer serializer) {
        this.serializers.put(classKey, serializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.serializers.remove(classKey);
                }
            }
        };
    }

    public synchronized AutoCloseable registerRouterIdSerializer(final Class<? extends CRouterIdentifier> classKey, final RouterIdTlvSerializer ridserializer) {
        this.ridserializers.put(classKey, ridserializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.ridserializers.remove(classKey);
                }
            }
        };
    }

    public synchronized AutoCloseable registerNlriTlvSerializer(final Integer tlvType, final NlriType nlriType, final NlriTlvObjectSerializer tlvserializer) {
        this.tlvserializer.put(tlvType, tlvserializer);
        if (nlriType.equals(NlriType.Node)) {
            if (tlvType.equals(BGP_ROUTER_ID) || tlvType.equals(MEMBER_AS_NUMBER)) {
                if(!epenodetlvlist.contains(tlvType)){
                    epenodetlvlist.add(tlvType);
                }
            } else {
                if(!nodetlvlist.contains(tlvType)) {
                    nodetlvlist.add(tlvType);
                }
            }
        }
        else if (nlriType.equals(NlriType.Link)) {
            if (tlvType.equals(TlvUtil.MULTI_TOPOLOGY_ID)) {
                if (!linktlvlist.contains(tlvType)) {
                    linktlvlist.add(tlvType);
                }
                if (!prefixtlvlist.contains(tlvType)) {
                    prefixtlvlist.add(tlvType);
                }
            } else {
                if (!linktlvlist.contains(tlvType)) {
                    linktlvlist.add(tlvType);
                }
            }
        }
        else if (nlriType.equals(NlriType.Ipv4Prefix)) {
            if (!prefixtlvlist.contains(tlvType)) {
                prefixtlvlist.add(tlvType);
            }
        }
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.tlvserializer.remove(tlvType);
                }
            }
        };
    }

    public synchronized AutoCloseable registerNlriTlvParser(final Integer tlvType, final NlriTlvObjectParser tlvparser) {
        this.tlvparser.put(tlvType, tlvparser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.tlvparser.remove(tlvType);
                }
            }
        };

    }

    public synchronized AutoCloseable registerNlriTypeParser(final NlriType key, final NlriTypeCaseParser parser) {
        this.parsers.put(key, parser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.parsers.remove(key);
                }
            }
        };
    }

    @VisibleForTesting
    public NlriTypeCaseSerializer getSerializer(Class<? extends ObjectType> objtyp) {
        return this.serializers.get(objtyp);
    }

    @VisibleForTesting
    public NlriTypeCaseParser getParser(NlriType nlrityp) {
        return this.parsers.get(nlrityp);
    }

    public ObjectType parseNlriType (final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor, final ByteBuf restBuffer) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        Preconditions.checkNotNull(parser, "Parser for Nlri type %s not found.", type);
        ObjectType nlriObjectType = parser.parseTypeNlri(buffer, type, localdescriptor, restBuffer);
        return nlriObjectType;
    }

    public void parseTlvObject (final ByteBuf buffer, final NlriType nlriType, final NlriTlvTypeBuilderContext builderctx) throws BGPParsingException {
        final NodeDescriptorsTlvBuilderParser buildparser = (NodeDescriptorsTlvBuilderParser) this.parsers.get(nlriType);
        Preconditions.checkNotNull(buildparser, "Parser for Nlri type %s TLVs not found.", nlriType);
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf value = buffer.readSlice(length);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Parsing {} Descriptor: {}", nlriType, ByteBufUtil.hexDump(value));
            }
            final NlriTlvObjectParser tlvparser = this.tlvparser.get(type);
            Preconditions.checkNotNull(tlvparser, "TLV Parser for type: %s not found.", type);
            tlvparser.parseNlriTlvObject(value, builderctx, buildparser, nlriType);
        }
        LOG.trace("Finished parsing {} descriptors.", nlriType);
    }

    public NodeIdentifier nodeDescriptorTlvBuilder (final NlriTlvTypeBuilderContext buildercontext, final NlriType nlri) {
        final NodeDescriptorsTlvBuilderParser builder = (NodeDescriptorsTlvBuilderParser) this.parsers.get(nlri);
        Preconditions.checkNotNull(builder, "Parser for Nlri type %s TLVs not found.", nlri);
        return builder.buildNodeDescriptors(buildercontext);
    }

    public NlriType serializeNlriType (final CLinkstateDestination destination, final ByteBuf localdesc, final ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(destination.getObjectType().getImplementedInterface());
        Preconditions.checkNotNull(serializer, "Serializer for %s not found.", destination.getObjectType().getImplementedInterface().getSimpleName());
        NlriType nlriTypeVal = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);
        if (nlriTypeVal == null) {
            LOG.warn("NLRI Type value is null.");
        }
        return nlriTypeVal;
    }

    public void serializeNodeTlvObject (final ObjectType nodeCaseObject, final NlriType nlriType, final ByteBuf localdescs) {
        this.islocal = true;
        Preconditions.checkArgument(!nodetlvlist.isEmpty(), "Node TLV serializers not registered");
        Collections.sort(nodetlvlist);
        final Iterator<Integer> nodeiter = nodetlvlist.iterator();
        while (nodeiter.hasNext()) {
            Integer nodetlvtype = nodeiter.next();
            final NlriTlvObjectSerializer nodetlvserializer = this.tlvserializer.get(nodetlvtype);
            Preconditions.checkNotNull(nodetlvserializer, "Local Node TLV Serializer for type: %s not found.", nodetlvtype);
            nodetlvserializer.serializeTlvObject(nodeCaseObject, nlriType, localdescs);
        }
    }

    public void serializeRemNodeTlvObject (final ObjectType nodeCaseObject, final NlriType nlriType, final ByteBuf rmdescs) {
        this.islocal = false;
        Preconditions.checkArgument(!nodetlvlist.isEmpty(), "Node TLV serializers not registered");
        Collections.sort(nodetlvlist);
        final Iterator<Integer> remnodeiter = nodetlvlist.iterator();
        while (remnodeiter.hasNext()) {
            Integer remnodetlvtype = remnodeiter.next();
            final NlriTlvObjectSerializer nodetlvserializer = this.tlvserializer.get(remnodetlvtype);
            Preconditions.checkNotNull(nodetlvserializer, "Remote Node TLV Serializer for type: %s not found.", remnodetlvtype);
            nodetlvserializer.serializeTlvObject(nodeCaseObject, nlriType, rmdescs);
        }
    }

    public void serializeEpeNodeTlvObject (final ObjectType nodeCaseObject, final NlriType nlriType, final ByteBuf rmdescs, final boolean isLocal) {
        this.islocal = isLocal;
        Preconditions.checkArgument(!epenodetlvlist.isEmpty(), "Epe Node TLV serializers not registered");
        Collections.sort(epenodetlvlist);
        final Iterator<Integer> epenodeiter = epenodetlvlist.iterator();
        while (epenodeiter.hasNext()) {
            Integer epenodetlvtype = epenodeiter.next();
            final NlriTlvObjectSerializer nodetlvserializer = this.tlvserializer.get(epenodetlvtype);
            Preconditions.checkNotNull(nodetlvserializer, "Epe Node TLV Serializer for type: %s not found.", epenodetlvtype);
            nodetlvserializer.serializeTlvObject(nodeCaseObject, nlriType, rmdescs);
        }
    }

    public void serializeLinkTlvObject (final ObjectType linkCaseObject, final NlriType nlriType, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(!linktlvlist.isEmpty(), "Link TLV serializers not registered");
        final Iterator<Integer> linkiter = linktlvlist.iterator();
        while (linkiter.hasNext()) {
            Integer linktlvtype = linkiter.next();
            final NlriTlvObjectSerializer linktlvserializer = this.tlvserializer.get(linktlvtype);
            Preconditions.checkNotNull(linktlvserializer, "Link TLV Serializer for type: %s not found.", linktlvtype);
            linktlvserializer.serializeTlvObject(linkCaseObject, nlriType, byteAggregator);
        }
    }

    public void serializePrefixTlvObject (final ObjectType prefixCaseObject, final NlriType nlriType, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(!prefixtlvlist.isEmpty(), "Prefix TLV serializers not registered");
        Collections.sort(prefixtlvlist);
        final Iterator<Integer> prefixiter = prefixtlvlist.iterator();
        while (prefixiter.hasNext()) {
            Integer preftlvtype = prefixiter.next();
            final NlriTlvObjectSerializer preftlvserializer = this.tlvserializer.get(preftlvtype);
            Preconditions.checkNotNull(preftlvserializer, "Prefix TLV Serializer for type: %s not found.", preftlvtype);
            preftlvserializer.serializeTlvObject(prefixCaseObject, nlriType, byteAggregator);
        }
    }

    public void serializeRouterId (final CRouterIdentifier routerId, final ByteBuf buffer) {
        final RouterIdTlvSerializer preftlvserializer = this.ridserializers.get(routerId.getImplementedInterface());
        Preconditions.checkNotNull(preftlvserializer, "RouterId Serializer for %s not found.", routerId.getImplementedInterface().getSimpleName());
        preftlvserializer.serializeRouterId(routerId, buffer);
    }

    public boolean isLocal() {
        return this.islocal;
    }

}
