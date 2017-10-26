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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.stateful._case.stateful.Srps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
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

public class PCEPValidatorTest {

    private Lspa lspa;
    private Metrics metrics;
    private Iro iro;
    private Ero ero;
    private Rro rro;
    private Srp srp;
    private Lsp lsp;
    private Lsp lspSrp;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth bandwidth;
    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidth reoptimizationBandwidth;

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
    public void setUp() throws Exception {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new BaseParserExtensionActivator();
        this.act.start(this.ctx);

        final LspaBuilder lspaBuilder = new LspaBuilder();
        lspaBuilder.setProcessingRule(false);
        lspaBuilder.setIgnore(false);
        lspaBuilder.setLocalProtectionDesired(false);
        lspaBuilder.setHoldPriority((short) 0);
        lspaBuilder.setSetupPriority((short) 0);
        lspaBuilder.setExcludeAny(new AttributeFilter(0L));
        lspaBuilder.setIncludeAll(new AttributeFilter(0L));
        lspaBuilder.setIncludeAny(new AttributeFilter(0L));
        lspaBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().build());
        this.lspa = lspaBuilder.build();

        final MetricBuilder mBuilder = new MetricBuilder();
        mBuilder.setIgnore(false);
        mBuilder.setProcessingRule(false);
        mBuilder.setComputed(false);
        mBuilder.setBound(false);
        mBuilder.setMetricType((short) 1);
        mBuilder.setValue(new Float32(new byte[4]));
        this.metrics = new MetricsBuilder().setMetric(mBuilder.build()).build();

        this.eroASSubobject = new AsNumberCaseBuilder().setAsNumber(
            new AsNumberBuilder().setAsNumber(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber(0xFFFFL)).build()).build();

        this.rroUnnumberedSub = new UnnumberedCaseBuilder().setUnnumbered(
            new UnnumberedBuilder().setRouterId(0x00112233L).setInterfaceId(0x00ff00ffL).build()).build();

