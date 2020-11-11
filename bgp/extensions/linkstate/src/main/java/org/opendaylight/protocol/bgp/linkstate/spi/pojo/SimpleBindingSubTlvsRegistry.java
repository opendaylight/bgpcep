/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi.pojo;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.BackupUnnumberedParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.EroMetricParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4BackupEro;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4EroParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4PrefixSidParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6BackupEro;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6EroParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6PrefixSidParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.SIDParser;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.UnnumberedEroParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sid.tlv.BindingSubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.EroMetricCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.Ipv6PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.SidLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleBindingSubTlvsRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleBindingSubTlvsRegistry.class);
    private static final @NonNull SimpleBindingSubTlvsRegistry INSTANCE = new SimpleBindingSubTlvsRegistry();

    private final HandlerRegistry<DataContainer, BindingSubTlvsParser, BindingSubTlvsSerializer> handlers =
            new HandlerRegistry<>();

    private SimpleBindingSubTlvsRegistry() {
        final SIDParser sidParser = new SIDParser();
        handlers.registerParser(sidParser.getType(), sidParser);
        handlers.registerSerializer(SidLabelCase.class, sidParser);

        final Ipv4PrefixSidParser prefixSidParser = new Ipv4PrefixSidParser();
        handlers.registerParser(prefixSidParser.getType(), prefixSidParser);
        handlers.registerSerializer(PrefixSidCase.class, prefixSidParser);

        final Ipv6PrefixSidParser ipv6PrefixSidParser = new Ipv6PrefixSidParser();
        handlers.registerParser(ipv6PrefixSidParser.getType(), ipv6PrefixSidParser);
        handlers.registerSerializer(Ipv6PrefixSidCase.class, ipv6PrefixSidParser);

        final EroMetricParser eroMetricParser = new EroMetricParser();
        handlers.registerParser(eroMetricParser.getType(), eroMetricParser);
        handlers.registerSerializer(EroMetricCase.class, eroMetricParser);

        final Ipv4EroParser ipv4EroParser = new Ipv4EroParser();
        handlers.registerParser(ipv4EroParser.getType(), ipv4EroParser);
        handlers.registerSerializer(Ipv4EroCase.class, ipv4EroParser);

        final Ipv4BackupEro ipv4BackupEro = new Ipv4BackupEro();
        handlers.registerParser(ipv4BackupEro.getType(), ipv4BackupEro);
        handlers.registerSerializer(Ipv4EroBackupCase.class, ipv4BackupEro);

        final Ipv6EroParser ipv6EroParser = new Ipv6EroParser();
        handlers.registerParser(ipv6EroParser.getType(), ipv6EroParser);
        handlers.registerSerializer(Ipv6EroCase.class, ipv6EroParser);

        final Ipv6BackupEro ipv6BackupEro = new Ipv6BackupEro();
        handlers.registerParser(ipv6BackupEro.getType(), ipv6BackupEro);
        handlers.registerSerializer(Ipv6EroBackupCase.class, ipv6BackupEro);

        final UnnumberedEroParser unnumberedEroParser = new UnnumberedEroParser();
        handlers.registerParser(unnumberedEroParser.getType(), unnumberedEroParser);
        handlers.registerSerializer(UnnumberedInterfaceIdEroCase.class, unnumberedEroParser);

        final BackupUnnumberedParser backupUnnumberedParser = new BackupUnnumberedParser();
        handlers.registerParser(backupUnnumberedParser.getType(), backupUnnumberedParser);
        handlers.registerSerializer(UnnumberedInterfaceIdBackupEroCase.class, backupUnnumberedParser);
    }

    public static @NonNull SimpleBindingSubTlvsRegistry getInstance() {
        return INSTANCE;
    }

    public void serializeBindingSubTlvs(final List<BindingSubTlvs> bindingSubTlvs, final ByteBuf aggregator) {
        if (bindingSubTlvs != null) {
            for (final BindingSubTlvs subTlv : bindingSubTlvs) {
                final BindingSubTlv bindingSubTlv = subTlv.getBindingSubTlv();
                final BindingSubTlvsSerializer serializer = handlers.getSerializer(
                    bindingSubTlv.implementedInterface());
                if (serializer == null) {
                    LOG.info("Unknown binding sub Tlv type {}", subTlv);
                    return;
                }
                serializer.serializeSubTlv(bindingSubTlv, aggregator);
            }
        }
    }

    public List<BindingSubTlvs> parseBindingSubTlvs(final ByteBuf buffer, final ProtocolId protocolId) {
        final List<BindingSubTlvs> subTlvs = new ArrayList<>();
        if (buffer != null) {
            while (buffer.isReadable()) {
                final int type = buffer.readUnsignedShort();
                final int length = buffer.readUnsignedShort();
                final ByteBuf slice = buffer.readSlice(length);
                final BindingSubTlvsParser parser = this.handlers.getParser(type);
                if (parser == null) {
                    return null;
                }
                subTlvs.add(new BindingSubTlvsBuilder().setBindingSubTlv(parser.parseSubTlv(slice, protocolId))
                    .build());
            }
        }
        return subTlvs;
    }
}
