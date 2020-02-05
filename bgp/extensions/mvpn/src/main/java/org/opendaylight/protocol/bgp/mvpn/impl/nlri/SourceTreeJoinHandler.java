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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceTreeJoinCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.SourceTreeJoinCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.source.tree.join._case.SourceTreeJoinBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.6.
 *
 * @author Claudio D. Gasparini
 */
public final class SourceTreeJoinHandler extends AbstractMvpnNlri<SourceTreeJoinCase> {
    @Override
    public int getType() {
        return NlriType.SourceTreeJoin.getIntValue();
    }

    @Override

    public SourceTreeJoinCase parseMvpn(final ByteBuf buffer) {
        return new SourceTreeJoinCaseBuilder().setSourceTreeJoin(
                new SourceTreeJoinBuilder().setCMulticast(CMulticastUtil.parseCMulticastGrouping(buffer))
                        .build()).build();
    }

    @Override
    protected ByteBuf serializeBody(final SourceTreeJoinCase mvpn) {
        return CMulticastUtil.serializeCMulticast(mvpn.getSourceTreeJoin().getCMulticast());
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return SourceTreeJoinCase.class;
    }
}
