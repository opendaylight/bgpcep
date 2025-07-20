/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.object.PCEPAssociationIPv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPAssociationIPv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExistingBandwidthObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPMonitoringObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPOverloadObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPccIdReqIPv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPccIdReqIPv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPceIdIPv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPceIdIPv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPProcTimeObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.parser.object.bnc.BNCUtil;
import org.opendaylight.protocol.pcep.parser.object.bnc.BranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.parser.object.bnc.NonBranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsObjectSerializer;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPP2MPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPP2MPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPIpv4UnreachDestinationParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPIpv6UnreachDestinationParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPUnreachDestinationSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.AssociationGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.association.object.association.group.AssociationTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.branch.node.object.BranchNodeListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.XroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.gc.object.GcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.load.balancing.object.LoadBalancingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.object.Monitoring.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.object.MonitoringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.non.branch.node.object.NonBranchNodeListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.of.object.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.overload.object.OverloadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.path.key.object.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.path.key.object.path.key.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.path.key.object.path.key.PathKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcc.id.req.object.PccIdReqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pce.id.object.PceIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.proc.time.object.ProcTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.svec.object.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.unreach.destination.object.UnreachDestinationObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.unreach.destination.object.unreach.destination.obj.destination.Ipv4DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.unreach.destination.object.unreach.destination.obj.destination.Ipv4DestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.unreach.destination.object.unreach.destination.obj.destination.Ipv6DestinationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.unreach.destination.object.unreach.destination.obj.destination.Ipv6DestinationCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AssociationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.P2mpLeaves;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.ProtocolOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.BidirectionalLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.DisjointnessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.PathProtectionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.association.type.tlvs.association.type.tlvs.SrPolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.disjointness.tlvs.ConfigurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.disjointness.tlvs.StatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.P2mpIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.P2mpIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.p2mp.ipv4._case.P2mpIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.p2mp.ipv6._case.P2mpIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.of.list.tlv.OfListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.tlvs.CpathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.sr.policy.tlvs.PolicyIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPObjectParserTest {

    private TlvRegistry tlvRegistry;

    private VendorInformationTlvRegistry viTlvRegistry;

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;
    private TestVendorInformationActivator viAct;

    @Before
    public void setUp() {
        ctx = new SimplePCEPExtensionProviderContext();
        act = new BaseParserExtensionActivator();
        viAct = new TestVendorInformationActivator();
        act.start(ctx);
        viAct.start(ctx);
        tlvRegistry = ctx.getTlvHandlerRegistry();
        viTlvRegistry = ctx.getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWOTLV() throws PCEPDeserializerException, IOException {
        final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPOpenObject1.bin")));

        final OpenBuilder builder = new OpenBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setVersion(new ProtocolVersion(Uint8.ONE))
                .setKeepalive(Uint8.valueOf(30))
                .setDeadTimer(Uint8.valueOf(120))
                .setSessionId(Uint8.ONE);

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open
            .object.open.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testCloseObject() throws IOException, PCEPDeserializerException {
        final PCEPCloseObjectParser parser = new PCEPCloseObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPCloseObject1.bin")));

        final CCloseBuilder builder = new CCloseBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setReason(Uint8.valueOf(5))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
                    .close.object.c.close.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testLoadBalancingObject() throws IOException, PCEPDeserializerException {
        final PCEPLoadBalancingObjectParser parser = new PCEPLoadBalancingObjectParser();
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPLoadBalancingObject1.bin")));

        final LoadBalancingBuilder builder = new LoadBalancingBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setMaxLsp(Uint8.valueOf(0xf1))
                .setMinBandwidth(new Bandwidth(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testERObject() throws Exception {
        final PCEPExplicitRouteObjectParser parser =
            new PCEPExplicitRouteObjectParser(ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPExplicitRouteObject1PackOfSubobjects.bin")));

        final EroBuilder builder = new EroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.Subobject> subs = new ArrayList<>();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.SubobjectBuilder()
                .setLoose(true)
                .setSubobjectType(new AsNumberCaseBuilder()
                    .setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(Uint32.valueOf(0xffff))).build())
                    .build())
                .build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.SubobjectBuilder().setLoose(true).setSubobjectType(new IpPrefixCaseBuilder()
                .setIpPrefix(
                    new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build())
                .build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.SubobjectBuilder()
                .setLoose(true)
                .setSubobjectType(new UnnumberedCaseBuilder()
                    .setUnnumbered(new UnnumberedBuilder()
                        .setRouterId(Uint32.valueOf(0xffffffffL))
                        .setInterfaceId(Uint32.valueOf(0xffffffffL))
                        .build())
                    .build())
                .build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null.", e.getMessage());
        }
    }

    @Test
    public void testIRObject() throws Exception {
        final PCEPIncludeRouteObjectParser parser =
            new PCEPIncludeRouteObjectParser(ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPIncludeRouteObject1PackOfSubobjects.bin")));
        final byte[] ip6PrefixBytes = {
            (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        final IroBuilder builder = new IroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include
            .route.object.iro.Subobject> subs = new ArrayList<>();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include
            .route.object.iro.SubobjectBuilder().setSubobjectType(
                new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(0x10))).build()).build()).setLoose(true).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include
            .route.object.iro.SubobjectBuilder().setSubobjectType(
                new IpPrefixCaseBuilder().setIpPrefix(
                    new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("18.52.80.0/21"))).build()).build())
                        .setLoose(true).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include
            .route.object.iro.SubobjectBuilder().setSubobjectType(
                new IpPrefixCaseBuilder().setIpPrefix(
                    new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22)))
                        .build()).build()).setLoose(true).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include
            .route.object.iro.SubobjectBuilder().setSubobjectType(new UnnumberedCaseBuilder()
                .setUnnumbered(new UnnumberedBuilder()
                    .setRouterId(Uint32.valueOf(0x1245678L)).setInterfaceId(Uint32.valueOf(0x9abcdef0L))
                    .build()).build()).setLoose(true).build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRRObject() throws Exception {
        final PCEPReportedRouteObjectParser parser =
            new PCEPReportedRouteObjectParser(ctx.getRROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPReportedRouteObject1PackOfSubobjects.bin")));
        final byte[] ip6PrefixBytes = {
            (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };

        final RroBuilder builder = new RroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported
                .route.object.rro.Subobject> subs = new ArrayList<>();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported
            .route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record
                .route.subobjects.subobject.type.IpPrefixCaseBuilder().setIpPrefix(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record
                    .route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder().setIpPrefix(
                        new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build())
            .setProtectionAvailable(false).setProtectionInUse(false).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported
            .route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route
                .subobjects.subobject.type.IpPrefixCaseBuilder().setIpPrefix(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record
                    .route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder().setIpPrefix(
                        new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22))).build()).build())
                        .setProtectionAvailable(false).setProtectionInUse(false).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported
            .route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record
                    .route.subobjects.subobject.type.UnnumberedCaseBuilder().setUnnumbered(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record
                            .route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder()
                            .setRouterId(Uint32.valueOf(0x1245678L)).setInterfaceId(Uint32.valueOf(0x9abcdef0L))
                            .build())
                    .build())
                .setProtectionAvailable(false).setProtectionInUse(false).build());
        builder.setSubobject(subs);

        // Verify Non empty RRO subObject
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        // Verify empty RRO subObject
        final RroBuilder emptyBuilder = new RroBuilder();
        emptyBuilder.setIgnore(false);
        emptyBuilder.setProcessingRule(false);
        assertEquals(emptyBuilder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), null));
        assertEquals(emptyBuilder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            Unpooled.EMPTY_BUFFER));
    }

    @Test
    public void testBandwidthObject() throws IOException, PCEPDeserializerException {
        final PCEPBandwidthObjectParser parser = new PCEPBandwidthObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPBandwidthObject1LowerBounds.bin")));

        final BandwidthBuilder builder = new BandwidthBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testExistingBandwidthObject() throws IOException, PCEPDeserializerException {
        final PCEPExistingBandwidthObjectParser parser = new PCEPExistingBandwidthObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPBandwidthObject2UpperBounds.bin")));

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reoptimization
            .bandwidth.object.ReoptimizationBandwidthBuilder builder = new org.opendaylight.yang.gen.v1
            .urn.opendaylight.params.xml
            .ns.yang.pcep.object.rev250930.reoptimization.bandwidth.object.ReoptimizationBandwidthBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setBandwidth(new Bandwidth(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testEndPointsObjectIPv4() throws IOException, PCEPDeserializerException {
        final byte[] srcIPBytes = { (byte) 0xA2, (byte) 0xF5, (byte) 0x11, (byte) 0x0E };
        final byte[] destIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        final PCEPEndPointsIpv4ObjectParser parser = new PCEPEndPointsIpv4ObjectParser();
        final ByteBuf result
            = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPEndPointsObject1IPv4.bin")));

        final EndpointsObjBuilder builder = new EndpointsObjBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setAddressFamily(new Ipv4CaseBuilder()
                    .setIpv4(new Ipv4Builder()
                        .setSourceIpv4Address(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes)))
                        .setDestinationIpv4Address(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(destIPBytes)))
                        .build())
                    .build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        final PCEPEndPointsObjectSerializer serializer = new PCEPEndPointsObjectSerializer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }


    @Test
    public void testEndPointsObjectP2MPIPv4() throws PCEPDeserializerException {
        final byte[] srcIPBytes = { (byte) 0xA2, (byte) 0xF5, (byte) 0x11, (byte) 0x0E };
        final byte[] destIPBytes = {
            (byte) 0x04, (byte) 0x32, (byte) 0x00, (byte) 0x14,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xA2, (byte) 0xF5, (byte) 0x11, (byte) 0x0E,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC};

        final PCEPP2MPEndPointsIpv4ObjectParser parser = new PCEPP2MPEndPointsIpv4ObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(destIPBytes);

        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setAddressFamily(new P2mpIpv4CaseBuilder().setP2mpIpv4(new P2mpIpv4Builder()
                .setP2mpLeaves(P2mpLeaves.NewLeavesToAdd)
                .setSourceIpv4Address(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes)))
                .setDestinationIpv4Address(ImmutableSet.of(new Ipv4AddressNoZone("255.255.255.255"),
                        new Ipv4AddressNoZone("255.255.255.252"))).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        final PCEPEndPointsObjectSerializer serializer = new PCEPEndPointsObjectSerializer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testEndPointsObjectIPv6() throws IOException, PCEPDeserializerException {
        final byte[] destIPBytes = {
            (byte) 0x00, (byte) 0x02, (byte) 0x5D, (byte) 0xD2, (byte) 0xFF, (byte) 0xEC, (byte) 0xA1, (byte) 0xB6,
            (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        };
        final byte[] srcIPBytes = {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };

        final PCEPEndPointsIpv6ObjectParser parser = new PCEPEndPointsIpv6ObjectParser();
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPEndPointsObject2IPv6.bin")));

        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setAddressFamily(new Ipv6CaseBuilder().setIpv6(
            new Ipv6Builder().setSourceIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes)))
                .setDestinationIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(destIPBytes)))
                .build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        final PCEPEndPointsObjectSerializer serializer = new PCEPEndPointsObjectSerializer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testEndPointsObjectP2MPIPv6() throws IOException, PCEPDeserializerException {
        final byte[] destIPBytes = {
            (byte) 0x04, (byte) 0x42, (byte) 0x00, (byte) 0x38,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0x00, (byte) 0x02, (byte) 0x5D, (byte) 0xD2,
            (byte) 0xFF, (byte) 0xEC, (byte) 0xA1, (byte) 0xB6,
            (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x03, (byte) 0x5D, (byte) 0xD2,
            (byte) 0xFF, (byte) 0xEC, (byte) 0xA1, (byte) 0xB6,
            (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        };
        final byte[] srcIPBytes = {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };

        final PCEPP2MPEndPointsIpv6ObjectParser parser = new PCEPP2MPEndPointsIpv6ObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(destIPBytes);

        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setAddressFamily(new P2mpIpv6CaseBuilder().setP2mpIpv6(new P2mpIpv6Builder()
                .setP2mpLeaves(P2mpLeaves.NewLeavesToAdd)
                .setSourceIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes)))
                .setDestinationIpv6Address(ImmutableSet.of(
                        new Ipv6AddressNoZone("2:5dd2:ffec:a1b6:581e:9f50::"),
                        new Ipv6AddressNoZone("3:5dd2:ffec:a1b6:581e:9f50::")
                )).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        final PCEPEndPointsObjectSerializer serializer = new PCEPEndPointsObjectSerializer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testErrorObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPErrorObjectParser parser = new PCEPErrorObjectParser(tlvRegistry, viTlvRegistry);

        final ErrorObjectBuilder builder = new ErrorObjectBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setType(Uint8.ONE)
                .setValue(Uint8.ONE);

        ByteBuf result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPErrorObject1.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPErrorObject3.bin")));

        builder.setType(Uint8.valueOf(7))
            .setValue(Uint8.ZERO)
            .setTlvs(new TlvsBuilder().setReqMissing(new ReqMissingBuilder()
                .setRequestId(new RequestId(Uint32.valueOf(0x00001155L)))
                .build())
                .build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testLspaObject() throws IOException, PCEPDeserializerException {
        final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(tlvRegistry, viTlvRegistry);

        final LspaBuilder builder = new LspaBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setExcludeAny(new AttributeFilter(Uint32.ZERO))
                .setIncludeAny(new AttributeFilter(Uint32.ZERO))
                .setIncludeAll(new AttributeFilter(Uint32.ZERO))
                .setHoldPriority(Uint8.ZERO)
                .setSetupPriority(Uint8.ZERO)
                .setLocalProtectionDesired(false)
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa
                    .object.lspa.TlvsBuilder().build());

        ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPLspaObject1LowerBounds.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPLspaObject2UpperBounds.bin")));

        builder.setExcludeAny(new AttributeFilter(Uint32.MAX_VALUE))
            .setIncludeAny(new AttributeFilter(Uint32.MAX_VALUE))
            .setIncludeAll(new AttributeFilter(Uint32.MAX_VALUE))
            .setHoldPriority(Uint8.MAX_VALUE)
            .setSetupPriority(Uint8.MAX_VALUE)
            .setLocalProtectionDesired(true);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testMetricObject() throws IOException, PCEPDeserializerException {
        final PCEPMetricObjectParser parser = new PCEPMetricObjectParser();

        final MetricBuilder builder = new MetricBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setComputed(false)
                .setBound(false)
                .setMetricType(Uint8.ONE)
                .setValue(new Float32(new byte[] { 0, 0, 0, 0 }));

        ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPMetricObject1LowerBounds.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPMetricObject2UpperBounds.bin")));

        builder.setComputed(true)
            .setBound(false)
            .setMetricType(Uint8.TWO)
            .setValue(new Float32(new byte[] { (byte) 0x4f, (byte) 0x70, (byte) 0x00, (byte) 0x00 }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testNoPathObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPNoPathObjectParser parser = new PCEPNoPathObjectParser(tlvRegistry, viTlvRegistry);
        ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPNoPathObject1WithoutTLV.bin")));

        final NoPathBuilder builder = new NoPathBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setNatureOfIssue(Uint8.ONE)
                .setUnsatisfiedConstraints(true)
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
                    .replies.result.failure._case.no.path.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPNoPathObject2WithTLV.bin")));

        builder.setNatureOfIssue(Uint8.ZERO);
        builder.setUnsatisfiedConstraints(false);

        final NoPathVectorBuilder b = new NoPathVectorBuilder();
        b.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
            .NoPathVectorTlv.Flags(false, false, false, true, false, true, false, true, true, true));
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
            .replies.result.failure._case.no.path.TlvsBuilder().setNoPathVector(
                b.build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testNotifyObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPNotificationObjectParser parser =
            new PCEPNotificationObjectParser(tlvRegistry, viTlvRegistry);

        final CNotificationBuilder builder = new CNotificationBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setType(Uint8.MAX_VALUE)
                .setValue(Uint8.MAX_VALUE);

        ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPNotificationObject2WithoutTlv.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPNotificationObject1WithTlv.bin")));

        builder.setType(Uint8.TWO)
            .setValue(Uint8.ONE)
            .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
            .notification.object.c.notification.TlvsBuilder().setOverloadDuration(
                new OverloadDurationBuilder().setDuration(Uint32.valueOf(0xff0000a2L)).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRPObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPRequestParameterObjectParser parser =
            new PCEPRequestParameterObjectParser(tlvRegistry, viTlvRegistry);

        final RpBuilder builder = new RpBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setReoptimization(true)
                .setBiDirectional(false)
                .setLoose(true)
                .setMakeBeforeBreak(true)
                .setOrder(false)
                .setPathKey(false)
                .setSupplyOf(false)
                .setFragmentation(false)
                .setP2mp(false)
                .setEroCompression(false)
                .setPriority(Uint8.valueOf(5))
                .setRequestId(new RequestId(Uint32.valueOf(0xdeadbeefL)))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp
                    .object.rp.TlvsBuilder().build());

        ByteBuf result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPRPObject1.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPRPObject2.bin")));

        builder.setReoptimization(false);
        builder.setFragmentation(true);
        builder.setEroCompression(true);

        final OrderBuilder b = new OrderBuilder()
                .setDelete(Uint32.valueOf(0xffffffffL))
                .setSetup(Uint32.ONE);

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp
            .object.rp.TlvsBuilder().setOrder(b.build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testSvecObject() throws IOException, PCEPDeserializerException {
        final PCEPSvecObjectParser parser = new PCEPSvecObjectParser();

        final SvecBuilder builder = new SvecBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setLinkDiverse(false)
                .setNodeDiverse(false)
                .setSrlgDiverse(false)
                .setPartialPathDiverse(false)
                .setLinkDirectionDiverse(false)
                .setRequestsIds(Set.of(new RequestId(Uint32.valueOf(0xFF))));

        ByteBuf result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPSvecObject2.bin")));
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPSvecObject1_10ReqIDs.bin")));

        builder.setProcessingRule(true);
        builder.setLinkDiverse(true);
        builder.setSrlgDiverse(true);

        builder.setRequestsIds(ImmutableSet.of(
            // Order is important for assertion
            new RequestId(Uint32.valueOf(0xFFFFFFFFL)),
            new RequestId(Uint32.valueOf(0x00000001L)),
            new RequestId(Uint32.valueOf(0x01234567L)),
            new RequestId(Uint32.valueOf(0x89ABCDEFL)),
            new RequestId(Uint32.valueOf(0xFEDCBA98L)),
            new RequestId(Uint32.valueOf(0x76543210L)),
            new RequestId(Uint32.valueOf(0x15825266L)),
            new RequestId(Uint32.valueOf(0x48120BBEL)),
            new RequestId(Uint32.valueOf(0x25FB7E52L)),
            new RequestId(Uint32.valueOf(0xB2F2546BL))));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testClassTypeObject() throws PCEPDeserializerException {
        final PCEPClassTypeObjectParser parser = new PCEPClassTypeObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] {
            (byte) 0x16, (byte) 0x12, (byte) 0x00, (byte) 0x08, 0, 0, 0, (byte) 0x04 });

        final ClassTypeBuilder builder = new ClassTypeBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setClassType(new ClassType(Uint8.valueOf(4)));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testExcludeRouteObject() throws Exception {
        final PCEPExcludeRouteObjectParser parser =
            new PCEPExcludeRouteObjectParser(ctx.getXROSubobjectHandlerRegistry());
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPExcludeRouteObject.1.bin")));

        final XroBuilder builder = new XroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
            .exclude.route.object.Xro.Flags(true));
        final List<Subobject> subs = new ArrayList<>();
        subs.add(new SubobjectBuilder()
            .setMandatory(true)
            .setSubobjectType(new IpPrefixCaseBuilder()
                .setIpPrefix(new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("192.168.0.0/16"))).build())
                .build())
            .setAttribute(Attribute.Node).build());
        subs.add(new SubobjectBuilder()
            .setMandatory(false)
            .setSubobjectType(new AsNumberCaseBuilder()
                .setAsNumber(new AsNumberBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(0x1234L)))
                    .build())
                .build())
            .build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPathKeyObject() throws Exception {
        final PCEPPathKeyObjectParser parser = new PCEPPathKeyObjectParser(ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPPathKeyObject.bin")));

        final PathKeyBuilder builder = new PathKeyBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        final List<PathKeys> list = new ArrayList<>();
        list.add(new PathKeysBuilder().setLoose(true).setPathKey(new PathKey(Uint16.valueOf(0x1234)))
            .setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 })).build());
        builder.setPathKeys(list);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testObjectiveFunctionObject() throws IOException, PCEPDeserializerException {
        final PCEPObjectiveFunctionObjectParser parser =
            new PCEPObjectiveFunctionObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPObjectiveFunctionObject.1.bin")));

        final OfBuilder builder = new OfBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setCode(new OfId(Uint16.valueOf(4)))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.of
                    .object.of.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testGlobalConstraintsObject() throws IOException, PCEPDeserializerException {
        final PCEPGlobalConstraintsObjectParser parser =
            new PCEPGlobalConstraintsObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPGlobalConstraintsObject.1.bin")));

        final GcBuilder builder = new GcBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setMaxHop(Uint8.ONE)
                .setMaxUtilization(Uint8.ZERO)
                .setMinUtilization(Uint8.valueOf(100))
                .setOverBookingFactor(Uint8.valueOf(99))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.gc
                    .object.gc.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testIgnoreUknownObject() throws PCEPDeserializerException {
        final Object object =
            ctx.getObjectHandlerRegistry().parseObject(35, 1, new ObjectHeaderImpl(false, false), null);
        assertNull(object);
    }

    @Test
    public void testUnrecognizedObjectType() throws PCEPDeserializerException {
        final Object object =
            ctx.getObjectHandlerRegistry().parseObject(2, 2, new ObjectHeaderImpl(true, true), null);
        assertNotNull(object);
        assertTrue(object instanceof UnknownObject);
        assertEquals(PCEPErrors.UNRECOGNIZED_OBJ_TYPE, ((UnknownObject) object).getError());
    }

    @Test
    public void testUnrecognizedObjectClass() throws PCEPDeserializerException {
        final Object object = ctx.getObjectHandlerRegistry()
            .parseObject(35, 1, new ObjectHeaderImpl(true, true), null);
        assertNotNull(object);
        assertTrue(object instanceof UnknownObject);
        assertEquals(PCEPErrors.UNRECOGNIZED_OBJ_CLASS, ((UnknownObject) object).getError());
    }

    @Test
    public void testLspaObjectSerializerDefence() throws IOException {
        final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result =
            Unpooled.wrappedBuffer(Files.readAllBytes(Path.of("src/test/resources/PCEPLspaObject1LowerBounds.bin")));

        final LspaBuilder builder = new LspaBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setLocalProtectionDesired(false);

        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));
    }

    @Test
    public void testEmptyEroObject() throws PCEPDeserializerException {
        final Object object = ctx.getObjectHandlerRegistry().parseObject(7, 1,
            new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
        assertNotNull(object);
        assertTrue(object instanceof Ero);
        final Ero eroObject = (Ero) object;
        assertNull(eroObject.getSubobject());

        final ByteBuf buffer = Unpooled.buffer();
        ctx.getObjectHandlerRegistry().serializeObject(eroObject, buffer);
        final byte[] expected = {0x07, 0x13, 0x00, 0x04};
        assertArrayEquals(expected, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testCloseObjectWithVendorInformationTlv() throws PCEPDeserializerException {
        final byte[] closeBytes = {
            0x0f, 0x10, 0x00, 0x14,
            0x00, 0x00, 0x00, 0x05,
            /* vendor-information TLV */
            0x00, 0x07, 0x00, 0x08,
            /* enterprise number */
            0x00, 0x00, 0x00, 0x00,
            /* enterprise specific information */
            0x00, 0x00, 0x00, 0x05
        };
        final PCEPCloseObjectParser parser = new PCEPCloseObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(closeBytes);

        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationTlv viTlv = new VendorInformationTlvBuilder()
                .setEnterpriseNumber(new EnterpriseNumber(Uint32.ZERO))
                .setEnterpriseSpecificInformation(esInfo).build();
        final CCloseBuilder builder = new CCloseBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setReason(Uint8.valueOf(5))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
                    .close.object.c.close.TlvsBuilder().setVendorInformationTlv(Lists.newArrayList(viTlv)).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));
    }

    @Test
    public void testVendorInformationObject() throws PCEPDeserializerException {
        final byte[] viObjBytes = {
            /* vendor-information object */
            0x22, 0x10, 0x00, 0x0C,
            /* enterprise number */
            0x00, 0x00, 0x00, 0x00,
            /* enterprise specific information */
            0x00, 0x00, 0x00, 0x05
        };
        final TestVendorInformationObjectParser parser = new TestVendorInformationObjectParser();
        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationObject viObj = new VendorInformationObjectBuilder()
                .setEnterpriseNumber(new EnterpriseNumber(Uint32.ZERO))
                .setEnterpriseSpecificInformation(esInfo).build();
        final ByteBuf result = Unpooled.wrappedBuffer(viObjBytes);
        result.readerIndex(8);
        final VendorInformationObject o = (VendorInformationObject) parser.parseObject(
            new ObjectHeaderImpl(false, false), result.readSlice(result.readableBytes()));
        assertEquals(viObj, o);

        final ByteBuf buf = Unpooled.buffer(viObjBytes.length);
        parser.serializeObject(viObj, buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));
    }

    @Test
    public void testMonitoringObject() throws PCEPDeserializerException {
        final byte[] monitoringBytes = {
            /* object header */
            0x13, 0x10, 0x00, 0x0C,
            /* flags */
            0x00, 0x00, 0x00, 0x01,
            /* monitoring-id=16 */
            0x00, 0x00, 0x00, 0x10
        };
        final PCEPMonitoringObjectParser parser = new PCEPMonitoringObjectParser(tlvRegistry, viTlvRegistry);
        final Monitoring monitoring = new MonitoringBuilder()
                .setMonitoringId(Uint32.valueOf(16L))
                .setFlags(new Flags(false, false, true, false, false))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
                    .monitoring.object.monitoring.TlvsBuilder().build()).build();
        final ByteBuf result = Unpooled.wrappedBuffer(monitoringBytes);
        assertEquals(monitoring, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(monitoringBytes.length);
        parser.serializeObject(monitoring, buf);
        assertArrayEquals(monitoringBytes, buf.array());
    }

    @Test
    public void testPccIdReqIPv4Object() throws PCEPDeserializerException {
        final byte[] pccIdReqBytes = {
            /* object header */
            0x14, 0x10, 0x00, 0x08,
            /* ipv4 address */
            0x7f, 0x00, 0x00, 0x01
        };
        final PCEPPccIdReqIPv4ObjectParser parser = new PCEPPccIdReqIPv4ObjectParser();
        final PccIdReq pccIdReq =
            new PccIdReqBuilder().setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(pccIdReqBytes.length);
        parser.serializeObject(pccIdReq, buf);
        assertArrayEquals(pccIdReqBytes, buf.array());
    }

    @Test
    public void testPccIdReqIPv6Object() throws PCEPDeserializerException {
        final byte[] pccIdReqBytes = {
            /* object header */
            0x14, 0x20, 0x00, 0x14,
            /* ipv6 address */
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01
        };
        final PCEPPccIdReqIPv6ObjectParser parser = new PCEPPccIdReqIPv6ObjectParser();
        final PccIdReq pccIdReq =
            new PccIdReqBuilder().setIpAddress(new IpAddressNoZone(new Ipv6AddressNoZone("::1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(pccIdReqBytes.length);
        parser.serializeObject(pccIdReq, buf);
        assertArrayEquals(pccIdReqBytes, buf.array());
    }

    @Test
    public void testPceIdIPv4Object() throws PCEPDeserializerException {
        final byte[] pccIdReqBytes = {
            /* object header */
            0x19, 0x10, 0x00, 0x08,
            /* ipv4 address */
            0x7f, 0x00, 0x00, 0x01
        };
        final PCEPPceIdIPv4ObjectParser parser = new PCEPPceIdIPv4ObjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pce.id.object
            .PceId pceId = new PceIdBuilder().setIpAddress(new IpAddressNoZone(
                new Ipv4AddressNoZone("127.0.0.1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pceId, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(pccIdReqBytes.length);
        parser.serializeObject(pceId, buf);
        assertArrayEquals(pccIdReqBytes, buf.array());
    }

    @Test
    public void testPceIdIPv6Object() throws PCEPDeserializerException {
        final byte[] pccIdReqBytes = {
            /* object header */
            0x19, 0x20, 0x00, 0x14,
            /* ipv6 header */
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01
        };
        final PCEPPceIdIPv6ObjectParser parser = new PCEPPceIdIPv6ObjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pce.id.object
            .PceId pccIdReq = new PceIdBuilder().setIpAddress(new IpAddressNoZone(
                new Ipv6AddressNoZone("::1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(pccIdReqBytes.length);
        parser.serializeObject(pccIdReq, buf);
        assertArrayEquals(pccIdReqBytes, buf.array());
    }

    @Test
    public void testProcTimeObject() throws PCEPDeserializerException {
        final byte[] proctimeBytes = {
            /* object header */
            0x1A, 0x10, 0x00, 0x1C,
            /* E flag */
            0x00, 0x00, 0x00, 0x01,
            /* current proc. time */
            0x00, 0x00, 0x00, 0x01,
            /* min proc. time */
            0x00, 0x00, 0x00, 0x02,
            /* max proc time */
            0x00, 0x00, 0x00, 0x03,
            /* average proc time */
            0x00, 0x00, 0x00, 0x04,
            /* variance proc time */
            0x00, 0x00, 0x00, 0x05,
        };
        final PCEPProcTimeObjectParser parser = new PCEPProcTimeObjectParser();
        final ProcTime procTime = new ProcTimeBuilder()
            .setEstimated(true)
            .setAverageProcTime(Uint32.valueOf(4))
            .setCurrentProcTime(Uint32.ONE)
            .setMaxProcTime(Uint32.valueOf(3))
            .setMinProcTime(Uint32.TWO)
            .setVarianceProcTime(Uint32.valueOf(5))
            .build();
        final ByteBuf result = Unpooled.wrappedBuffer(proctimeBytes);
        assertEquals(procTime, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4,
            result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(proctimeBytes.length);
        parser.serializeObject(procTime, buf);
        assertArrayEquals(proctimeBytes, buf.array());
    }

    @Test
    public void testOverloadObject() throws PCEPDeserializerException {
        final byte[] overloadBytes = {
            /* object header */
            0x1B, 0x10, 0x00, 0x08,
            /* overload duration */
            0x00, 0x00, 0x00, 0x78
        };
        final PCEPOverloadObjectParser parser = new PCEPOverloadObjectParser();
        final Overload overload = new OverloadBuilder().setDuration(Uint16.valueOf(120)).build();
        final ByteBuf result = Unpooled.wrappedBuffer(overloadBytes);
        assertEquals(overload, parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));

        final ByteBuf buf = Unpooled.buffer(overloadBytes.length);
        parser.serializeObject(overload, buf);
        assertArrayEquals(overloadBytes, buf.array());
    }

    @Test
    public void testRpObjectWithPstTlvParser() throws PCEPDeserializerException {

        final byte[] rpObjectWithPstTlvBytes = { 0x2, 0x10, 0x0, 0x14, 0x0, 0x0, 0x4, 0x2d, (byte) 0xde,
            (byte) 0xad, (byte) 0xbe, (byte) 0xef,
            /* pst-tlv */
            0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0 };

        final PCEPRequestParameterObjectParser parser
            = new PCEPRequestParameterObjectParser(tlvRegistry, viTlvRegistry);
        final RpBuilder builder = new RpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setReoptimization(true)
                .setBiDirectional(false)
                .setLoose(true)
                .setMakeBeforeBreak(true)
                .setOrder(false)
                .setPathKey(false)
                .setSupplyOf(false)
                .setFragmentation(false)
                .setP2mp(false)
                .setEroCompression(false)
                .setPriority(Uint8.valueOf(5))
                .setRequestId(new RequestId(Uint32.valueOf(0xdeadbeefL)))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp
                    .object.rp.TlvsBuilder()
                        .setPathSetupType(new PathSetupTypeBuilder().setPst(PsType.RsvpTe)
                            .build())
                        .build());

        final ByteBuf result = Unpooled.wrappedBuffer(rpObjectWithPstTlvBytes);
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }

    @Test
    public void testBranchNodeListObject() throws Exception {
        final byte[] expected = {
            0x1f, 0x10, 0x0, 0xc,
            (byte) 0x81, 0x8,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x20, 0x0,
        };

        final BranchNodeListObjectParser parser
            = new BranchNodeListObjectParser(ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(expected);

        final BranchNodeListBuilder builder = new BranchNodeListBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.Subobject> subs = new ArrayList<>();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.SubobjectBuilder()
            .setLoose(true)
            .setSubobjectType(new IpPrefixCaseBuilder()
                .setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                    new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build()).build());
        builder.setSubobject(BNCUtil.toBncSubobject(subs));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testNonBranchNodeListObject() throws Exception {
        final byte[] expected = {
            0x1f, 0x20, 0x0, 0xc,
            (byte) 0x81, 0x8,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x20, 0x0,
        };

        final NonBranchNodeListObjectParser parser
            = new NonBranchNodeListObjectParser(ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(expected);

        final NonBranchNodeListBuilder builder = new NonBranchNodeListBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.Subobject> subs = new ArrayList<>();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit
            .route.object.ero.SubobjectBuilder()
            .setLoose(true)
            .setSubobjectType(new IpPrefixCaseBuilder()
                .setIpPrefix(new IpPrefixBuilder().setIpPrefix(
                    new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build()).build());
        builder.setSubobject(BNCUtil.toBncSubobject(subs));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPCEPIpv4UnreachDestinationObject() throws Exception {
        final byte[] expected = {
            0x1c, 0x10, 0x0, 0x8,
            (byte) 0x7F, (byte) 0x0, (byte) 0x0, (byte) 0x1
        };

        final PCEPIpv4UnreachDestinationParser parser = new PCEPIpv4UnreachDestinationParser();
        final PCEPUnreachDestinationSerializer serializer = new PCEPUnreachDestinationSerializer();
        final ByteBuf result = Unpooled.wrappedBuffer(expected);

        final UnreachDestinationObjBuilder builder = new UnreachDestinationObjBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final Ipv4DestinationCase dest = new Ipv4DestinationCaseBuilder()
            .setDestinationIpv4Address(Set.of(new Ipv4AddressNoZone("127.0.0.1")))
            .build();
        builder.setDestination(dest);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPCEPIpv6UnreachDestinationObject() throws Exception {
        final byte[] expected = {
            0x1c, 0x20, 0x0, 0x14,
            (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
            (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
            (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
            (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1
        };

        final PCEPIpv6UnreachDestinationParser parser = new PCEPIpv6UnreachDestinationParser();
        final PCEPUnreachDestinationSerializer serializer = new PCEPUnreachDestinationSerializer();
        final ByteBuf result = Unpooled.wrappedBuffer(expected);

        final UnreachDestinationObjBuilder builder = new UnreachDestinationObjBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final Ipv6DestinationCase dest = new Ipv6DestinationCaseBuilder()
            .setDestinationIpv6Address(Set.of(new Ipv6AddressNoZone("::1")))
            .build();
        builder.setDestination(dest);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        serializer.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPcepAssociationIpv4Object() throws Exception {
        final byte[] pathProtection = {
            // Object Header
            0x28, 0x10, 0x00, 0x2c,
            // Association Group Object
            0x00, 0x00, 0x00, 0x01, // Reserved + Remove Flag set
            0x00, 0x01, 0x00, 0x01, // Association-Type = 1 (Path Protection) + Association-Id = 1
            0x0a, 0x01, 0x02, 0x03, // Association-Source IPv4 (10.1.2.3)
            0x00, 0x1e, 0x00, 0x04, // Global Association TLV
            0x01, 0x02, 0x03, 0x04,
            0x00, 0x1f, 0x00, 0x08, // Extended Association ID
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x00, 0x26, 0x00, 0x04, // Path Protection TLV
            0x08, 0x00, 0x00, 0x01
        };

        final byte[] disjointness = {
            // Object Header
            0x28, 0x10, 0x00, 0x3c,
            // Association Group Object
            0x00, 0x00, 0x00, 0x00, // Reserved + Remove Flag unset
            0x00, 0x02, 0x00, 0x01, // Association-Type = 2 (Disjointness) + Association-Id = 1
            0x0a, 0x01, 0x02, 0x03, // Association-Source IPv4 (10.1.2.3)
            0x00, 0x1e, 0x00, 0x04, // Global Association TLV
            0x01, 0x02, 0x03, 0x04,
            0x00, 0x2e, 0x00, 0x04, // Disjointness Configuration TLVs
            0x00, 0x00, 0x00, 0x05,
            0x00, 0x2f, 0x00, 0x04, // Disjointness Status TLV
            0x00, 0x00, 0x00, 0x1a,
            0x00, 0x04, 0x00, 0x02, // Objective Function TLV
            0x00, 0x0f, 0x00, 0x00,
            0x00, 0x07, 0x00, 0x08, // Vendor Information TLV
            0x00, 0x00, 0x05, 0x58,
            (byte )0xde, (byte )0xad, (byte )0xbe, (byte )0xef
        };

        final AssociationTlvsBuilder associationTlvs = new AssociationTlvsBuilder()
            .setExtendedAssociationId(List.of(Uint32.valueOf(16909060), Uint32.valueOf(84281096)))
            .setGlobalAssociationSource(Uint32.valueOf(16909060))
            .setAssociationTypeTlvs(new PathProtectionBuilder()
                .setProtecting(true)
                .setSecondary(false)
                .setProtectionType(LspFlag.UnidirectionalProtection)
                .build()
            );
        final AssociationGroupBuilder builder = new AssociationGroupBuilder()
            .setProcessingRule(false)
            .setIgnore(false)
            .setRemovalFlag(true)
            .setAssociationType(AssociationType.forValue(1))
            .setAssociationId(Uint16.ONE)
            .setAssociationSource(new IpAddressNoZone(new Ipv4AddressNoZone("10.1.2.3")))
            .setAssociationTlvs(associationTlvs.build());

        final PCEPAssociationIPv4ObjectParser parser = new PCEPAssociationIPv4ObjectParser();
        final ByteBuf protectionResult = Unpooled.wrappedBuffer(pathProtection);

        // Test Association Group with Path Protection
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
                protectionResult.slice(4, protectionResult.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(pathProtection, ByteArray.getAllBytes(buf));

        // Test Association Group with Disjointness
        associationTlvs.setExtendedAssociationId(null).setAssociationTypeTlvs(new DisjointnessBuilder()
            .setConfiguration(new ConfigurationBuilder().setLinkDiverse(true).setNodeDiverse(false).setSrlgDiverse(true)
                .setShortestPath(false).setStrictDisjointness(false).build())
            .setStatus(new StatusBuilder().setLinkDiverse(false).setNodeDiverse(true).setSrlgDiverse(false)
                .setShortestPath(true).setStrictDisjointness(true).build())
            .setOfList(new OfListBuilder()
                .setCodes(Set.of(new OfId(Uint16.valueOf(15)))).build())
            .build()
        );
        builder.setRemovalFlag(false)
            .setAssociationType(AssociationType.Disjoint)
            .setAssociationTlvs(associationTlvs.build());

        final ByteBuf disjointResult = Unpooled.wrappedBuffer(disjointness);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
                disjointResult.slice(4, disjointResult.readableBytes() - 4)));
        buf.clear();
        parser.serializeObject(builder.build(), buf);
        // Skip Vendor Information and change Object Size before comparison
        final byte[] expected = Arrays.copyOfRange(disjointness, 0, disjointness.length - 12);
        expected[3] = 0x30;
        assertArrayEquals(expected, ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPcepAssociationIpv6Object() throws Exception {
        final byte[] policyParameter = {
            // Object Header
            0x28, 0x20, 0x00, 0x30,
            // Association Group Object
            0x00, 0x00, 0x00, 0x01, // Reserved + Remove Flag set
            0x00, 0x03, 0x00, 0x01, // Association-Type = 3 (Policy Parameters) + Association-Id = 1
            0x00, 0x00, 0x00, 0x00, // Association-Source IPv6 (::1)
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x1f, 0x00, 0x04, // Extended Association ID
            0x05, 0x06, 0x07, 0x08,
            0x00, 0x30, 0x00, 0x07, // Policy Parameters TLV
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x00
        };

        final byte[] bidirectional = {
            // Object Header
            0x28, 0x20, 0x00, 0x24,
            // Association Group Object
            0x00, 0x00, 0x00, 0x00, // Reserved + Remove Flag unset
            0x00, 0x04, 0x00, 0x01, // Association-Type = 4 (Bidirectional) + Association-Id = 1
            0x00, 0x00, 0x00, 0x00, // Association-Source IPv6 (::1)
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x36, 0x00, 0x04, // Bidirectional TLV
            0x00, 0x00, 0x00, 0x03
        };

        final AssociationTlvsBuilder associationTlvs = new AssociationTlvsBuilder()
            .setExtendedAssociationId(List.of(Uint32.valueOf(84281096)))
            .setAssociationTypeTlvs(new PolicyBuilder()
                .setPolicyParameters(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07})
                .build()
            );
        final AssociationGroupBuilder builder = new AssociationGroupBuilder()
            .setProcessingRule(false)
            .setIgnore(false)
            .setRemovalFlag(true)
            .setAssociationType(AssociationType.forValue(3))
            .setAssociationId(Uint16.ONE)
            .setAssociationSource(new IpAddressNoZone(new Ipv6AddressNoZone("::1")))
            .setAssociationTlvs(associationTlvs.build());

        final PCEPAssociationIPv6ObjectParser parser = new PCEPAssociationIPv6ObjectParser();
        final ByteBuf protectionResult = Unpooled.wrappedBuffer(policyParameter);

        // Test Association Group with Policy Parameter
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
                protectionResult.slice(4, protectionResult.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(policyParameter, ByteArray.getAllBytes(buf));

        // Test Association Group with Bidirectional LSP
        associationTlvs.setExtendedAssociationId(null).setAssociationTypeTlvs(new BidirectionalLspBuilder()
            .setCoRoutedPath(true).setReverseLsp(true).build()
        );
        builder.setRemovalFlag(false)
            .setAssociationType(AssociationType.SingleSideLsp)
            .setAssociationTlvs(associationTlvs.build());

        final ByteBuf disjointResult = Unpooled.wrappedBuffer(bidirectional);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
                disjointResult.slice(4, disjointResult.readableBytes() - 4)));
        buf.clear();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(bidirectional, ByteArray.getAllBytes(buf));

        try {
            parser.parseObject(new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testPcepSrPolicy() throws Exception {
        final byte[] srPolicy = {
            // Object Header
            0x28, 0x10, 0x00, 0x54,
            // Association Group Object
            0x00, 0x00, 0x00, 0x00, // Reserved
            0x00, 0x06, 0x00, 0x01, // Association-Type = 6 (SR Policy) + Association-Id = 1
            0x0a, 0x01, 0x02, 0x03, // Association-Source IPv4 (10.1.2.3)
            0x00, 0x1f, 0x00, 0x08, // Extended Association ID
            0x00, 0x00, 0x00, 0x0a, // SR Policy Color
            0x0a, 0x01, 0x02, 0x01, // SR Policy Endpoint (10.1.2.1)
            0x00, 0x38, 0x00, 0x04, // SR Policy Name = "Test"
            0x54, 0x65, 0x73, 0x74,
            0x00, 0x39, 0x00, 0x1c, // SR Policy Candidate Path Identifier
            0x0a, 0x00, 0x00, 0x00, // Origin = PCEP
            0x00, 0x00, (byte) 0xff, (byte) 0xff, // ASN = 65535
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x0a, 0x00, 0x00, 0x01, // Originator address = 10.0.0.1
            0x00, 0x00, 0x00, 0x02, // Discriminator = 2
            0x00, 0x3a, 0x00, 0x03, // SR Policy Candidate Path Name = "Foo"
            0x46, 0x6f, 0x6f, 0x00,
            0x00, 0x3b, 0x00, 0x04, // SR Policy Candidate Path Preference = 10
            0x00, 0x00, 0x00, 0x0a
        };

        final AssociationTlvsBuilder associationTlvs = new AssociationTlvsBuilder()
            .setExtendedAssociationId(List.of(Uint32.TEN, Uint32.valueOf(167838209)))
            .setAssociationTypeTlvs(new SrPolicyBuilder()
                .setPolicyId(new PolicyIdBuilder()
                    .setColor(Uint32.TEN)
                    .setEndpoint(new IpAddressNoZone(new Ipv4AddressNoZone("10.1.2.1")))
                    .setName(new byte[] {0x54, 0x65, 0x73, 0x74})
                    .build())
                .setCpath(new CpathBuilder()
                    .setOrigin(ProtocolOrigin.Pcep)
                    .setOriginatorAsn(Uint32.valueOf(65535))
                    .setOriginatorAddress(new IpAddressNoZone(new Ipv6AddressNoZone("::a00:1")))
                    .setDiscriminator(Uint32.TWO)
                    .setName(new byte[] {0x46, 0x6f, 0x6f})
                    .setPreference(Uint32.TEN)
                    .build())
                .build()
            );
        final AssociationGroupBuilder builder = new AssociationGroupBuilder()
            .setProcessingRule(false)
            .setIgnore(false)
            .setRemovalFlag(false)
            .setAssociationType(AssociationType.SrPolicy)
            .setAssociationId(Uint16.ONE)
            .setAssociationSource(new IpAddressNoZone(new Ipv4AddressNoZone("10.1.2.3")))
            .setAssociationTlvs(associationTlvs.build());

        final PCEPAssociationIPv4ObjectParser parser = new PCEPAssociationIPv4ObjectParser();
        final ByteBuf protectionResult = Unpooled.wrappedBuffer(srPolicy);

        // Test Association Group with SrPolicy
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
                protectionResult.slice(4, protectionResult.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srPolicy, ByteArray.getAllBytes(buf));
    }
}
