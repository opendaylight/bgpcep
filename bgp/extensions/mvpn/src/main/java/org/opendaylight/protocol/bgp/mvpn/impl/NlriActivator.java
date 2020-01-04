/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl;

import java.util.List;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.InterASIPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.IntraAsIPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.LeafADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SPmsiADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SharedTreeJoinHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SourceActiveADHandler;
import org.opendaylight.protocol.bgp.mvpn.impl.nlri.SourceTreeJoinHandler;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri.SimpleMvpnNlriRegistry;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Nlri Registry activator.
 *
 * @author Claudio D. Gasparini
 */
public final class NlriActivator {
    private NlriActivator() {
        // Hidden on purpose
    }

    public static void registerNlriParsers(final List<Registration> regs) {
        final SimpleMvpnNlriRegistry nlriRegistry = SimpleMvpnNlriRegistry.getInstance();

        final IntraAsIPmsiADHandler intra = new IntraAsIPmsiADHandler();
        regs.add(nlriRegistry.registerNlriParser(intra));
        regs.add(nlriRegistry.registerNlriSerializer(intra));

        final InterASIPmsiADHandler inter = new InterASIPmsiADHandler();
        regs.add(nlriRegistry.registerNlriParser(inter));
        regs.add(nlriRegistry.registerNlriSerializer(inter));

        final SPmsiADHandler sPmsiADHandler = new SPmsiADHandler();
        regs.add(nlriRegistry.registerNlriParser(sPmsiADHandler));
        regs.add(nlriRegistry.registerNlriSerializer(sPmsiADHandler));

        final LeafADHandler leafHandler = new LeafADHandler();
        regs.add(nlriRegistry.registerNlriParser(leafHandler));
        regs.add(nlriRegistry.registerNlriSerializer(leafHandler));

        final SourceActiveADHandler activeADHandler = new SourceActiveADHandler();
        regs.add(nlriRegistry.registerNlriParser(activeADHandler));
        regs.add(nlriRegistry.registerNlriSerializer(activeADHandler));

        final SharedTreeJoinHandler sharedTreeJoinHandler = new SharedTreeJoinHandler();
        regs.add(nlriRegistry.registerNlriParser(sharedTreeJoinHandler));
        regs.add(nlriRegistry.registerNlriSerializer(sharedTreeJoinHandler));

        final SourceTreeJoinHandler sourceTreeJoinHandler = new SourceTreeJoinHandler();
        regs.add(nlriRegistry.registerNlriParser(sourceTreeJoinHandler));
        regs.add(nlriRegistry.registerNlriSerializer(sourceTreeJoinHandler));
    }
}
