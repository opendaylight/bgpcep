/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.TE.AdminStatusObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.AssociationObjectParserType1;
import org.opendaylight.protocol.bgp.parser.impl.TE.AssociationObjectParserType2;
import org.opendaylight.protocol.bgp.parser.impl.TE.AttributesObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.BandwidthObjectType1Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.BandwidthObjectType2Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.DetourObjectType7Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.DetourObjectType8Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.ExcludeRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.ExplicitRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.FastRerouteObjectType1Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.FastRerouteObjectType7Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.FlowSpecObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.MetricObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.PrimaryPathRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.ProtectionObjectType1Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.ProtectionObjectType2Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.RecordRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.RequiredAttributesObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.SecondaryExplicitRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.SecondaryRecordRouteObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.SenderTspecObjectParser;
import org.opendaylight.protocol.bgp.parser.impl.TE.SessionAttributeObjectType1Parser;
import org.opendaylight.protocol.bgp.parser.impl.TE.SessionAttributeObjectType7Parser;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.RsvpTeObject;

public class TEObjectTest {
    BGPRsvpActivator act;
    BGPExtensionProviderContext context;

    @Before
    public void setUp() {
        act = new BGPRsvpActivator();
        context = new SimpleBGPExtensionProviderContext();
        act.start(context);
    }

    @Test
    public void testAdminStatusObjectParser() throws BGPParsingException {
        final AdminStatusObjectParser admParser = new AdminStatusObjectParser();
        final RsvpTeObject obj = admParser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ADMIN_STATUS, 4, TEObjectUtil.TE_LSP_ADMIN_STATUS.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        admParser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ADMIN_STATUS, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAssociationObjectParser1() throws BGPParsingException {
        final AssociationObjectParserType1 parser = new AssociationObjectParserType1();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_1, 4, TEObjectUtil.TE_LSP_ASSOCIATION_1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAssociationObjectParser2() throws BGPParsingException {
        final AssociationObjectParserType2 parser = new AssociationObjectParserType2();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_2, 4, TEObjectUtil.TE_LSP_ASSOCIATION_2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testBandwidthObjectParser1() throws BGPParsingException {
        final BandwidthObjectType1Parser parser = new BandwidthObjectType1Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_1, 4, TEObjectUtil.TE_LSP_BANDWIDTH_1
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testBandwidthObjectParser2() throws BGPParsingException {
        final BandwidthObjectType2Parser parser = new BandwidthObjectType2Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_2, 4, TEObjectUtil.TE_LSP_BANDWIDTH_2
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testExcludeRouteParser() throws BGPParsingException {
        final ExcludeRouteObjectParser parser = new ExcludeRouteObjectParser(context.getXROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, 4, TEObjectUtil.TE_LSP_EXCLUDE_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testExplicitRouteParser() throws BGPParsingException {
        final ExplicitRouteObjectParser parser = new ExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(ByteArray.subByte(TEObjectUtil.TE_LSP_EXPLICIT, 4,
            TEObjectUtil.TE_LSP_EXPLICIT.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAttributesObject12Parser() throws BGPParsingException {
        final AttributesObjectParser parser = new AttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ATTRIBUTES, 4, TEObjectUtil.TE_LSP_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    public void testRequiredAttributesParser() throws BGPParsingException {
        final RequiredAttributesObjectParser parser = new RequiredAttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, 4, TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    public void testPrimaryPathRouteParser() throws BGPParsingException {
        final PrimaryPathRouteObjectParser parser = new PrimaryPathRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, 4, TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, ByteArray.getAllBytes(output));
    }


    @Test
    public void testProtectionObjectParser1() throws BGPParsingException {
        final ProtectionObjectType1Parser parser = new ProtectionObjectType1Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C1, 4, TEObjectUtil.TE_LSP_PROTECTION_C1.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testProtectionObjectParser2() throws BGPParsingException {
        final ProtectionObjectType2Parser parser = new ProtectionObjectType2Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C2, 4, TEObjectUtil.TE_LSP_PROTECTION_C2.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testRecordRouteParser() throws BGPParsingException {
        final RecordRouteObjectParser parser = new RecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_RECORD_ROUTE, 4, TEObjectUtil.TE_LSP_RECORD_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSecondaryExplicitRouteParser() throws BGPParsingException {
        final SecondaryExplicitRouteObjectParser parser = new SecondaryExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, 4, TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSecondaryRecordRouteObjectParser() throws BGPParsingException {
        final SecondaryRecordRouteObjectParser parser = new SecondaryRecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, 4, TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testDetourObjectParser7() throws BGPParsingException {
        final DetourObjectType7Parser parser = new DetourObjectType7Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR7, 4, TEObjectUtil.TE_LSP_DETOUR7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR7, ByteArray.getAllBytes(output));
    }

    @Test
    public void testDetourObjectParser8() throws BGPParsingException {
        final DetourObjectType8Parser parser = new DetourObjectType8Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR8, 4, TEObjectUtil.TE_LSP_DETOUR8.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR8, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFastRerouteObjectParser1() throws BGPParsingException {
        final FastRerouteObjectType1Parser parser = new FastRerouteObjectType1Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE1, 4, TEObjectUtil.TE_LSP_FAST_REROUTE1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFastRerouteObjectParser7() throws BGPParsingException {
        final FastRerouteObjectType7Parser parser = new FastRerouteObjectType7Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE7, 4, TEObjectUtil.TE_LSP_FAST_REROUTE7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE7, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFlowSpecObjectParser_HEADER_5() throws BGPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H5, 4, TEObjectUtil.TE_LSP_FLOWSPEC_H5.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H5, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFlowSpecObjectParser_HEADER_2() throws BGPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H2, 4, TEObjectUtil.TE_LSP_FLOWSPEC_H2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testMetricObjectParser() throws BGPParsingException {
        final MetricObjectParser parser = new MetricObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_METRIC, 4, TEObjectUtil.TE_LSP_METRIC.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_METRIC, ByteArray.getAllBytes(output));
    }

    @Test
    public void testParser_HEADER_5() throws BGPParsingException {
        final SenderTspecObjectParser parser = new SenderTspecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SENDER_TSPEC, 4, TEObjectUtil.TE_LSP_SENDER_TSPEC.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SENDER_TSPEC, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSessionAttributeParser1() throws BGPParsingException {
        final SessionAttributeObjectType1Parser parser = new SessionAttributeObjectType1Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C1, 4, TEObjectUtil.TE_LSP_SESSION_C1
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSessionAttributeParser7() throws BGPParsingException {
        final SessionAttributeObjectType7Parser parser = new SessionAttributeObjectType7Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C7, 4, TEObjectUtil.TE_LSP_SESSION_C7
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C7, ByteArray.getAllBytes(output));
    }
}