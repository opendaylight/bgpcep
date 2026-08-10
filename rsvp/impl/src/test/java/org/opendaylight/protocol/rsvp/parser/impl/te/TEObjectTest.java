/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl.te;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.rsvp.parser.impl.RSVPActivator;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SEROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.ero.SERODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRROBasicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.SRRODynamicProtectionSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary._record.route.subobjects.subobject.type.DynamicControlProtectionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.subobjects.subobject.type.DynamicControlProtectionCaseBuilder;

class TEObjectTest {
    private RSVPActivator act;
    private SimpleRSVPExtensionProviderContext context;

    @BeforeEach
    void setUp() {
        this.act = new RSVPActivator();
        this.context = new SimpleRSVPExtensionProviderContext();
        this.act.start(this.context);
    }

    @Test
    void testAdminStatusObjectParser() throws RSVPParsingException {
        final AdminStatusObjectParser admParser = new AdminStatusObjectParser();
        final RsvpTeObject obj = admParser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ADMIN_STATUS, 4,
                TEObjectUtil.TE_LSP_ADMIN_STATUS.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        admParser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ADMIN_STATUS, ByteArray.getAllBytes(output));
    }

    @Test
    void testAssociationObjectParser1() throws RSVPParsingException {
        final AssociationObjectParserIPV4 parser = new AssociationObjectParserIPV4();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_1, 4,
                TEObjectUtil.TE_LSP_ASSOCIATION_1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_1, ByteArray.getAllBytes(output));
    }

    @Test
    void testAssociationObjectParser2() throws RSVPParsingException {
        final AssociationObjectParserIPV6 parser = new AssociationObjectParserIPV6();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ASSOCIATION_2, 4, TEObjectUtil.TE_LSP_ASSOCIATION_2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ASSOCIATION_2, ByteArray.getAllBytes(output));
    }

    @Test
    void testBandwidthObjectParser1() throws RSVPParsingException {
        final BandwidthObjectParser parser = new BandwidthObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_1, 4, TEObjectUtil.TE_LSP_BANDWIDTH_1
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_1, ByteArray.getAllBytes(output));
    }

    @Test
    void testBandwidthObjectParser2() throws RSVPParsingException {
        final ReoptimizationBandwidthObjectParser parser = new ReoptimizationBandwidthObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BANDWIDTH_2, 4, TEObjectUtil.TE_LSP_BANDWIDTH_2
                .length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BANDWIDTH_2, ByteArray.getAllBytes(output));
    }

    @Test
    void testExcludeRouteParser() throws RSVPParsingException {
        final ExcludeRouteObjectParser parser = new ExcludeRouteObjectParser(
            this.context.getXROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, 4,
                TEObjectUtil.TE_LSP_EXCLUDE_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXCLUDE_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    void testExplicitRouteParser() throws RSVPParsingException {
        final ExplicitRouteObjectParser parser = new ExplicitRouteObjectParser(
            this.context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(ByteArray.subByte(
            TEObjectUtil.TE_LSP_EXPLICIT, 4, TEObjectUtil.TE_LSP_EXPLICIT.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    void testAttributesObject12Parser() throws RSVPParsingException {
        final AttributesObjectParser parser = new AttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_ATTRIBUTES, 4,
                TEObjectUtil.TE_LSP_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    void testRequiredAttributesParser() throws RSVPParsingException {
        final RequiredAttributesObjectParser parser = new RequiredAttributesObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, 4,
                TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_REQUIRED_ATTRIBUTES, ByteArray.getAllBytes(output));
    }

    @Test
    void testPrimaryPathRouteParser() throws RSVPParsingException {
        final PrimaryPathRouteObjectParser parser = new PrimaryPathRouteObjectParser(
            this.context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, 4,
                TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PRIMARY_PATH_ROUTE, ByteArray.getAllBytes(output));
    }


    @Test
    void testProtectionObjectParser1() throws RSVPParsingException {
        final ProtectionObjectParser parser = new ProtectionObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C1, 4,
                TEObjectUtil.TE_LSP_PROTECTION_C1.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    void testProtectionObjectParser2() throws RSVPParsingException {
        final DynamicProtectionObjectParser parser = new DynamicProtectionObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_PROTECTION_C2, 4,
                TEObjectUtil.TE_LSP_PROTECTION_C2.length - 4)));
        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_PROTECTION_C2, ByteArray.getAllBytes(output));
    }

    @Test
    void testProtectionCommonParser() {
        final ByteBuf emptyBuff = Unpooled.buffer();
        final var ex = assertThrows(RSVPParsingException.class,
            () -> ProtectionCommonParser.parseCommonProtectionBodyType2(emptyBuff));
        assertEquals("Wrong length of array of bytes. Passed: " + emptyBuff.readableBytes()
            + "; Expected: " + ProtectionCommonParser.CONTENT_LENGTH_C2 + ".", ex.getMessage());
    }

    @Test
    void testRecordRouteParser() throws RSVPParsingException {
        final RecordRouteObjectParser parser = new RecordRouteObjectParser(
            this.context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_RECORD_ROUTE, 4,
                TEObjectUtil.TE_LSP_RECORD_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    void testSecondaryExplicitRouteParser() throws RSVPParsingException {
        final SecondaryExplicitRouteObjectParser parser = new SecondaryExplicitRouteObjectParser(
            this.context.getEROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, 4,
                TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_EXPLICIT, ByteArray.getAllBytes(output));
    }

    @Test
    void testSecondaryRecordRouteObjectParser() throws RSVPParsingException {
        final SecondaryRecordRouteObjectParser parser = new SecondaryRecordRouteObjectParser(
            this.context.getRROSubobjectHandlerRegistry());
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, 4,
                TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SECONDARY_RECORD_ROUTE, ByteArray.getAllBytes(output));
    }

    @Test
    void testSSRODynamicProtectionSubobjectParser() throws RSVPParsingException {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final SRRODynamicProtectionSubobjectParser dynamicParser = new SRRODynamicProtectionSubobjectParser();
        final SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION, 2,
                TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION.length - 2)));

        final ByteBuf output = Unpooled.buffer();
        dynamicParser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DYNAMIC_SRRO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    void testSSROBasicProtectionSubobjectParser() throws RSVPParsingException {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION, 2,
                TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION.length - 2)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BASIC_SRRO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    void testWrongParseSRRO() {
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parseSubobject(null));
    }

    @Test
    void testWrongSerializeSRRO() {
        final DynamicControlProtectionCase dynamicProtection = new org.opendaylight.yang.gen.v1.urn.opendaylight.params
            .xml.ns.yang.rsvp.rev150820.secondary._record.route.subobjects.subobject.type
            .DynamicControlProtectionCaseBuilder().build();
        final SRROBasicProtectionSubobjectParser parser = new SRROBasicProtectionSubobjectParser();
        final var container = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                ._record.route.subobjects.list.SubobjectContainerBuilder()
                .setSubobjectType(dynamicProtection)
                .build();
        assertThrows(IllegalArgumentException.class,
            () -> parser.serializeSubobject(container, Unpooled.buffer()));
    }

    @Test
    void testSERODynamicProtectionSubobjectParser() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        final SERODynamicProtectionSubobjectParser dynamicParser = new SERODynamicProtectionSubobjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects
            .list.SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION, 2,
                TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION.length - 2)), true);

        final ByteBuf output = Unpooled.buffer();
        dynamicParser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DYNAMIC_SERO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    void testWrongParseSERO() {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parseSubobject(null, false));
    }

    @Test
    void testWrongSerializeSERO() {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        final var container = new SubobjectContainerBuilder().setSubobjectType(
            new DynamicControlProtectionCaseBuilder().build()).build();
        assertThrows(IllegalArgumentException.class,
            () -> parser.serializeSubobject(container, Unpooled.buffer()));
    }

    @Test
    void testSEROBasicProtectionSubobjectParser() throws RSVPParsingException {
        final SEROBasicProtectionSubobjectParser parser = new SEROBasicProtectionSubobjectParser();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects
            .list.SubobjectContainer sub = parser.parseSubobject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION, 2,
                TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION.length - 2)), true);

        final ByteBuf output = Unpooled.buffer();
        parser.serializeSubobject(sub, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_BASIC_SERO_PROTECTION, ByteArray.getAllBytes(output));
    }

    @Test
    void testDetourObjectParser7() throws RSVPParsingException {
        final DetourObjectIpv4Parser parser = new DetourObjectIpv4Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR7, 4, TEObjectUtil.TE_LSP_DETOUR7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR7, ByteArray.getAllBytes(output));
    }

    @Test
    void testDetourObjectParser8() throws RSVPParsingException {
        final DetourObjectIpv6Parser parser = new DetourObjectIpv6Parser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_DETOUR8, 4, TEObjectUtil.TE_LSP_DETOUR8.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_DETOUR8, ByteArray.getAllBytes(output));
    }

    @Test
    void testFastRerouteObjectParser1() throws RSVPParsingException {
        final FastRerouteObjectParser parser = new FastRerouteObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE1, 4,
                TEObjectUtil.TE_LSP_FAST_REROUTE1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE1, ByteArray.getAllBytes(output));
    }

    @Test
    void testFastRerouteObjectParser7() throws RSVPParsingException {
        final InformationalFastRerouteObjectParser parser = new InformationalFastRerouteObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FAST_REROUTE7, 4,
                TEObjectUtil.TE_LSP_FAST_REROUTE7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FAST_REROUTE7, ByteArray.getAllBytes(output));
    }

    @Test
    void testFlowSpecObjectParser_HEAD_5() throws RSVPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H5, 4,
                TEObjectUtil.TE_LSP_FLOWSPEC_H5.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H5, ByteArray.getAllBytes(output));
    }

    @Test
    void testFlowSpecObjectParser_HEAD_2() throws RSVPParsingException {
        final FlowSpecObjectParser parser = new FlowSpecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_FLOWSPEC_H2, 4,
                TEObjectUtil.TE_LSP_FLOWSPEC_H2.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_FLOWSPEC_H2, ByteArray.getAllBytes(output));
    }

    @Test
    void testParser_HEAD_5() throws RSVPParsingException {
        final SenderTspecObjectParser parser = new SenderTspecObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SENDER_TSPEC, 4,
                TEObjectUtil.TE_LSP_SENDER_TSPEC.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SENDER_TSPEC, ByteArray.getAllBytes(output));
    }

    @Test
    void testSessionAttributeParser1() throws RSVPParsingException {
        final SessionAttributeLspRaObjectParser parser = new SessionAttributeLspRaObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C1, 4,
                TEObjectUtil.TE_LSP_SESSION_C1.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C1, ByteArray.getAllBytes(output));
    }

    @Test
    void testSessionAttributeParser7() throws RSVPParsingException {
        final SessionAttributeLspObjectParser parser = new SessionAttributeLspObjectParser();
        final RsvpTeObject obj = parser.parseObject(Unpooled.copiedBuffer(
            ByteArray.subByte(TEObjectUtil.TE_LSP_SESSION_C7, 4,
                TEObjectUtil.TE_LSP_SESSION_C7.length - 4)));

        final ByteBuf output = Unpooled.buffer();
        parser.serializeObject(obj, output);
        assertArrayEquals(TEObjectUtil.TE_LSP_SESSION_C7, ByteArray.getAllBytes(output));
    }
}
