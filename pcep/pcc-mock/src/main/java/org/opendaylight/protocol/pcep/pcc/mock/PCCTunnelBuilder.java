/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.pcep.pcc.mock.api.LspType;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;

final class PCCTunnelBuilder {
    private static final Subobject DEFAULT_ENDPOINT_HOP = getDefaultEROEndpointHop();
    private static final String ENDPOINT_ADDRESS = "1.1.1.1";
    private static final String ENDPOINT_PREFIX = ENDPOINT_ADDRESS + "/32";
    static final int PCC_DELEGATION = -1;

    private PCCTunnelBuilder() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static Map<PlspId, PCCTunnel> createTunnels(final String address, final int lsps) {
        final Map<PlspId, PCCTunnel> tunnels = new HashMap<>();
        for (int i = 1; i <= lsps; i++) {
            final PCCTunnel tunnel = new PCCTunnel(MsgBuilderUtil.getDefaultPathName(address, i),
                    PCC_DELEGATION, LspType.PCC_LSP, createPath(Lists.newArrayList(DEFAULT_ENDPOINT_HOP)));
            tunnels.put(new PlspId((long) i), tunnel);
        }
        return tunnels;
    }

    private static Subobject getDefaultEROEndpointHop() {
        final SubobjectBuilder builder = new SubobjectBuilder();
        builder.setLoose(false);
        builder.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(new IpPrefixBuilder().setIpPrefix(
            new IpPrefix(new Ipv4Prefix(ENDPOINT_PREFIX))).build()).build());
        return builder.build();
    }
}
