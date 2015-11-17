/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.api.handlers;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecTypeRegistry;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;

public final class FSNlriHandler extends FlowspecHandler {

    protected static final int DESTINATION_PREFIX_VALUE = 1;

    public FSNlriHandler(final SimpleFlowspecTypeRegistry registry) {
        super(registry);
    }

    public void serializeFSNlri(List<Flowspec> fss, ByteBuf output) {
        for (final Flowspec fs : fss) {
            serializeFlowspec(fs, output);
        }
    }

    public List<Flowspec> parseFSNlri(ByteBuf buffer) {
        final List<Flowspec> fss = new ArrayList<>();
        while ( buffer.isReadable() ) {
            fss.add(parseFlowspec(buffer));
        }
        return fss;
    }
}
