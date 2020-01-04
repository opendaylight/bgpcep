/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.types.rev151018;

import java.util.regex.Pattern;
import org.opendaylight.yangtools.yang.binding.CodeHelpers;

/**
 * Helper builder utility for {@code RouteDistinguisher} union type.
 */
public final class RouteDistinguisherBuilder {
    private static final Pattern[] PATTERNS = CodeHelpers.compilePatterns(RouteDistinguisher.PATTERN_CONSTANTS);

    private RouteDistinguisherBuilder() {
        // Hidden on purpose
    }

    public static RouteDistinguisher getDefaultInstance(final String defaultValue) {
        if (anyMatch(defaultValue)) {
            return new RouteDistinguisher(defaultValue);
        }
        throw new IllegalArgumentException("Cannot create RouteDistinguisher from " + defaultValue);
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
