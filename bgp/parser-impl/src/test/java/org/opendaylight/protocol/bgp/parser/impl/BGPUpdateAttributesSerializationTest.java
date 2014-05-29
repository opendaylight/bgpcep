/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv6NextHopCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPUpdateAttributesSerializationTest {

    private static final Logger LOG = LoggerFactory.getLogger(BGPUpdateAttributesSerializationTest.class);
    static final List<byte[]> inputBytes = new ArrayList<byte[]>();
    private static BGPUpdateMessageParser updateParser = new BGPUpdateMessageParser(ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry());

    private static int COUNTER = 9;//17;
    private static int MAX_SIZE = 300;
    private ByteBuf byteAggregator;



    @Before
    public void setupUpdateMessage() throws Exception{

        for (int i = 1; i <= COUNTER; i++) {
            final String name = "/up" + i + ".bin";
            final InputStream is = BGPParserTest.class.getResourceAsStream(name);
            if (is == null) {
                throw new IOException("Failed to get resource " + name);
            }

            final ByteArrayOutputStream bis = new ByteArrayOutputStream();
            final byte[] data = new byte[MAX_SIZE];
            int nRead = 0;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                bis.write(data, 0, nRead);
            }
            bis.flush();

            inputBytes.add(bis.toByteArray());
        }


    }

    private Update readUpdateMessageFromList (int listIndex) throws BGPDocumentedException {
        return readUpdateMessageBytes(Unpooled.copiedBuffer(inputBytes.get(listIndex)));
    }
    private Update readUpdateMessageBytes(ByteBuf messageBytes) throws BGPDocumentedException {
        final byte[] body = ByteArray.cutBytes(ByteArray.getAllBytes(messageBytes), MessageUtil.COMMON_HEADER_LENGTH);
        final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(ByteArray.getAllBytes(messageBytes), MessageUtil.MARKER_LENGTH,
                MessageUtil.LENGTH_FIELD_LENGTH));
        return BGPUpdateAttributesSerializationTest.updateParser.parseMessageBody(Unpooled.copiedBuffer(body), messageLength);
    }

    private void assertEqualsPathAttributes(PathAttributes left, PathAttributes right){
        if (left.getCNextHop()!=null) {
            assertEqualsNextHop(left.getCNextHop(), right.getCNextHop());
        }
        if (left.getAsPath()!=null) {
            assertEqualsAsPath(left.getAsPath(), right.getAsPath());
        }
        if (left.getExtendedCommunities()!=null) {
            assertEqualsExtendedCommunities(left.getExtendedCommunities(), right.getExtendedCommunities());
        }
        if (left.getCommunities()!=null) {
            assertEqualsCommunities(left.getCommunities(), right.getCommunities());
        }
        if (left.getAggregator()!=null){
            assertEquals(left.getAggregator().getAsNumber().getValue(),right.getAggregator().getAsNumber().getValue());
            assertEquals(left.getAggregator().getNetworkAddress().getValue(), right.getAggregator().getNetworkAddress().getValue());
        }
        if (left.getAtomicAggregate()!=null){
            assertEquals(left.getAtomicAggregate() != null, right.getAtomicAggregate() != null);
        }
        if (left.getClusterId()!=null){
            assertEqualsClusterId(left.getClusterId(), right.getClusterId());
        }
        if (left.getLocalPref()!=null){
            assertEquals(left.getLocalPref().getPref(), right.getLocalPref().getPref());
        }
        if (left.getMultiExitDisc()!=null){
            assertEquals(left.getMultiExitDisc().getMed(),right.getMultiExitDisc().getMed());
        }
        if (left.getOrigin()!=null){
            assertEquals(left.getOrigin().getValue().getIntValue(), right.getOrigin().getValue().getIntValue());
        }
        if (left.getOriginatorId()!=null){
            assertEquals(left.getOriginatorId().getValue(), right.getOriginatorId().getValue());
        }
    }

    private void assertEqualsClusterId(List<ClusterIdentifier> left,List<ClusterIdentifier> right){
        assertEquals(left.size(),right.size());
        for (ClusterIdentifier clusterIdentifier:left){
            right.remove(clusterIdentifier);
        }
        assertEquals(right.size(),0);
    }
    private void assertEqualsCommunities(List<Communities> left,List<Communities> right){
        assertEquals(left.size(),right.size());
        for (Communities communities:left){
            right.remove(communities);
        }
        assertEquals(right.size(),0);
    }
    private void assertEqualsExtendedCommunities(List<ExtendedCommunities> left, List<ExtendedCommunities> right){
        assertEquals(left.size(), right.size());
        for (ExtendedCommunity extendedCommunity:left){
            right.remove(extendedCommunity);
        }
        assertEquals(right.size(),0);
    }
    private void assertEqualsAsPath(AsPath left, AsPath right){
        for (Segments segments:left.getSegments()){
            right.getSegments().remove(segments);
        }
        assertTrue(right.getSegments().size() == 0);
    }
    private void assertEqualsNextHop(CNextHop left,CNextHop right){
        if (left instanceof Ipv4NextHopCase) {
            assertTrue(left instanceof Ipv4NextHopCase && right instanceof Ipv4NextHopCase);
            Ipv4NextHopCase leftIpv4NextHopCase = (Ipv4NextHopCase) left;
            Ipv4NextHopCase rightIpv4NextHopCase = (Ipv4NextHopCase) right;
            assertEquals(leftIpv4NextHopCase.getIpv4NextHop().getGlobal().getValue(), rightIpv4NextHopCase.getIpv4NextHop().getGlobal().getValue());
        }
        if (left instanceof Ipv6NextHopCase) {
            assertTrue(left instanceof Ipv6NextHopCase && right instanceof Ipv6NextHopCase);
            Ipv6NextHopCase leftIpv6NextHopCase = (Ipv6NextHopCase) left;
            Ipv6NextHopCase rightIpv6NextHopCase = (Ipv6NextHopCase) right;
            assertEquals(leftIpv6NextHopCase.getIpv6NextHop().getGlobal().getValue(), rightIpv6NextHopCase.getIpv6NextHop().getGlobal().getValue());
            assertEquals(leftIpv6NextHopCase.getIpv6NextHop().getLinkLocal().getValue(),rightIpv6NextHopCase.getIpv6NextHop().getLinkLocal().getValue());
        }
    }
    private void assertEqualsNlri(Nlri left, Nlri right){
        assertEquals(left.getNlri().size(), right.getNlri().size());
        for (Ipv4Prefix ipv4Prefix: left.getNlri()){
            right.getNlri().remove(ipv4Prefix);
        }
        assertEquals(right.getNlri().size(), 0);
    }
    private void assertWithdrawnRoutes(WithdrawnRoutes left,WithdrawnRoutes right){
        assertEquals(left.getWithdrawnRoutes().size(), right.getWithdrawnRoutes().size());
        for (Ipv4Prefix ipv4Prefix:left.getWithdrawnRoutes()){
            right.getWithdrawnRoutes().remove(ipv4Prefix);
        }
        assertEquals(right.getWithdrawnRoutes().size(),0);
    }
    @Test
    public void testUpdateMessageSerialization() throws BGPDocumentedException {
        for (int i = 0; i < COUNTER; i++) {
            Update originalMessage = readUpdateMessageFromList(i);
            this.byteAggregator = updateParser.serializeMessage(originalMessage);
            Update serializedMessage = readUpdateMessageBytes(BGPUpdateAttributesSerializationTest.updateParser.serializeMessage(originalMessage));
            if (originalMessage.getNlri()!=null) {
                assertEqualsNlri(originalMessage.getNlri(), serializedMessage.getNlri());
            }
            if (originalMessage.getPathAttributes()!=null) {
                assertEqualsPathAttributes(originalMessage.getPathAttributes(), serializedMessage.getPathAttributes());
            }
            if (originalMessage.getWithdrawnRoutes()!=null){
                assertWithdrawnRoutes(originalMessage.getWithdrawnRoutes(),serializedMessage.getWithdrawnRoutes());
            }
        }
    }
}
