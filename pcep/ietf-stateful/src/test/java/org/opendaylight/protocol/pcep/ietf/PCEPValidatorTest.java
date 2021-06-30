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
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.initiated.InitiatedActivator;
import org.opendaylight.protocol.pcep.ietf.initiated.InitiatedPCInitiateMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulActivator;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulErrorMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulPCReportMessageParser;
import org.opendaylight.protocol.pcep.ietf.stateful.StatefulPCUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
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
                    .rev200720.srp.object.srp.TlvsBuilder().build())
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

        this.lspSrp = lspBuilder.build();
        this.lsp = lspBuilder.setTlvs(new TlvsBuilder()
            .setLspIdentifiers(new LspIdentifiersBuilder()
                .setAddressFamily(new Ipv4CaseBuilder()
                    .setIpv4(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                        .rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder()
                            .setIpv4TunnelSenderAddress(new Ipv4AddressNoZone("127.0.1.1"))
                            .setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4AddressNoZone("127.0.1.2")))
                            .setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone("127.0.1.3"))
                            .build())
                    .build())
                .setLspId(new LspId(Uint32.ONE))
                .setTunnelId(new TunnelId(Uint16.ONE))
                .build())
            .build())
            .build();

        final Ipv4Builder afi = new Ipv4Builder()
            .setSourceIpv4Address(new Ipv4AddressNoZone("255.255.255.255"))
            .setDestinationIpv4Address(new Ipv4AddressNoZone("255.255.255.255"));

        this.bandwidth = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
            .bandwidth.object.BandwidthBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setBandwidth(new Bandwidth(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }))
                .build();

        this.reoptimizationBandwidth = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
            .rev181109.reoptimization.bandwidth.object.ReoptimizationBandwidthBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setBandwidth(new Bandwidth(new byte[] { (byte) 0x47, (byte) 0x74, (byte) 0x24, (byte) 0x00 }))
                .build();
    }

    @Test
    public void testOpenMsg() throws IOException, PCEPDeserializerException {
        new StatefulActivator().start(this.ctx);

        final ByteBuf result = Unpooled.wrappedBuffer(
            ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin"));
        final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(this.ctx.getObjectHandlerRegistry());
        final OpenMessageBuilder builder = new OpenMessageBuilder()
            .setOpen(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open
                .object.OpenBuilder()
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
                    .build())
            .build());

        assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), List.of()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testSyncOptActivator() {
        new SyncOptimizationsActivator().start(ctx);
    }

    @Test
    public void testUpdMsg() throws IOException, PCEPDeserializerException {
        new InitiatedActivator().start(ctx);
        final StatefulPCUpdateRequestMessageParser parser = new StatefulPCUpdateRequestMessageParser(
            this.ctx.getObjectHandlerRegistry());

        final PathBuilder pBuilder = new PathBuilder()
            .setEro(this.ero)
            .setLspa(this.lspa);
        final PcupdMessageBuilder builder = new PcupdMessageBuilder()
            .setUpdates(List.of(
                new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build()));

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin"));
        assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        builder.setUpdates(List.of(
            new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build(),
            new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(new PathBuilder()
                .setEro(this.ero)
                .setLspa(this.lspa)
                .build())
            .build()));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin"));
        assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testRptMsg() throws IOException, PCEPDeserializerException {
        new InitiatedActivator().start(ctx);
        new StatefulActivator().start(ctx);
        ByteBuf result = Unpooled.wrappedBuffer(PCRT1);

        final StatefulPCReportMessageParser parser = new StatefulPCReportMessageParser(
            this.ctx.getObjectHandlerRegistry());

        final PcrptMessageBuilder builder = new PcrptMessageBuilder();

        final List<Reports> reports = new ArrayList<>();
        reports.add(new ReportsBuilder().setLsp(this.lsp).build());
        builder.setReports(reports);
        final Message parseResult = parser.parseMessage(result.slice(4, result.readableBytes() - 4),
            List.of());
        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parseResult);
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(PCRT2);

        builder.setReports(List.of(new ReportsBuilder()
            .setLsp(this.lsp)
            .setPath(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720
                .pcrpt.message.pcrpt.message.reports.PathBuilder()
                    .setEro(this.ero)
                    .setLspa(this.lspa)
                    .build())
            .build()));

        final ByteBuf input = result.slice(4, result.readableBytes() - 4);
        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
            parser.parseMessage(input, List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin"));

        final List<Reports> reports2 = new ArrayList<>();
        final var pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
            .rev200720.pcrpt.message.pcrpt.message.reports.PathBuilder();
        pBuilder.setEro(this.ero);
        pBuilder.setLspa(this.lspa);
        pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
        pBuilder.setRro(this.rro);
        reports2.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
        builder.setReports(reports2);

        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin"));

        final List<Reports> reports3 = new ArrayList<>();
        final var pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
            .rev200720.pcrpt.message.pcrpt.message.reports.PathBuilder();
        pBuilder1.setEro(this.ero);
        pBuilder1.setLspa(this.lspa);
        pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
        pBuilder1.setRro(this.rro);
        reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
        reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder1.build()).build());
        builder.setReports(reports3);

        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(PCRT3);

        final List<Reports> reports4 = new ArrayList<>();
        reports4.add(new ReportsBuilder().setLsp(this.lsp).setPath(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.reports.PathBuilder()
            .setEro(this.ero).setLspa(this.lspa).setBandwidth(this.bandwidth)
            .setReoptimizationBandwidth(this.reoptimizationBandwidth).build()).build());
        builder.setReports(reports4);

        final ByteBuf input2 = result.slice(4, result.readableBytes() - 4);
        assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(),
            parser.parseMessage(input2, List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testPcinitMsg() throws IOException, PCEPDeserializerException {
        new InitiatedActivator().start(ctx);

        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/Pcinit.bin"));

        final InitiatedPCInitiateMessageParser parser = new InitiatedPCInitiateMessageParser(
            this.ctx.getObjectHandlerRegistry());

        final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder()
            .setRequests(List.of(new RequestsBuilder()
            .setSrp(this.srp)
            .setLsp(this.lspSrp)
            .setEro(this.ero)
            .setLspa(this.lspa)
            .setMetrics(List.of(this.metrics))
            .setIro(this.iro)
            .build()));

        assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4,result.readableBytes() - 4), List.of()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testErrorMsg() throws IOException, PCEPDeserializerException {
        new StatefulActivator().start(ctx);

        final StatefulErrorMessageParser parser = new StatefulErrorMessageParser(
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
                    .rev200720.srp.object.srp.TlvsBuilder().build())
                .build())
            .build());
        builder.setErrorType(new StatefulCaseBuilder().setStateful(new org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcerr.pcerr.message.error.type.stateful._case
            .StatefulBuilder().setSrps(srps).build()).build());

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.1.bin"));
        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
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
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message
            .pcerr.message.error.type.request._case.request.Rps> rps = new ArrayList<>();
        rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message
            .pcerr.message.error.type.request._case.request.RpsBuilder().setRp(rpBuilder.build()).build());

        innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

        builder.setErrors(innerErr);
        builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.5.bin"));
        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), List.of()));
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
            parser.parseMessage(result.slice(4,result.readableBytes() - 4), List.of()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
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

        new InitiatedActivator().start(ctx);
        new StatefulActivator().start(ctx);

        final StatefulPCReportMessageParser parser = new StatefulPCReportMessageParser(
            this.ctx.getObjectHandlerRegistry());

        final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
        final List<Message> errors = new ArrayList<>();
        parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);
        assertFalse(errors.isEmpty());
        assertEquals(new PcerrBuilder()
            .setPcerrMessage(new PcerrMessageBuilder()
                .setErrors(List.of(new ErrorsBuilder()
                    .setErrorObject(new ErrorObjectBuilder()
                        .setType(Uint8.valueOf(6))
                        .setValue(Uint8.valueOf(8))
                        .build())
                    .build()))
                .build())
            .build(), errors.get(0));
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

        new InitiatedActivator().start(ctx);

        final StatefulPCUpdateRequestMessageParser parser = new StatefulPCUpdateRequestMessageParser(
            this.ctx.getObjectHandlerRegistry());

        final ByteBuf buf = Unpooled.wrappedBuffer(badUpdateMsg);
        final List<Message> errors = new ArrayList<>();
        parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);
        assertFalse(errors.isEmpty());
        assertEquals(new PcerrBuilder()
            .setPcerrMessage(new PcerrMessageBuilder()
                .setErrors(List.of(new ErrorsBuilder()
                    .setErrorObject(new ErrorObjectBuilder()
                        .setType(Uint8.valueOf(6))
                        .setValue(Uint8.valueOf(10))
                        .build())
                    .build()))
                .build())
            .build(), errors.get(0));
    }
}
