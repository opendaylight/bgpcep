/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.types.rev151018;

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
public class RouteDistinguisherBuilder {

    private static final Pattern[] PATTERNS;

    static {
        final Pattern a[] = new Pattern[RouteDistinguisher.PATTERN_CONSTANTS.size()];
        int i = 0;
        for (final String regEx : RouteDistinguisher.PATTERN_CONSTANTS) {
            a[i++] = Pattern.compile(regEx);
        }

        PATTERNS = a;
    }

    public static RouteDistinguisher getDefaultInstance(final String defaultValue) {
        if (anyMatch(defaultValue)) {
            return new RouteDistinguisher(defaultValue);
        } else {
            throw new IllegalArgumentException("Cannot create RouteDistinguisher from " + defaultValue);
        }
    }

    private static boolean anyMatch(final String defaultValue) {
        for (final Pattern pattern : PATTERNS) {
            if (pattern.matcher(defaultValue).matches()) {
                return true;
            }
        }
        return false;
    }

}
