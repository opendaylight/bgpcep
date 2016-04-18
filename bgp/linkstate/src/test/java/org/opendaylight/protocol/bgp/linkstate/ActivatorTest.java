/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opendaylight.protocol.bgp.linkstate.nlri.LinkNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.NodeNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.PrefixNlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.SimpleNlriTypeRegistry;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv4NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspIpv6NlriParser;
import org.opendaylight.protocol.bgp.linkstate.nlri.TeLspNlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.ServiceLoaderRSVPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;

public class ActivatorTest {

    @Test
    public void testActivator() throws Exception {
        final BGPActivator act = new BGPActivator(true, ServiceLoaderRSVPExtensionProviderContext.getSingletonInstance().getRsvpRegistry());
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        final SimpleNlriTypeRegistry typeReg = SimpleNlriTypeRegistry.getInstance();

        assertNull(context.getAddressFamilyRegistry().classForFamily(16388));
        assertNull(context.getSubsequentAddressFamilyRegistry().classForFamily(71));

        act.start(context);

        assertEquals(LinkstateAddressFamily.class, context.getAddressFamilyRegistry().classForFamily(16388));
        assertEquals(LinkstateSubsequentAddressFamily.class, context.getSubsequentAddressFamilyRegistry().classForFamily(71));
        assertTrue(typeReg.getParser(NlriType.Node) instanceof NodeNlriParser);
        assertTrue(typeReg.getParser(NlriType.Link) instanceof LinkNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv4Prefix) instanceof PrefixNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv6Prefix) instanceof PrefixNlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv4TeLsp) instanceof TeLspIpv4NlriParser);
        assertTrue(typeReg.getParser(NlriType.Ipv6TeLsp) instanceof TeLspIpv6NlriParser);
        assertTrue(typeReg.getSerializer(NodeCase.class) instanceof NodeNlriParser);
        assertTrue(typeReg.getSerializer(LinkCase.class) instanceof LinkNlriParser);
        assertTrue(typeReg.getSerializer(PrefixCase.class) instanceof PrefixNlriParser);
        assertTrue(typeReg.getSerializer(TeLspCase.class) instanceof TeLspNlriSerializer);

        act.close();
    }

    @Test
    public void testRIBActivator() {
        final RIBActivator ribAct = new RIBActivator();
        final RIBExtensionProviderContext context = new SimpleRIBExtensionProviderContext();

        assertNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.startRIBExtensionProvider(context);

        assertNotNull(context.getRIBSupport(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class));

        ribAct.close();
    }
}
