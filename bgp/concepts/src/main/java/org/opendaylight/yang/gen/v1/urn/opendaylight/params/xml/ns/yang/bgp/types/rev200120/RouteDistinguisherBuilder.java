/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120;

import java.util.regex.Pattern;


/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public final class RouteDistinguisherBuilder {

    private static final Pattern RD_TWO_OCTET_AS =
        Pattern.compile("0:"
            + "([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|"
            + "[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]|"
            + "65[0-4][0-9][0-9]|655[0-2][0-9]|6553[0-5])"
            + ":"
            + "([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|[1-3][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "4[0-1][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|42[0-8][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "429[0-3][0-9][0-9][0-9][0-9][0-9][0-9]|4294[0-8][0-9][0-9][0-9][0-9][0-9]|"
            + "42949[0-5][0-9][0-9][0-9][0-9]|429496[0-6][0-9][0-9][0-9]|4294967[0-1][0-9][0-9]|"
            + "42949672[0-8][0-9]|429496729[0-5])");

    private static final Pattern RD_IPV4 =
        Pattern.compile("((([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}"
            + "([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5]))"
            + ":"
            + "([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|"
            + "[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]|"
            + "65[0-4][0-9][0-9]|655[0-2][0-9]|6553[0-5])");

    private static final Pattern RD_AS =
        Pattern.compile("([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9][0-9][0-9]|[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "[1-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|[1-3][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "4[0-1][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|42[0-8][0-9][0-9][0-9][0-9][0-9][0-9][0-9]|"
            + "429[0-3][0-9][0-9][0-9][0-9][0-9][0-9]|4294[0-8][0-9][0-9][0-9][0-9][0-9]|"
            + "42949[0-5][0-9][0-9][0-9][0-9]|429496[0-6][0-9][0-9][0-9]|4294967[0-1][0-9][0-9]|"
            + "42949672[0-8][0-9]|429496729[0-5])"
            + ":"
            + "([0-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|"
            + "[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]|"
            + "65[0-4][0-9][0-9]|655[0-2][0-9]|6553[0-5])");

    private RouteDistinguisherBuilder() {

    }

    public static RouteDistinguisher getDefaultInstance(final java.lang.String defaultValue) {
        if (RD_TWO_OCTET_AS.matcher(defaultValue).matches()) {
            return new RouteDistinguisher((new RdTwoOctetAs(defaultValue)));
        } else if (RD_IPV4.matcher(defaultValue).matches()) {
            return new RouteDistinguisher(new RdIpv4(defaultValue));
        } else if (RD_AS.matcher(defaultValue).matches()) {
            return new RouteDistinguisher(new RdAs(defaultValue));
        } else {
            throw new IllegalArgumentException("Cannot create Route Distinguisher from " + defaultValue);
        }
    }

}
