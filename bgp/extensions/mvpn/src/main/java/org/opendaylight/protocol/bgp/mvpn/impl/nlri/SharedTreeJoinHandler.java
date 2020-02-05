/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SharedTreeJoinCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SharedTreeJoinCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.shared.tree.join._case.SharedTreeJoinBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.6.
 *
 * @author Claudio D. Gasparini
 */
public final class SharedTreeJoinHandler extends AbstractMvpnNlri<SharedTreeJoinCase> {
    @Override
    public int getType() {
        return NlriType.SharedTreeJoin.getIntValue();
    }

    @Override
    public SharedTreeJoinCase parseMvpn(final ByteBuf buffer) {
        return new SharedTreeJoinCaseBuilder().setSharedTreeJoin(new SharedTreeJoinBuilder()
                .setCMulticast(CMulticastUtil.parseCMulticastGrouping(buffer))
                .build()).build();
    }

    @Override
    protected ByteBuf serializeBody(final SharedTreeJoinCase mvpn) {
        return CMulticastUtil.serializeCMulticast(mvpn.getSharedTreeJoin().getCMulticast());
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return SharedTreeJoinCase.class;
    }
}
