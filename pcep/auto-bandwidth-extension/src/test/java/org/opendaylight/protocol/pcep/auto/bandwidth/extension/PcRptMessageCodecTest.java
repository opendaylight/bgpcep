/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.impl.Activator;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev160109.Bandwidth1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev160109.bandwidth.usage.object.BandwidthUsage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.auto.bandwidth.rev160109.bandwidth.usage.object.BandwidthUsageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;

public class PcRptMessageCodecTest {

    private SimplePCEPExtensionProviderContext ctx;
    private Activator act;
    private StatefulActivator statefulAct;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new Activator();
        this.act.start(this.ctx);
        this.statefulAct = new StatefulActivator();
        this.statefulAct.start(this.ctx);
    }

    @After
    public void tearDown() {
        this.act.stop();
        this.statefulAct.stop();
    }

    @Test
    public void testGetValidReportsPositive() {
        final PcRptMessageCodec codec = new PcRptMessageCodec(this.ctx.getObjectHandlerRegistry());
        final BandwidthUsage bw = new BandwidthUsageBuilder().setBwSample(Lists.newArrayList(new Bandwidth(new byte[] {0, 0, 0, 1}))).build();
        final Lsp lsp = new LspBuilder().build();
        final Ero ero = new EroBuilder().build();
        final List<Object> objects = Lists.<Object>newArrayList(lsp, ero, bw);
        final Reports validReports = codec.getValidReports(objects, Collections.<Message>emptyList());
        assertNotNull(validReports.getPath().getBandwidth().getAugmentation(Bandwidth1.class));
        assertTrue(objects.isEmpty());
    }

}
