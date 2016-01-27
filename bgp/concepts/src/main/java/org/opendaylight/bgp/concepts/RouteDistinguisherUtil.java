/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.AdministratorSubfield;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.route.distinguisher.administrator.subfield.Ipv4CaseBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Utility class for of RouteDistinguisher serialization and parsing.
 * https://tools.ietf.org/html/rfc4364#section-4.2
 */
public final class RouteDistinguisherUtil {

    private static final int AS_2BYTE_TYPE = 0;
    private static final int IPV4_TYPE = 1;
    private static final int AS_4BYTE_TYPE = 2;

    private RouteDistinguisherUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Serializes route distinguisher according to type and writes into ByteBuf.
     *
     * @param distinquisher
     * @param byteAggregator
     */
    public static void serializeRouteDistinquisher(final RouteDistinguisher distinquisher, final ByteBuf byteAggregator) {
        final int type = distinquisher.getType();
        byteAggregator.writeShort(type);
        switch (type) {
        case AS_2BYTE_TYPE:
            byteAggregator.writeShort(((AsNumberCase) distinquisher.getAdministratorSubfield()).getAsNumber().getValue().intValue());
            byteAggregator.writeInt(distinquisher.getAssignedNumberSubfield().intValue());
            break;
        case IPV4_TYPE:
            byteAggregator.writeBytes(Ipv4Util.byteBufForAddress(((Ipv4Case) distinquisher.getAdministratorSubfield()).getIpv4Address()));
            byteAggregator.writeShort(distinquisher.getAssignedNumberSubfield().intValue());
            break;
        case AS_4BYTE_TYPE:
            byteAggregator.writeInt(((AsNumberCase) distinquisher.getAdministratorSubfield()).getAsNumber().getValue().intValue());
            byteAggregator.writeShort(distinquisher.getAssignedNumberSubfield().intValue());
            break;
        }
    }

    /**
     * Parses three types of route distinguisher from given ByteBuf.
     *
     * @param buffer
     * @return RouteDistinguisher
     */
    public static RouteDistinguisher parseRouteDistinguisher(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        final AdministratorSubfield administratorSubfield;
        final Long assignedNumberSubfield;
        switch (type) {
        case AS_2BYTE_TYPE:
            administratorSubfield = new AsNumberCaseBuilder().setAsNumber(new AsNumber(new Long(buffer.readUnsignedShort()))).build();
            assignedNumberSubfield = new Long(buffer.readUnsignedInt());
            break;
        case IPV4_TYPE:
            administratorSubfield = new Ipv4CaseBuilder().setIpv4Address(Ipv4Util.addressForByteBuf(buffer)).build();
            assignedNumberSubfield = new Long(buffer.readUnsignedShort());
            break;
        case AS_4BYTE_TYPE:
            administratorSubfield = new AsNumberCaseBuilder().setAsNumber(new AsNumber(new Long(buffer.readUnsignedInt()))).build();
            assignedNumberSubfield = new Long(buffer.readUnsignedShort());
            break;
        default:
            administratorSubfield = null;
            assignedNumberSubfield = null;
            break;
        }
        return new RouteDistinguisher() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return RouteDistinguisher.class;
            }
            @Override
            public Integer getType() {
                return type;
            }
            @Override
            public Long getAssignedNumberSubfield() {
                return assignedNumberSubfield;
            }
            @Override
            public AdministratorSubfield getAdministratorSubfield() {
                return administratorSubfield;
            }
        };
    }
}
