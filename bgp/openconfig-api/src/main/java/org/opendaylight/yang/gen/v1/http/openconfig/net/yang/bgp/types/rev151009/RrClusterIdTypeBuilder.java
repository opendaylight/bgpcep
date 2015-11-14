/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009;

import com.google.common.primitives.UnsignedInts;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class RrClusterIdTypeBuilder {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(%[\\p{N}\\p{L}]+)?$");

    public static RrClusterIdType getDefaultInstance(final java.lang.String defaultValue) {
        final Matcher ipv4Matcher = IPV4_PATTERN.matcher(defaultValue);
        if (ipv4Matcher.matches()) {
            return new RrClusterIdType(new Ipv4Address(defaultValue));
        } else {
            try {
                final long parseUnsignedInt = UnsignedInts.parseUnsignedInt(defaultValue);
                return new RrClusterIdType(parseUnsignedInt);
            } catch (final NumberFormatException e) {
                throw new IllegalArgumentException("Cannot create RrClusterIdType from " + defaultValue);
            }
        }
    }

}