        final IroBuilder iroBuilder = new IroBuilder();
        iroBuilder.setIgnore(false);
        iroBuilder.setProcessingRule(false);
        final List<Subobject> iroSubs = Lists.newArrayList();
        iroSubs.add(new SubobjectBuilder().setSubobjectType(this.eroASSubobject).setLoose(false).build());
        iroBuilder.setSubobject(iroSubs);
        this.iro = iroBuilder.build();

        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> eroSubs = Lists.newArrayList();
        eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setSubobjectType(
            this.eroASSubobject).setLoose(false).build());
        eroBuilder.setSubobject(eroSubs);
        this.ero = eroBuilder.build();

        final RroBuilder rroBuilder = new RroBuilder();
        rroBuilder.setIgnore(false);
        rroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject> rroSubs = Lists.newArrayList();
        rroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(
            this.rroUnnumberedSub).setProtectionAvailable(false).setProtectionInUse(false).build());
        rroBuilder.setSubobject(rroSubs);
        this.rro = rroBuilder.build();

        final SrpBuilder srpBuilder = new SrpBuilder();
        srpBuilder.setIgnore(false);
        srpBuilder.setProcessingRule(false);
        srpBuilder.setOperationId(new SrpIdNumber(1L));
        srpBuilder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(false).build());
        srpBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder().build());
        this.srp = srpBuilder.build();

        final LspBuilder lspBuilder = new LspBuilder();
        lspBuilder.setIgnore(false);
        lspBuilder.setProcessingRule(false);
        lspBuilder.setAdministrative(false);
        lspBuilder.setDelegate(false);
        lspBuilder.setPlspId(new PlspId(0L));
        lspBuilder.setOperational(OperationalStatus.Down);
        lspBuilder.setSync(false);
        lspBuilder.setRemove(false);
        lspBuilder.setTlvs(new TlvsBuilder().build());
        lspBuilder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(false).build());

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(new Ipv4Address("127.0.1.1"));
        final LspId lspId = new LspId(1L);
        final TunnelId tunnelId = new TunnelId(1);
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(new Ipv4Address("127.0.1.2")));
        builder.setIpv4TunnelEndpointAddress(new Ipv4Address("127.0.1.3"));
        final AddressFamily afiLsp = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        final LspIdentifiers identifier = new LspIdentifiersBuilder().setAddressFamily(afiLsp).setLspId(lspId).setTunnelId(tunnelId).build();
        this.lspSrp = lspBuilder.build();
        this.lsp = lspBuilder.setTlvs(new TlvsBuilder().setLspIdentifiers(identifier).build()).build();

        final Ipv4Builder afi = new Ipv4Builder();
        afi.setSourceIpv4Address(new Ipv4Address("255.255.255.255"));
        afi.setDestinationIpv4Address(new Ipv4Address("255.255.255.255"));

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder bandwidthBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder();
        bandwidthBuilder.setIgnore(false);
        bandwidthBuilder.setProcessingRule(false);
        bandwidthBuilder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
        this.bandwidth = bandwidthBuilder.build();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidthBuilder reoptimizationBandwidthBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidthBuilder();
        reoptimizationBandwidthBuilder.setIgnore(false);
        reoptimizationBandwidthBuilder.setProcessingRule(false);
        reoptimizationBandwidthBuilder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x47, (byte) 0x74, (byte) 0x24, (byte) 0x00 }));
        this.reoptimizationBandwidth = reoptimizationBandwidthBuilder.build();
    }

    @Test
    public void testOpenMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin"));
            final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(this.ctx.getObjectHandlerRegistry());
            final OpenMessageBuilder builder = new OpenMessageBuilder();

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
            b.setProcessingRule(false);
            b.setIgnore(false);
            b.setVersion(new ProtocolVersion((short) 1));
            b.setKeepalive((short) 30);
            b.setDeadTimer((short) 120);
            b.setSessionId((short) 1);
            final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).build();
            b.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().addAugmentation(
                Tlvs1.class, new Tlvs1Builder().setStateful(tlv1).build()).build());
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
            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.2.bin"));

            final Stateful07PCUpdateRequestMessageParser parser = new Stateful07PCUpdateRequestMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcupdMessageBuilder builder = new PcupdMessageBuilder();

            final List<Updates> updates = Lists.newArrayList();
            final PathBuilder pBuilder = new PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            updates.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            builder.setUpdates(updates);

            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCUpd.5.bin"));

            final List<Updates> updates1 = Lists.newArrayList();
            final PathBuilder pBuilder1 = new PathBuilder();
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            updates1.add(new UpdatesBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder1.build()).build());
            builder.setUpdates(updates1);

            assertEquals(new PcupdBuilder().setPcupdMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcupdBuilder().setPcupdMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testRptMsg() throws IOException, PCEPDeserializerException {
        try (CrabbeInitiatedActivator a = new CrabbeInitiatedActivator(); StatefulActivator b = new StatefulActivator()) {
            a.start(this.ctx);
            b.start(this.ctx);
            ByteBuf result = Unpooled.wrappedBuffer(PCRT1);

            final Stateful07PCReportMessageParser parser = new Stateful07PCReportMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcrptMessageBuilder builder = new PcrptMessageBuilder();

            final List<Reports> reports = Lists.newArrayList();
            reports.add(new ReportsBuilder().setLsp(this.lsp).build());
            builder.setReports(reports);
            final Message parseResult = parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList());
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parseResult);
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(PCRT2);

            final List<Reports> reports1 = Lists.newArrayList();
            reports1.add(new ReportsBuilder().setLsp(this.lsp).setPath(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder().setEro(
                    this.ero).setLspa(this.lspa).build()).build());
            builder.setReports(reports1);

            final ByteBuf input = result.slice(4, result.readableBytes() - 4);
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(input, Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.3.bin"));

            final List<Reports> reports2 = Lists.newArrayList();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder();
            pBuilder.setEro(this.ero);
            pBuilder.setLspa(this.lspa);
            pBuilder.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            pBuilder.setRro(this.rro);
            reports2.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            builder.setReports(reports2);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRpt.5.bin"));

            final List<Reports> reports3 = Lists.newArrayList();
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder pBuilder1 = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder();
            pBuilder1.setEro(this.ero);
            pBuilder1.setLspa(this.lspa);
            pBuilder1.setMetrics(Lists.newArrayList(this.metrics, this.metrics));
            pBuilder1.setRro(this.rro);
            reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder.build()).build());
            reports3.add(new ReportsBuilder().setSrp(this.srp).setLsp(this.lspSrp).setPath(pBuilder1.build()).build());
            builder.setReports(reports3);

            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcrptBuilder().setPcrptMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(PCRT3);

            final List<Reports> reports4 = Lists.newArrayList();
            reports4.add(new ReportsBuilder().setLsp(this.lsp).setPath(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder().setEro(
                    this.ero).setLspa(this.lspa).setBandwidth(this.bandwidth).setReoptimizationBandwidth(this.reoptimizationBandwidth).build()).build());
            builder.setReports(reports4);

            final ByteBuf input2 = result.slice(4, result.readableBytes() - 4);
            assertEquals(new PcrptBuilder().setPcrptMessage(builder.build()).build(), parser.parseMessage(input2, Collections.emptyList()));
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

            final CInitiated00PCInitiateMessageParser parser = new CInitiated00PCInitiateMessageParser(this.ctx.getObjectHandlerRegistry());

            final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
            final RequestsBuilder rBuilder = new RequestsBuilder();

            final List<Requests> reqs = Lists.newArrayList();
            rBuilder.setSrp(this.srp);
            rBuilder.setLsp(this.lspSrp);
            rBuilder.setEro(this.ero);
            rBuilder.setLspa(this.lspa);
            rBuilder.setMetrics(Lists.newArrayList(this.metrics));
            rBuilder.setIro(this.iro);
            reqs.add(rBuilder.build());
            builder.setRequests(reqs);

            assertEquals(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            final ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testErrorMsg() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator()) {
            a.start(this.ctx);
            final Stateful07ErrorMessageParser parser = new Stateful07ErrorMessageParser(this.ctx.getObjectHandlerRegistry());

            ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.1.bin"));
            ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 19).setValue(
                (short) 1).build();

            List<Errors> innerErr = new ArrayList<>();
            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            PcerrMessageBuilder builder = new PcerrMessageBuilder();
            builder.setErrors(innerErr);
            final List<Srps> srps = new ArrayList<>();
            srps.add(new SrpsBuilder().setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(3L)).setIgnore(false).setProcessingRule(false).setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder().build()).build()).build());
            builder.setErrorType(new StatefulCaseBuilder().setStateful(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.stateful._case.StatefulBuilder().setSrps(srps).build()).build());

            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            ByteBuf buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.5.bin"));
            error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

            innerErr = new ArrayList<>();
            builder = new PcerrMessageBuilder();

            final RpBuilder rpBuilder = new RpBuilder();
            rpBuilder.setProcessingRule(true);
            rpBuilder.setIgnore(false);
            rpBuilder.setReoptimization(false);
            rpBuilder.setBiDirectional(false);
            rpBuilder.setLoose(true);
            rpBuilder.setMakeBeforeBreak(false);
            rpBuilder.setOrder(false);
            rpBuilder.setPathKey(false);
            rpBuilder.setSupplyOf(false);
            rpBuilder.setFragmentation(false);
            rpBuilder.setP2mp(false);
            rpBuilder.setEroCompression(false);
            rpBuilder.setPriority((short) 1);
            rpBuilder.setRequestId(new RequestId(10L));
            rpBuilder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder().build());
            rpBuilder.setProcessingRule(false);
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.Rps> rps = Lists.newArrayList();
            rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder().setRp(
                rpBuilder.build()).build());

            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            builder.setErrors(innerErr);
            builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());

            result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.3.bin"));

            builder = new PcerrMessageBuilder();
            error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType((short) 3).setValue((short) 1).build();

            innerErr = new ArrayList<>();
            innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

            builder.setErrors(innerErr);
            builder.setErrorType(new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder()
                        .setDeadTimer((short) 1)
                        .setKeepalive((short) 1)
                        .setVersion(new ProtocolVersion((short) 1))
                        .setSessionId((short) 0)
                        .setIgnore(false)
                        .setProcessingRule(false)
                        .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().build())
                        .build()).build()).build());

            assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
            buf = Unpooled.buffer(result.readableBytes());
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
            assertArrayEquals(result.array(), buf.array());
        }
    }

    @Test
    public void testMissingLspObjectErrorInPcRptMsg() throws PCEPDeserializerException {
        final byte[] statefulMsg= {
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
                    .setErrorObject(new ErrorObjectBuilder().setType((short) 6).setValue((short) 8).build()).build()));
            final PcerrBuilder builder = new PcerrBuilder();
            builder.setPcerrMessage(errMsgBuilder.build());

            final ByteBuf buf = Unpooled.wrappedBuffer(statefulMsg);
            final List<Message> errors = Lists.newArrayList();
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
                    .setErrorObject(new ErrorObjectBuilder().setType((short) 6).setValue((short) 10).build()).build()));
            final PcerrBuilder builder = new PcerrBuilder();
            builder.setPcerrMessage(errMsgBuilder.build());

            final ByteBuf buf = Unpooled.wrappedBuffer(badUpdateMsg);
            final List<Message> errors = Lists.newArrayList();
            parser.parseMessage(buf.slice(4, buf.readableBytes() - 4), errors);
            assertFalse(errors.isEmpty());
            assertEquals(builder.build(), errors.get(0));
        }
    }
}
