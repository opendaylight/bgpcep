/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00PCInitiateMessageParser;
import org.opendaylight.protocol.pcep.ietf.initiated00.CrabbeInitiatedActivator;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07ErrorMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCReportMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPValidatorTest {

    private Lspa lspa;
    private Metrics metrics;
    private Iro iro;
    private Ero ero;
    private Rro rro;
    private Srp srp;
    private Lsp lsp;
    private Lsp lspSrp;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object
        .Bandwidth bandwidth;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reoptimization
        .bandwidth.object.ReoptimizationBandwidth reoptimizationBandwidth;

    private AsNumberCase eroASSubobject;
    private UnnumberedCase rroUnnumberedSub;

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;

    private static final byte[] PCRT1 = {
        (byte) 0x20, (byte) 0x0A, (byte) 0x00, (byte) 0x20,

        (byte) 0x20, (byte) 0x10, (byte) 0x00, (byte) 0x1C,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //Skip
        (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x10, //TLV Type + TLV Length
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x01, //TLV 127.0.1.1
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, //TLV LSP Id + Tunnel id
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x02, //TLV Ipv4ExtendedTunnelId 127.0.1.2
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x03, //TLV TunnelEndpointAddress 127.0.1.3
    };

    private static final byte[] PCRT2 = {
        (byte) 0x20, (byte) 0x0A, (byte) 0x00, (byte) 0x3C,

        (byte) 0x20, (byte) 0x10, (byte) 0x00, (byte) 0x1C, //(byte) 0x39,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //Skip
        (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x10, //TLV Type + TLV Length
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x01, //TLV 127.0.1.1
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, //TLV LSP Id + Tunnel id
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x02, //TLV Ipv4ExtendedTunnelId 127.0.1.2
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x03, //TLV TunnelEndpointAddress 127.0.1.3

        (byte) 0x07, (byte) 0x10, (byte) 0x00, (byte) 0x08,
        (byte) 0x20, (byte) 0x04, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x09, (byte) 0x10, (byte) 0x00, (byte) 0x14,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private static final byte[] PCRT3 = {
        (byte) 0x20, (byte) 0x0A, (byte) 0x00, (byte) 0x4C,

        (byte) 0x20, (byte) 0x10, (byte) 0x00, (byte) 0x1C, //(byte) 0x39,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //Skip
        (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x10, //TLV Type + TLV Length
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x01, //TLV 127.0.1.1
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, //TLV LSP Id + Tunnel id
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x02, //TLV Ipv4ExtendedTunnelId 127.0.1.2
        (byte) 0x7F, (byte) 0x00, (byte) 0x01, (byte) 0x03, //TLV TunnelEndpointAddress 127.0.1.3

        (byte) 0x07, (byte) 0x10, (byte) 0x00, (byte) 0x08,
        (byte) 0x20, (byte) 0x04, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x09, (byte) 0x10, (byte) 0x00, (byte) 0x14,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

        (byte) 0x05, (byte) 0x10, (byte) 0x00, (byte) 0x08,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x05, (byte) 0x20, (byte) 0x00, (byte) 0x08,
        (byte) 0x47, (byte) 0x74, (byte) 0x24, (byte) 0x00
    };

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new BaseParserExtensionActivator();
        this.act.start(this.ctx);

        this.lspa = new LspaBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setLocalProtectionDesired(false)
                .setHoldPriority(Uint8.ZERO)
                .setSetupPriority(Uint8.ZERO)
                .setExcludeAny(new AttributeFilter(Uint32.ZERO))
                .setIncludeAll(new AttributeFilter(Uint32.ZERO))
                .setIncludeAny(new AttributeFilter(Uint32.ZERO))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .lspa.object.lspa.TlvsBuilder().build())
                .build();

        this.metrics = new MetricsBuilder()
                .setMetric(new MetricBuilder()
                    .setIgnore(false)
                    .setProcessingRule(false)
                    .setComputed(false)
                    .setBound(false)
                    .setMetricType(Uint8.ONE)
                    .setValue(new Float32(new byte[4])).build())
                .build();

        this.eroASSubobject = new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber(
                Uint32.valueOf(0xFFFF)))
            .build()).build();

        this.rroUnnumberedSub = new UnnumberedCaseBuilder()
                .setUnnumbered(new UnnumberedBuilder()
                    .setRouterId(Uint32.valueOf(0x00112233L))
                    .setInterfaceId(Uint32.valueOf(0x00ff00ffL))
                    .build())
                .build();

        final IroBuilder iroBuilder = new IroBuilder()
                .setIgnore(false)
                .setProcessingRule(false);
        final List<Subobject> iroSubs = new ArrayList<>();
        iroSubs.add(new SubobjectBuilder().setSubobjectType(this.eroASSubobject).setLoose(false).build());
        iroBuilder.setSubobject(iroSubs);
        this.iro = iroBuilder.build();

        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route
            .object.ero.Subobject> eroSubs = new ArrayList<>();
        eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit
            .route.object.ero.SubobjectBuilder().setSubobjectType(this.eroASSubobject).setLoose(false).build());
        eroBuilder.setSubobject(eroSubs);
        this.ero = eroBuilder.build();

        final RroBuilder rroBuilder = new RroBuilder();
        rroBuilder.setIgnore(false);
        rroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route
            .object.rro.Subobject> rroSubs = new ArrayList<>();
        rroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported
            .route.object.rro.SubobjectBuilder().setSubobjectType(this.rroUnnumberedSub).setProtectionAvailable(false)
            .setProtectionInUse(false).build());
        rroBuilder.setSubobject(rroSubs);
        this.rro = rroBuilder.build();

        this.srp = new SrpBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .addAugmentation(new Srp1Builder().setRemove(false).build())
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                    .rev181109.srp.object.srp.TlvsBuilder().build())
                .build();

        final LspBuilder lspBuilder = new LspBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setAdministrative(false)
                .setDelegate(false)
                .setPlspId(new PlspId(Uint32.ZERO))
                .setOperational(OperationalStatus.Down)
                .setSync(false)
                .setRemove(false)
                .setTlvs(new TlvsBuilder().build())
                .addAugmentation(new Lsp1Builder().setCreate(false).build());

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
            .identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
                    .identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4AddressNoZone("127.0.1.1"));
        final LspId lspId = new LspId(Uint32.ONE);
        final TunnelId tunnelId = new TunnelId(Uint16.ONE);
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4AddressNoZone("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone("127.0.1.3"));
        final AddressFamily afiLsp = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        final LspIdentifiers identifier = new LspIdentifiersBuilder().setAddressFamily(afiLsp).setLspId(lspId)
                .setTunnelId(tunnelId).build();
        this.lspSrp = lspBuilder.build();
        this.lsp = lspBuilder.setTlvs(new TlvsBuilder().setLspIdentifiers(identifier).build()).build();

        final Ipv4Builder afi = new Ipv4Builder();
        afi.setSourceIpv4Address(new Ipv4AddressNoZone("255.255.255.255"));
        afi.setDestinationIpv4Address(new Ipv4AddressNoZone("255.255.255.255"));

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object
            .BandwidthBuilder bandwidthBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .pcep.types.rev181109.bandwidth.object.BandwidthBuilder();
        bandwidthBuilder.setIgnore(false);
        bandwidthBuilder.setProcessingRule(false);
        bandwidthBuilder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
        this.bandwidth = bandwidthBuilder.build();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reoptimization
            .bandwidth.object.ReoptimizationBandwidthBuilder reoptimizationBandwidthBuilder = new org.opendaylight.yang
                .gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reoptimization.bandwidth.object
                .ReoptimizationBandwidthBuilder();
        reoptimizationBandwidthBuilder.setIgnore(false);
        reoptimizationBandwidthBuilder.setProcessingRule(false);
        reoptimizationBandwidthBuilder.setBandwidth(
            new Bandwidth(new byte[] { (byte) 0x47, (byte) 0x74, (byte) 0x24, (byte) 0x00 }));
        this.reoptimizationBandwidth = reoptimizationBandwidthBuilder.build();
    }

    @Test
    public void testOpenMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            final ByteBuf result = Unpooled.wrappedBuffer(
                ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin"));
            final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(this.ctx.getObjectHandlerRegistry());
            final OpenMessageBuilder builder = new OpenMessageBuilder();

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object
                .OpenBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
                    .rev181109.open.object.OpenBuilder()
                    .setProcessingRule(false)
                    .setIgnore(false)
                    .setVersion(new ProtocolVersion(Uint8.ONE))
                    .setKeepalive(Uint8.valueOf(30))
                    .setDeadTimer(Uint8.valueOf(120))
                    .setSessionId(Uint8.ONE)
                    .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .open.object.open.TlvsBuilder()
                            .addAugmentation(new Tlvs1Builder()
                                .setStateful(new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).build())
                                .build())
                            .build());
            builder.setOpen(b.build());

            assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testSyncOptActivator() {
        try (SyncOptimizationsActivator a = new SyncOptimizationsActivator()) {
            a.start(this.ctx);
            a.close();
        }
    }

    @Test
    public void testUpdMsg() throws IOException, PCEPDeserializerException {
        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator()) {
            a.start(this.ctx);
            final Stateful07PCUpdateRequestMessageParser parser = new Stateful07PCUpdateRequestMessageParser(
                this.ctx.getObjectHandlerRegistry());

            final PcupdMessageBuilder builder = new PcupdMessageBuilder();

            final List<Updates> updates = new ArrayList<>();
            final PathBuilder pBuilder = new PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            updates.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            builder.setUpdates(updates);

            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin"));
            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            final List<Updates> updates1 = new ArrayList<>();
            final PathBuilder pBuilder1 = new PathBuilder();
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder1.build()).build());
            builder.setUpdates(updates1);

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin"));
            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testRptMsg() throws IOException, PCEPDeserializerException {
        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator();
                StatefulActivator b = new StatefulActivator()) {
            a.start(this.ctx);
            b.start(this.ctx);
            ByteBuf result = Unpooled.wrappedBuffer(PCRT1);

            final Stateful07PCReportMessageParser parser = new Stateful07PCReportMessageParser(
                this.ctx.getObjectHandlerRegistry());

            final PcrptMessageBuilder builder = new PcrptMessageBuilder();

            final List<Reports> reports = new ArrayList<>();
            reports.add(new ReportsBuilder().setLsp(this.lsp).build());
            builder.setReports(reports);
            final Message parseResult = parser.parseMessage(result.slice(4, result.readableBytes() - 4),
                Collections.emptyList());
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parseResult);
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(PCRT2);

            final List<Reports> reports1 = new ArrayList<>();
            reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(new org.opendaylight.yang.gen.v1.urn.opendaylight
                    .params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.reports.PathBuilder()
                        .setEro(this.ero).setLspa(this.lspa).build()).build());
            builder.setReports(reports1);

            final ByteBuf input = result.slice(4, result.readableBytes() - 4);
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
                parser.parseMessage(input, Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin"));

            final List<Reports> reports2 = new ArrayList<>();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt
                .message.pcrpt.message.reports.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight
                    .params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.reports.PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            pBuilder.setRro(this.rro);
            reports2.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            builder.setReports(reports2);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin"));

            final List<Reports> reports3 = new ArrayList<>();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt
                .message.pcrpt.message.reports.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight
                .params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.reports.PathBuilder();
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            pBuilder1.setRro(this.rro);
            reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder1.build()).build());
            builder.setReports(reports3);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(PCRT3);

            final List<Reports> reports4 = new ArrayList<>();
            reports4.add(new ReportsBuilder().setLsp(this.lsp).setPath(new org.opendaylight.yang.gen.v1.urn.opendaylight
                .params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcrpt.message.pcrpt.message.reports.PathBuilder()
                    .setEro(this.ero).setLspa(this.lspa).setBandwidth(this.bandwidth)
                    .setReoptimizationBandwidth(this.reoptimizationBandwidth).build()).build());
            builder.setReports(reports4);

            final ByteBuf input2 = result.slice(4, result.readableBytes() - 4);
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
                parser.parseMessage(input2, Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testPcinitMsg() throws IOException, PCEPDeserializerException {
        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator()) {
            a.start(this.ctx);
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/Pcinit.bin"));

            final CInitiated00PCInitiateMessageParser parser = new CInitiated00PCInitiateMessageParser(
                this.ctx.getObjectHandlerRegistry());

            final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
            final RequestsBuilder rBuilder = new RequestsBuilder();

            final List<Requests> reqs = new ArrayList<>();
            rBuilder.setSrp(this.srp);
            rBuilder.setLsp(this.lspSrp);
            rBuilder.setEro(this.ero);
            rBuilder.setLspa(this.lspa);
            rBuilder.setMetrics(Lists.newArrayList(this.metrics));
            rBuilder.setIro(this.iro);
            reqs.add(rBuilder.build());
            builder.setRequests(reqs);

            assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4,result.readableBytes() - 4), Collections.emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testErrorMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            final Stateful07ErrorMessageParser parser = new Stateful07ErrorMessageParser(
                this.ctx.getObjectHandlerRegistry());

            ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false)
                    .setType(Uint8.valueOf(19)).setValue(Uint8.ONE).build();

            List<Errors> innerErr = new ArrayList<>();
            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            PcerrMessageBuilder builder = new PcerrMessageBuilder();
            builder.setErrors(innerErr);
            final List<Srps> srps = new ArrayList<>();
            srps.add(new SrpsBuilder()
                .setSrp(new SrpBuilder()
                    .setOperationId(new SrpIdNumber(Uint32.valueOf(3)))
                    .setIgnore(false)
                    .setProcessingRule(false)
                    .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                        .rev181109.srp.object.srp.TlvsBuilder().build())
                    .build())
                .build());
            builder.setErrorType(new StatefulCaseBuilder().setStateful(new org.opendaylight.yang.gen.v1.urn.opendaylight
                .params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcerr.pcerr.message.error.type.stateful._case
                .StatefulBuilder().setSrps(srps).build()).build());

            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.1.bin"));
            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType(Uint8.valueOf(3))
                    .setValue(Uint8.ONE).build();

            innerErr = new ArrayList<>();
            builder = new PcerrMessageBuilder();

            final RpBuilder rpBuilder = new RpBuilder()
                    .setProcessingRule(true)
                    .setIgnore(false)
                    .setReoptimization(false)
                    .setBiDirectional(false)
                    .setLoose(true)
                    .setMakeBeforeBreak(false)
                    .setOrder(false)
                    .setPathKey(false)
                    .setSupplyOf(false)
                    .setFragmentation(false)
                    .setP2mp(false)
                    .setEroCompression(false)
                    .setPriority(Uint8.ONE)
                    .setRequestId(new RequestId(Uint32.TEN))
                    .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .rp.object.rp.TlvsBuilder().build())
                    .setProcessingRule(false);
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr
                .message.pcerr.message.error.type.request._case.request.Rps> rps = new ArrayList<>();
            rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr
                .message.pcerr.message.error.type.request._case.request.RpsBuilder().setRp(rpBuilder.build()).build());

            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            builder.setErrors(innerErr);
            builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.5.bin"));
            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());


            builder = new PcerrMessageBuilder();
            error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType(Uint8.valueOf(3))
                    .setValue(Uint8.ONE).build();

            innerErr = new ArrayList<>();
            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            builder.setErrors(innerErr);
            builder.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder()
                .setOpen(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open
                    .object.OpenBuilder()
                        .setDeadTimer(Uint8.ONE)
                        .setKeepalive(Uint8.ONE)
                        .setVersion(new ProtocolVersion(Uint8.ONE))
                        .setSessionId(Uint8.ZERO)
                        .setIgnore(false)
                        .setProcessingRule(false)
                        .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
                            .rev181109.open.object.open.TlvsBuilder().build())
                        .build()).build()).build());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.3.bin"));
            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
                parser.parseMessage(result.slice(4,result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testMissingLspObjectErrorInPcRptMsg() throws PCEPDeserializerException {
        final byte[] statefulMsg = {
            (byte) 0x20, (byte) 0x0B, (byte) 0x00, (byte) 0x1C,
            /* srp-object */
            (byte) 0x21, (byte) 0x10, (byte) 0x00, (byte) 0x0C,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x001,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            /* lsp-object is missing*/
            /* sr-ero-object */
            (byte) 0x07, (byte) 0x10, (byte) 0x00, (byte) 0x0C,
            /* ipv4 prefix subobject */
            (byte) 0x81, (byte) 0x08, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x00
        };

        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator();
                StatefulActivator b = new StatefulActivator()) {
            a.start(this.ctx);
            b.start(this.ctx);
            final Stateful07PCReportMessageParser parser = new Stateful07PCReportMessageParser(
                    this.ctx.getObjectHandlerRegistry());

            final PcerrMessageBuilder errMsgBuilder = new PcerrMessageBuilder();
            errMsgBuilder.setErrors(Lists.newArrayList(new ErrorsBuilder()
                    .setErrorObject(new ErrorObjectBuilder()
                        .setType(Uint8.valueOf(6))
                        .setValue(Uint8.valueOf(8))
                        .build())
                    .build()));
            final PcerrBuilder builder = new PcerrBuilder();
            builder.setPcerrMessage(errMsgBuilder.build());

            final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
            final List<Message> errors = new ArrayList<>();
            parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);
            assertFalse(errors.isEmpty());
            assertEquals(builder.build(), errors.get(0));
        }
    }

    @Test
    public void testUnexpectedRroObjectInPcUpdMsg() throws PCEPDeserializerException {
        final byte[] badUpdateMsg = {
            (byte) 0x20, (byte) 0x0b, (byte) 0x00, (byte) 0x50,
            /* SRP, LSP and ERO objects */
            (byte) 0x21, (byte) 0x12, (byte) 0x00, (byte) 0x0c,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x20, (byte) 0x10, (byte) 0x00, (byte) 0x08,
            (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x09,
            (byte) 0x07, (byte) 0x10, (byte) 0x00, (byte) 0x14,
            (byte) 0x01, (byte) 0x08, (byte) 0x05, (byte) 0x05,
            (byte) 0x05, (byte) 0x03, (byte) 0x18, (byte) 0x00,
            (byte) 0x01, (byte) 0x08, (byte) 0x08, (byte) 0x08,
            (byte) 0x08, (byte) 0x04, (byte) 0x18, (byte) 0x00,
            /* RRO object */
            (byte) 0x08, (byte) 0x10, (byte) 0x00, (byte) 0x24,
            (byte) 0x01, (byte) 0x08, (byte) 0x0a, (byte) 0x00,
            (byte) 0x00, (byte) 0x83, (byte) 0x20, (byte) 0x20,
            (byte) 0x03, (byte) 0x08, (byte) 0x01, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x08, (byte) 0x0a, (byte) 0x00,
            (byte) 0x09, (byte) 0xde, (byte) 0x20, (byte) 0x00,
            (byte) 0x03, (byte) 0x08, (byte) 0x01, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator()) {
            a.start(this.ctx);

            final Stateful07PCUpdateRequestMessageParser parser = new Stateful07PCUpdateRequestMessageParser(
                    this.ctx.getObjectHandlerRegistry());

            final PcerrMessageBuilder errMsgBuilder = new PcerrMessageBuilder();
            errMsgBuilder.setErrors(Lists.newArrayList(new ErrorsBuilder()
                    .setErrorObject(new ErrorObjectBuilder()
                        .setType(Uint8.valueOf(6))
                        .setValue(Uint8.valueOf(10))
                        .build())
                    .build()));
            final PcerrBuilder builder = new PcerrBuilder();
            builder.setPcerrMessage(errMsgBuilder.build());

            final ByteBuf buf = Unpooled.wrappedBuffer(badUpdateMsg);
            final List<Message> errors = new ArrayList<>();
            parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);
            assertFalse(errors.isEmpty());
            assertEquals(builder.build(), errors.get(0));
        }
    }
}
