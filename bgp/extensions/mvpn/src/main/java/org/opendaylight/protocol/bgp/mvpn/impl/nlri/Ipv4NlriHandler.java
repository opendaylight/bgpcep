/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mvpn.impl.nlri;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.mvpn.spi.pojo.nlri.SimpleMvpnNlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.PathIdUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination.MvpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.mvpn.destination.MvpnDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationMvpnIpv4AdvertizedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.mvpn.ipv4.advertized._case.DestinationMvpnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv4WithdrawnCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationMvpnIpv4WithdrawnCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.NlriType;

/**
 * Handles Ipv4 Family nlri.
 *
 * @author Claudio D. Gasparini
 */
public final class Ipv4NlriHandler {
    private Ipv4NlriHandler() {
        // Hidden on purpose
    }

    static DestinationMvpnIpv4AdvertizedCase parseIpv4ReachNlri(
        final ByteBuf nlri,
        final boolean addPathSupported) {
        final List<MvpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final MvpnDestinationBuilder builder = new MvpnDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final NlriType type = NlriType.forValue(nlri.readUnsignedByte());
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setMvpnChoice(SimpleMvpnNlriRegistry.getInstance().parseMvpn(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationMvpnIpv4AdvertizedCaseBuilder()
            .setDestinationMvpn(new DestinationMvpnBuilder().setMvpnDestination(dests).build()).build();
    }

    static DestinationMvpnIpv4WithdrawnCase parseIpv4UnreachNlri(
        final ByteBuf nlri,
        final boolean addPathSupported) {
        final List<MvpnDestination> dests = new ArrayList<>();

        while (nlri.isReadable()) {
            final MvpnDestinationBuilder builder = new MvpnDestinationBuilder();
            if (addPathSupported) {
                builder.setPathId(PathIdUtil.readPathId(nlri));
            }
            final NlriType type = NlriType.forValue(nlri.readUnsignedByte());
            final int length = nlri.readUnsignedByte();
            final ByteBuf nlriBuf = nlri.readSlice(length);
            builder.setMvpnChoice(SimpleMvpnNlriRegistry.getInstance().parseMvpn(type, nlriBuf));
            dests.add(builder.build());
        }

        return new DestinationMvpnIpv4WithdrawnCaseBuilder().setDestinationMvpn(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.ipv4.rev180417.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.mvpn.ipv4
                .withdrawn._case.DestinationMvpnBuilder().setMvpnDestination(dests).build()).build();
    }

    public static void serializeNlri(final List<MvpnDestination> destinationList, final ByteBuf output) {
        for (final MvpnDestination dest : destinationList) {
            output.writeBytes(SimpleMvpnNlriRegistry.getInstance().serializeMvpn(dest.getMvpnChoice()));
        }
    }
}
