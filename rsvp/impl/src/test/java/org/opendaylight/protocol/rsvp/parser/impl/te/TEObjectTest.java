/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.impl.te;

import static org.junit.Assert.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.rsvp.parser.impl.RSVPActivator;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SEROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SERODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRRODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.secondary.explicit.route.object.subobject.container.subobject.type.DynamicControlProtectionCaseBuilder;

public class TEObjectTest {
    RSVPActivator act;
    SimpleRSVPExtensionProviderContext context;

    @Before
    public void setUp() {
        act = new RSVPActivator();
        context = new SimpleRSVPExtensionProviderContext();
        act.start(context);
    }

    @Test
    public void testAdminStatusObjectParser() throws RSVPParsingException {
        final AdminStatusObjectParser admParser = new AdminStatusObjectParser();
        final RsvpTeObject obj = admParser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ADMIN_STATUS, 4, TEObjectUtil.TE_LSP_ADMIN_STATUS.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        admParser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ADMIN_STATUS, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAssociationObjectParser1() throws RSVPParsingException {
        final AssociationObjectParserIPV4 parser = new AssociationObjectParserIPV4();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_1, 4, TEObjectUtil.TE_LSP_ASSOCIATION_1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAssociationObjectParser2() throws RSVPParsingException {
        final AssociationObjectParserIPV6 parser = new AssociationObjectParserIPV6();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_2, 4, TEObjectUtil.TE_LSP_ASSOCIATION_2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testBandwidthObjectParser1() throws RSVPParsingException {
        final BandwidthObjectParser parser = new BandwidthObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_1, 4, TEObjectUtil.TE_LSP_BANDWIDTH_1
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testBandwidthObjectParser2() throws RSVPParsingException {
        final ReoptimizationBandwidthObjectParser parser = new ReoptimizationBandwidthObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_2, 4, TEObjectUtil.TE_LSP_BANDWIDTH_2
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testExcludeRouteParser() throws RSVPParsingException {
        final ExcludeRouteObjectParser parser = new ExcludeRouteObjectParser(context.getXROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, 4, TEObjectUtil.TE_LSP_EXCLUDE_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testExplicitRouteParser() throws RSVPParsingException {
        final ExplicitRouteObjectParser parser = new ExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(ByteArray.subByte(TEObjectUtil.TE_LSP_EXPLICIT, 4,
            TEObjectUtil.TE_LSP_EXPLICIT.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    public void testAttributesObject12Parser() throws RSVPParsingException {
        final AttributesObjectParser parser = new AttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ATTRIBUTES, 4, TEObjectUtil.TE_LSP_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    public void testRequiredAttributesParser() throws RSVPParsingException {
        final RequiredAttributesObjectParser parser = new RequiredAttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, 4, TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    public void testPrimaryPathRouteParser() throws RSVPParsingException {
        final PrimaryPathRouteObjectParser parser = new PrimaryPathRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, 4, TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, ByteArray.getAllBytes(output));
    }


    @Test
    public void testProtectionObjectParser1() throws RSVPParsingException {
        final ProtectionObjectParser parser = new ProtectionObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C1, 4, TEObjectUtil.TE_LSP_PROTECTION_C1.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testProtectionObjectParser2() throws RSVPParsingException {
        final DynamicProtectionObjectParser parser = new DynamicProtectionObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C2, 4, TEObjectUtil.TE_LSP_PROTECTION_C2.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testProtectionCommonParser() {
        final ByteBuf emptyBuff = Unpooled.buffer();
        try {
            ProtectionCommonParser.parseCommonProtectionBodyType2(emptyBuff);
            Assert.fail();
        } catch (final RSVPParsingException e) {
            Assert.assertEquals("Wrong length of array of bytes. Passed: " + emptyBuff.readableBytes() + "; Expected:" +
                " " + ProtectionCommonParser.CONTENT_LENGTH_C2 + ".", e.getMessage());
        }
    }

    @Test
    public void testRecordRouteParser() throws RSVPParsingException {
        final RecordRouteObjectParser parser = new RecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_RECORD_ROUTE, 4, TEObjectUtil.TE_LSP_RECORD_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSecondaryExplicitRouteParser() throws RSVPParsingException {
        final SecondaryExplicitRouteObjectParser parser = new SecondaryExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, 4, TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSecondaryRecordRouteObjectParser() throws RSVPParsingException {
        final SecondaryRecordRouteObjectParser parser = new SecondaryRecordRouteObjectParser(context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, 4, TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSSRODynamicProtectionSubobjectParser() throws RSVPParsingException {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final SRRODynamicProtectionSubobjectParser dynamicParser = new SRRODynamicProtectionSubobjectParser();
        final SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION, 2, TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION
                .length - 2)));

        final ByteBuf output = Unpooled.buffer();
        dynamicParser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSSROBasicProtectionSubobjectParser() throws RSVPParsingException {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION, 2, TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION.length - 2)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongParseSRRO() throws RSVPParsingException {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        parser.parseSubobject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongSerializeSRRO() throws RSVPParsingException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route
            .object.secondary.record.route.object.subobject.container.subobject.type
            .DynamicControlProtectionCase dynamicProtection = new org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.record.route.object.secondary.record.route
            .object.subobject.container.subobject.type.DynamicControlProtectionCaseBuilder().build();
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.
            SubobjectContainer subContainer = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder().setSubobjectType(dynamicProtection).build();
        parser.serializeSubobject(subContainer, Unpooled.buffer());
    }

    @Test
    public void testSERODynamicProtectionSubobjectParser() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        final SERODynamicProtectionSubobjectParser dynamicParser = new SERODynamicProtectionSubobjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.
            SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION, 2, TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION.length - 2)), true);

        final ByteBuf output = Unpooled.buffer();
        dynamicParser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongParseSERO() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        parser.parseSubobject(null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongSerializeSERO() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        parser.serializeSubobject(new SubobjectContainerBuilder().setSubobjectType
            (new DynamicControlProtectionCaseBuilder().build()).build(), Unpooled.buffer());
    }

    @Test
    public void testSEROBasicProtectionSubobjectParser() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.
            SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION, 2, TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION.length - 2)), true);

        final ByteBuf output = Unpooled.buffer();
        parser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    public void testDetourObjectParser7() throws RSVPParsingException {
        final DetourObjectIpv4Parser parser = new DetourObjectIpv4Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR7, 4, TEObjectUtil.TE_LSP_DETOUR7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR7, ByteArray.getAllBytes(output));
    }

    @Test
    public void testDetourObjectParser8() throws RSVPParsingException {
        final DetourObjectIpv6Parser parser = new DetourObjectIpv6Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR8, 4, TEObjectUtil.TE_LSP_DETOUR8.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR8, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFastRerouteObjectParser1() throws RSVPParsingException {
        final FastRerouteObjectParser parser = new FastRerouteObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE1, 4, TEObjectUtil.TE_LSP_FAST_REROUTE1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFastRerouteObjectParser7() throws RSVPParsingException {
        final InformationalFastRerouteObjectParser parser = new InformationalFastRerouteObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE7, 4, TEObjectUtil.TE_LSP_FAST_REROUTE7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE7, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFlowSpecObjectParser_HEADER_5() throws RSVPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H5, 4, TEObjectUtil.TE_LSP_FLOWSPEC_H5.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H5, ByteArray.getAllBytes(output));
    }

    @Test
    public void testFlowSpecObjectParser_HEADER_2() throws RSVPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H2, 4, TEObjectUtil.TE_LSP_FLOWSPEC_H2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H2, ByteArray.getAllBytes(output));
    }

    @Test
    public void testParser_HEADER_5() throws RSVPParsingException {
        final SenderTspecObjectParser parser = new SenderTspecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SENDER_TSPEC, 4, TEObjectUtil.TE_LSP_SENDER_TSPEC.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SENDER_TSPEC, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSessionAttributeParser1() throws RSVPParsingException {
        final SessionAttributeLspRaObjectParser parser = new SessionAttributeLspRaObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C1, 4, TEObjectUtil.TE_LSP_SESSION_C1
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    public void testSessionAttributeParser7() throws RSVPParsingException {
        final SessionAttributeLspObjectParser parser = new SessionAttributeLspObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C7, 4, TEObjectUtil.TE_LSP_SESSION_C7
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C7, ByteArray.getAllBytes(output));
    }
}