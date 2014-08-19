/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public class ParserToSalTest extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {

    private Pcrpt rptmsg;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final List<Reports> reports = Lists.newArrayList(new ReportsBuilder().setPath(new PathBuilder().setEro(new EroBuilder().build()).build()).setLsp(
                new LspBuilder().setPlspId(new PlspId(5L)).setSync(false).setRemove(false).setTlvs(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1L)).build()).setSymbolicPathName(
                                new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(new byte[] { 22, 34 })).build()).build()).build()).build());
        this.rptmsg = new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();

        assertTrue(getTopology().isPresent());
    }

    @Test
    public void testUnknownLsp() throws Exception {
        this.session.sessionUp();
        this.session.handleMessage(this.rptmsg);
        final Topology topology = getTopology().get();
        assertFalse(topology.getNode().isEmpty());
    }
}
