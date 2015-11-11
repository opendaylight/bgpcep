/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009;

import com.google.common.primitives.UnsignedInts;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetMedType.Enumeration;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class BgpSetMedTypeBuilder {

    private static final Pattern MED_TYPE_STRING_PATTERN = Pattern.compile("^^[+-][0-9]+$");

    public static BgpSetMedType getDefaultInstance(final java.lang.String defaultValue) {
        final Matcher ipv4Matcher = MED_TYPE_STRING_PATTERN.matcher(defaultValue);

        if (ipv4Matcher.matches()) {
            return new BgpSetMedType(defaultValue);
        } else {
            try {
                final long parseUnsignedInt = UnsignedInts.parseUnsignedInt(defaultValue);
                return new BgpSetMedType(parseUnsignedInt);
            } catch (final NumberFormatException e) {
                try {
                    final Enumeration medTypeEnum = BgpSetMedType.Enumeration.valueOf(defaultValue.toUpperCase());
                    return new BgpSetMedType(medTypeEnum);
                } catch(final IllegalArgumentException e1) {
                    throw new IllegalArgumentException("Cannot create BgpSetMedType from " + defaultValue);
                }
            }
        }
    }

}
