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

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.impl.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExistingBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMonitoringObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOverloadObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPccIdReqIPv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPccIdReqIPv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPceIdIPv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPceIdIPv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPProcTimeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.UnknownObject;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.XroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.GcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.MonitoringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.OrderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.object.OverloadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.path.key.PathKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.error.object.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.proc.time.object.ProcTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.tlvs.VendorInformationTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

public class PCEPObjectParserTest {

    private TlvRegistry tlvRegistry;

    private VendorInformationTlvRegistry viTlvRegistry;

    private SimplePCEPExtensionProviderContext ctx;
    private Activator act;
    private TestVendorInformationActivator viAct;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new Activator();
        this.viAct = new TestVendorInformationActivator();
        this.act.start(this.ctx);
        this.viAct.start(this.ctx);
        this.tlvRegistry = this.ctx.getTlvHandlerRegistry();
        this.viTlvRegistry = this.ctx.getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWOTLV() throws PCEPDeserializerException, IOException {
        final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin"));

        final OpenBuilder builder = new OpenBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setVersion(new ProtocolVersion((short) 1));
        builder.setKeepalive((short) 30);
        builder.setDeadTimer((short) 120);
        builder.setSessionId((short) 1);

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testCloseObject() throws IOException, PCEPDeserializerException {
        final PCEPCloseObjectParser parser = new PCEPCloseObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPCloseObject1.bin"));

        final CCloseBuilder builder = new CCloseBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setReason((short) 5);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.c.close.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testLoadBalancingObject() throws IOException, PCEPDeserializerException {
        final PCEPLoadBalancingObjectParser parser = new PCEPLoadBalancingObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLoadBalancingObject1.bin"));

        final LoadBalancingBuilder builder = new LoadBalancingBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setMaxLsp((short) UnsignedBytes.toInt((byte) 0xf1));
        builder.setMinBandwidth(new Bandwidth(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(this.ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPExplicitRouteObject1PackOfSubobjects.bin"));

        final EroBuilder builder = new EroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject> subs = Lists.newArrayList();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setLoose(
                true).setSubobjectType(
                        new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(0xffffL)).build()).build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setLoose(
                true).setSubobjectType(
                        new IpPrefixCaseBuilder().setIpPrefix(
                                new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder().setLoose(
                true).setSubobjectType(
                        new UnnumberedCaseBuilder().setUnnumbered(
                                new UnnumberedBuilder().setRouterId(0xffffffffL).setInterfaceId(0xffffffffL).build()).build()).build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPIncludeRouteObjectParser parser = new PCEPIncludeRouteObjectParser(this.ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPIncludeRouteObject1PackOfSubobjects.bin"));
        final byte[] ip6PrefixBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

        final IroBuilder builder = new IroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.Subobject> subs = Lists.newArrayList();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder().setSubobjectType(
                new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(0x10L)).build()).build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder().setSubobjectType(
                new IpPrefixCaseBuilder().setIpPrefix(
                        new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("18.52.80.0/21"))).build()).build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder().setSubobjectType(
                new IpPrefixCaseBuilder().setIpPrefix(
                        new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22))).build()).build()).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.iro.SubobjectBuilder().setSubobjectType(
                new UnnumberedCaseBuilder().setUnnumbered(
                        new UnnumberedBuilder().setRouterId(0x1245678L).setInterfaceId(0x9abcdef0L).build()).build()).build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPReportedRouteObjectParser parser = new PCEPReportedRouteObjectParser(this.ctx.getRROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPReportedRouteObject1PackOfSubobjects.bin"));
        final byte[] ip6PrefixBytes = { (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

        final RroBuilder builder = new RroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject> subs = Lists.newArrayList();
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder().setIpPrefix(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder().setIpPrefix(
                                new IpPrefix(new Ipv4Prefix("255.255.255.255/32"))).build()).build()).setProtectionAvailable(false).setProtectionInUse(
                                        false).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder().setIpPrefix(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder().setIpPrefix(
                                new IpPrefix(Ipv6Util.prefixForBytes(ip6PrefixBytes, 22))).build()).build()).setProtectionAvailable(false).setProtectionInUse(
                                        false).build());
        subs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder().setSubobjectType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder().setUnnumbered(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder().setRouterId(
                                0x1245678L).setInterfaceId(0x9abcdef0L).build()).build()).setProtectionAvailable(false).setProtectionInUse(
                                        false).build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testBandwidthObject() throws IOException, PCEPDeserializerException {
        final PCEPBandwidthObjectParser parser = new PCEPBandwidthObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPBandwidthObject1LowerBounds.bin"));

        final BandwidthBuilder builder = new BandwidthBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPBandwidthObject2UpperBounds.bin"));

        final ReoptimizationBandwidthBuilder builder = new ReoptimizationBandwidthBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setBandwidth(new Bandwidth(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPEndPointsObject1IPv4.bin"));

        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setAddressFamily(new Ipv4CaseBuilder().setIpv4(
                new Ipv4Builder().setSourceIpv4Address(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes))).setDestinationIpv4Address(
                        Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(destIPBytes))).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testEndPointsObjectIPv6() throws IOException, PCEPDeserializerException {
        final byte[] destIPBytes = { (byte) 0x00, (byte) 0x02, (byte) 0x5D, (byte) 0xD2, (byte) 0xFF, (byte) 0xEC, (byte) 0xA1,
            (byte) 0xB6, (byte) 0x58, (byte) 0x1E, (byte) 0x9F, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, };
        final byte[] srcIPBytes = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        final PCEPEndPointsIpv6ObjectParser parser = new PCEPEndPointsIpv6ObjectParser();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPEndPointsObject2IPv6.bin"));

        final EndpointsObjBuilder builder = new EndpointsObjBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setAddressFamily(new Ipv6CaseBuilder().setIpv6(
                new Ipv6Builder().setSourceIpv6Address(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(srcIPBytes))).setDestinationIpv6Address(
                        Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(destIPBytes))).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
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
    public void testErrorObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPErrorObjectParser parser = new PCEPErrorObjectParser(this.tlvRegistry, this.viTlvRegistry);
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPErrorObject1.bin"));

        final ErrorObjectBuilder builder = new ErrorObjectBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setType((short) 1);
        builder.setValue((short) 1);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPErrorObject3.bin"));

        builder.setType((short) 7);
        builder.setValue((short) 0);
        builder.setTlvs(new TlvsBuilder().setReqMissing(new ReqMissingBuilder().setRequestId(new RequestId(0x00001155L)).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(this.tlvRegistry, this.viTlvRegistry);
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspaObject1LowerBounds.bin"));

        final LspaBuilder builder = new LspaBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setExcludeAny(new AttributeFilter(0L));
        builder.setIncludeAny(new AttributeFilter(0L));
        builder.setIncludeAll(new AttributeFilter(0L));
        builder.setHoldPriority((short) 0);
        builder.setSetupPriority((short) 0);
        builder.setLocalProtectionDesired(false);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspaObject2UpperBounds.bin"));

        builder.setExcludeAny(new AttributeFilter(0xFFFFFFFFL));
        builder.setIncludeAny(new AttributeFilter(0xFFFFFFFFL));
        builder.setIncludeAll(new AttributeFilter(0xFFFFFFFFL));
        builder.setHoldPriority((short) 0xFF);
        builder.setSetupPriority((short) 0xFF);
        builder.setLocalProtectionDesired(true);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
    public void testMetricObject() throws IOException, PCEPDeserializerException {
        final PCEPMetricObjectParser parser = new PCEPMetricObjectParser();
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPMetricObject1LowerBounds.bin"));

        final MetricBuilder builder = new MetricBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setComputed(false);
        builder.setBound(false);
        builder.setMetricType((short) 1);
        builder.setValue(new Float32(new byte[] { 0, 0, 0, 0 }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPMetricObject2UpperBounds.bin"));

        builder.setComputed(true);
        builder.setBound(false);
        builder.setMetricType((short) 2);
        builder.setValue(new Float32(new byte[] { (byte) 0x4f, (byte) 0x70, (byte) 0x00, (byte) 0x00 }));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
    public void testNoPathObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPNoPathObjectParser parser = new PCEPNoPathObjectParser(this.tlvRegistry, this.viTlvRegistry);
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPNoPathObject1WithoutTLV.bin"));

        final NoPathBuilder builder = new NoPathBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setNatureOfIssue((short) 1);
        builder.setUnsatisfiedConstraints(true);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPNoPathObject2WithTLV.bin"));

        builder.setNatureOfIssue((short) 0);
        builder.setUnsatisfiedConstraints(false);

        final NoPathVectorBuilder b = new NoPathVectorBuilder();
        b.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv.Flags(false, true, false, true, false, true, true, true));
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder().setNoPathVector(
                b.build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
    public void testNotifyObjectWithTlv() throws PCEPDeserializerException, IOException {
        final PCEPNotificationObjectParser parser = new PCEPNotificationObjectParser(this.tlvRegistry, this.viTlvRegistry);
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPNotificationObject2WithoutTlv.bin"));

        final CNotificationBuilder builder = new CNotificationBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setType((short) 0xff);
        builder.setValue((short) 0xff);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPNotificationObject1WithTlv.bin"));

        builder.setType((short) 2);
        builder.setValue((short) 1);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.c.notification.TlvsBuilder().setOverloadDuration(
                new OverloadDurationBuilder().setDuration(0xff0000a2L).build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPRequestParameterObjectParser parser = new PCEPRequestParameterObjectParser(this.tlvRegistry, this.viTlvRegistry);
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPRPObject1.bin"));

        final RpBuilder builder = new RpBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setReoptimization(true);
        builder.setBiDirectional(false);
        builder.setLoose(true);
        builder.setMakeBeforeBreak(true);
        builder.setOrder(false);
        builder.setPathKey(false);
        builder.setSupplyOf(false);
        builder.setFragmentation(false);
        builder.setP2mp(false);
        builder.setEroCompression(false);
        builder.setPriority((short) 5);
        builder.setRequestId(new RequestId(0xdeadbeefL));
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPRPObject2.bin"));

        builder.setReoptimization(false);
        builder.setFragmentation(true);
        builder.setEroCompression(true);

        final OrderBuilder b = new OrderBuilder();
        b.setDelete(0xffffffffL);
        b.setSetup(1L);

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder().setOrder(
                b.build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
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
    public void testSvecObject() throws IOException, PCEPDeserializerException {
        final PCEPSvecObjectParser parser = new PCEPSvecObjectParser();
        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPSvecObject2.bin"));

        final SvecBuilder builder = new SvecBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setLinkDiverse(false);
        builder.setNodeDiverse(false);
        builder.setSrlgDiverse(false);
        builder.setRequestsIds(Lists.newArrayList(new RequestId(0xFFL)));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPSvecObject1_10ReqIDs.bin"));

        builder.setProcessingRule(true);
        builder.setLinkDiverse(true);
        builder.setSrlgDiverse(true);

        final List<RequestId> requestIDs = Lists.newArrayList();
        requestIDs.add(new RequestId(0xFFFFFFFFL));
        requestIDs.add(new RequestId(0x00000001L));
        requestIDs.add(new RequestId(0x01234567L));
        requestIDs.add(new RequestId(0x89ABCDEFL));
        requestIDs.add(new RequestId(0xFEDCBA98L));
        requestIDs.add(new RequestId(0x76543210L));
        requestIDs.add(new RequestId(0x15825266L));
        requestIDs.add(new RequestId(0x48120BBEL));
        requestIDs.add(new RequestId(0x25FB7E52L));
        requestIDs.add(new RequestId(0xB2F2546BL));

        builder.setRequestsIds(requestIDs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] { (byte) 0x16, (byte) 0x12, (byte) 0x00, (byte) 0x08, 0, 0, 0, (byte) 0x04 });

        final ClassTypeBuilder builder = new ClassTypeBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setClassType(new ClassType((short) 4));

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testExcludeRouteObject() throws Exception {
        final PCEPExcludeRouteObjectParser parser = new PCEPExcludeRouteObjectParser(this.ctx.getXROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPExcludeRouteObject.1.bin"));

        final XroBuilder builder = new XroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro.Flags(true));
        final List<Subobject> subs = Lists.newArrayList();
        subs.add(new SubobjectBuilder().setMandatory(true).setSubobjectType(
                new IpPrefixCaseBuilder().setIpPrefix(
                        new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("192.168.0.0/16"))).build()).build()).setAttribute(
                                Attribute.Node).build());
        subs.add(new SubobjectBuilder().setMandatory(false).setSubobjectType(
                new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(0x1234L)).build()).build()).build());
        builder.setSubobject(subs);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPPathKeyObjectParser parser = new PCEPPathKeyObjectParser(this.ctx.getEROSubobjectHandlerRegistry());
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPPathKeyObject.bin"));

        final PathKeyBuilder builder = new PathKeyBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        final List<PathKeys> list = Lists.newArrayList();
        list.add(new PathKeysBuilder().setLoose(true).setPathKey(new PathKey(0x1234)).setPceId(
                new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 })).build());
        builder.setPathKeys(list);

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPObjectiveFunctionObjectParser parser = new PCEPObjectiveFunctionObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPObjectiveFunctionObject.1.bin"));

        final OfBuilder builder = new OfBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setCode(new OfId(4));
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.of.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
        final PCEPGlobalConstraintsObjectParser parser = new PCEPGlobalConstraintsObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPGlobalConstraintsObject.1.bin"));

        final GcBuilder builder = new GcBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(false);
        builder.setMaxHop((short) 1);
        builder.setMaxUtilization((short) 0);
        builder.setMinUtilization((short) 100);
        builder.setOverBookingFactor((short) 99);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.gc.TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, false), result.slice(4, result.readableBytes() - 4)));
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
    public void testIgnoreUknownObject() throws PCEPDeserializerException {
        final Object object = this.ctx.getObjectHandlerRegistry().parseObject(35, 1, new ObjectHeaderImpl(false, false), null);
        assertNull(object);
    }

    @Test
    public void testUnrecognizedObjectType() throws PCEPDeserializerException {
        final Object object = this.ctx.getObjectHandlerRegistry().parseObject(2, 2, new ObjectHeaderImpl(true, true), null);
        assertNotNull(object);
        assertTrue(object instanceof UnknownObject);
        assertEquals(PCEPErrors.UNRECOGNIZED_OBJ_TYPE, ((UnknownObject) object).getError());
    }

    @Test
    public void testUnrecognizedObjectClass() throws PCEPDeserializerException {
        final Object object = this.ctx.getObjectHandlerRegistry().parseObject(35, 1, new ObjectHeaderImpl(true, true), null);
        assertNotNull(object);
        assertTrue(object instanceof UnknownObject);
        assertEquals(PCEPErrors.UNRECOGNIZED_OBJ_CLASS, ((UnknownObject) object).getError());
    }

    @Test
    public void testLspaObjectSerializerDefence() throws IOException, PCEPDeserializerException {
        final PCEPLspaObjectParser parser = new PCEPLspaObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspaObject1LowerBounds.bin"));

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
        final Object object = this.ctx.getObjectHandlerRegistry().parseObject(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE, new ObjectHeaderImpl(true, true), Unpooled.EMPTY_BUFFER);
        assertNotNull(object);
        assertTrue(object instanceof Ero);
        final Ero eroObject = (Ero) object;
        assertTrue(eroObject.getSubobject().isEmpty());

        final ByteBuf buffer = Unpooled.buffer();
        this.ctx.getObjectHandlerRegistry().serializeObject(eroObject, buffer);
        final byte[] expected = {0x07, 0x13, 0x00, 0x04};
        assertArrayEquals(expected, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testCloseObjectWithVendorInformationTlv() throws IOException, PCEPDeserializerException {
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
        final PCEPCloseObjectParser parser = new PCEPCloseObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(closeBytes);

        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationTlv viTlv = new VendorInformationTlvBuilder().setEnterpriseNumber(new EnterpriseNumber(0L))
                .setEnterpriseSpecificInformation(esInfo).build();
        final CCloseBuilder builder = new CCloseBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setReason((short) 5);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.c.close.TlvsBuilder()
            .setVendorInformationTlv(Lists.newArrayList(viTlv)).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final VendorInformationObject viObj = new VendorInformationObjectBuilder().setEnterpriseNumber(new EnterpriseNumber(0L))
                .setEnterpriseSpecificInformation(esInfo).build();
        final ByteBuf result = Unpooled.wrappedBuffer(viObjBytes);
        result.readerIndex(8);
        final VendorInformationObject o = (VendorInformationObject) parser.parseObject(new ObjectHeaderImpl(false, false), result.readSlice(result.readableBytes()));
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
        final PCEPMonitoringObjectParser parser = new PCEPMonitoringObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final Monitoring monitoring = new MonitoringBuilder().setMonitoringId(16L).setFlags(new Flags(false, false, true, false, false)).setTlvs(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.monitoring.TlvsBuilder().build()).build();
        final ByteBuf result = Unpooled.wrappedBuffer(monitoringBytes);
        assertEquals(monitoring, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final PccIdReq pccIdReq = new PccIdReqBuilder().setIpAddress(new IpAddress(new Ipv4Address("127.0.0.1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final PccIdReq pccIdReq = new PccIdReqBuilder().setIpAddress(new IpAddress(new Ipv6Address("::1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId pceId = new PceIdBuilder().setIpAddress(new IpAddress(
                new Ipv4Address("127.0.0.1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pceId, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId pccIdReq = new PceIdBuilder().setIpAddress(new IpAddress(
                new Ipv6Address("::1"))).build();
        final ByteBuf result = Unpooled.wrappedBuffer(pccIdReqBytes);
        assertEquals(pccIdReq, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
            .setAverageProcTime(4L)
            .setCurrentProcTime(1L)
            .setMaxProcTime(3L)
            .setMinProcTime(2L)
            .setVarianceProcTime(5L).build();
        final ByteBuf result = Unpooled.wrappedBuffer(proctimeBytes);
        assertEquals(procTime, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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
        final Overload overload = new OverloadBuilder().setDuration(120).build();
        final ByteBuf result = Unpooled.wrappedBuffer(overloadBytes);
        assertEquals(overload, parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));

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

        final PCEPRequestParameterObjectParser parser = new PCEPRequestParameterObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final RpBuilder builder = new RpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setReoptimization(true);
        builder.setBiDirectional(false);
        builder.setLoose(true);
        builder.setMakeBeforeBreak(true);
        builder.setOrder(false);
        builder.setPathKey(false);
        builder.setSupplyOf(false);
        builder.setFragmentation(false);
        builder.setP2mp(false);
        builder.setEroCompression(false);
        builder.setPriority((short) 5);
        builder.setRequestId(new RequestId(0xdeadbeefL));
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder().setPathSetupType(
                new PathSetupTypeBuilder().setPst((short) 0).build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(rpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }
}
