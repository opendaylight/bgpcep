/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev151009;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.local.routing.rev151009.LocalStaticConfig.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;


/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class LocalStaticConfigNextHopBuilder {

    public static NextHop getDefaultInstance(final java.lang.String defaultValue) {
        try {
            final LocalDefinedNextHop nextHopEnum = LocalDefinedNextHop.valueOf(defaultValue.toUpperCase());
            return new NextHop(nextHopEnum);
        } catch (final IllegalArgumentException e) {
            try {
                final IpAddress ipAddress = IpAddressBuilder.getDefaultInstance(defaultValue);
                return new NextHop(ipAddress);
            } catch (final IllegalArgumentException e1) {
                return new NextHop(defaultValue);
            }
        }
    }

}
