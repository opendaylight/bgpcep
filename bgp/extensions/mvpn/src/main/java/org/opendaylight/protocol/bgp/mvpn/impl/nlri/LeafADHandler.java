/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.IpAddressUtil;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri.SimpleMvpnNlriRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.LeafAD;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.LeafADBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.LeafADRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.leaf.a.d.route.key.InterAsIPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.leaf.a.d.route.key.InterAsIPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.leaf.a.d.route.key.SPmsiADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.leaf.a.d.grouping.leaf.a.d.leaf.a.d.route.key.SPmsiADCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.MvpnChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.LeafADCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn.mvpn.choice.LeafADCaseBuilder;

/**
 * https://tools.ietf.org/html/rfc6514#section-4.4.
 *
 * @author Claudio D. Gasparini
 */
public final class LeafADHandler extends AbstractMvpnNlri<LeafADCase> {
    @Override
    public int getType() {
        return NlriType.LeafAD.getIntValue();
    }

    @Override
    public LeafADCase parseMvpn(final ByteBuf buffer) {
        final NlriType type = NlriType.forValue(buffer.readUnsignedByte());
        final short length = buffer.readUnsignedByte();
        final MvpnChoice key = SimpleMvpnNlriRegistry.getInstance().parseMvpn(type, buffer.readBytes(length));
        final LeafADRouteKey routeKey;
        if (type == NlriType.InterAsIPmsiAD) {
            routeKey = new InterAsIPmsiADCaseBuilder((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCase) key).build();
        } else {
            routeKey = new SPmsiADCaseBuilder((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                    .mvpn.rev180417.mvpn.mvpn.choice.SPmsiADCase) key).build();
        }
        return new LeafADCaseBuilder().setLeafAD(new LeafADBuilder()
                .setLeafADRouteKey(routeKey)
                .setOrigRouteIp(IpAddressUtil.addressForByteBufWOLength(buffer)).build())
                .build();
    }

    @Override
    protected ByteBuf serializeBody(final LeafADCase mvpn) {
        final LeafAD leaf = mvpn.getLeafAD();
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        final LeafADRouteKey key = leaf.getLeafADRouteKey();
        final MvpnChoice keyCase;
        if (key instanceof InterAsIPmsiADCase) {
            keyCase = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                    .bgp.mvpn.rev200120.mvpn.mvpn.choice.InterAsIPmsiADCaseBuilder((InterAsIPmsiADCase) key).build();
        } else {
            keyCase = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev200120.mvpn
                    .mvpn.choice.SPmsiADCaseBuilder((SPmsiADCase) key).build();
        }
        nlriByteBuf.writeBytes(SimpleMvpnNlriRegistry.getInstance().serializeMvpn(keyCase));
        final ByteBuf orig = IpAddressUtil.bytesWOLengthFor(leaf.getOrigRouteIp());
        Preconditions.checkArgument(orig.readableBytes() > 0);
        nlriByteBuf.writeBytes(orig);
        return nlriByteBuf;
    }

    @Override
    public Class<? extends MvpnChoice> getClazz() {
        return LeafADCase.class;
    }
}
