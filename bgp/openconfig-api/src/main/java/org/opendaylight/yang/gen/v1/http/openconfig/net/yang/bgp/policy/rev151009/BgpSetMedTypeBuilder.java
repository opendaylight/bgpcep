/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009;

import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.BgpSetMedType.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customized handler for instantiating {@link BgpSetMedType} from a String.
 */
public final class BgpSetMedTypeBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(BgpSetMedTypeBuilder.class);
    private static final Pattern MED_TYPE_STRING_PATTERN = Pattern.compile("^[+-][0-9]+$");

    private BgpSetMedTypeBuilder() {
        // Hidden
    }

    public static BgpSetMedType getDefaultInstance(final String defaultValue) {
        if (MED_TYPE_STRING_PATTERN.matcher(defaultValue).matches()) {
            return new BgpSetMedType(defaultValue);
        }

        try {
            return new BgpSetMedType(Integer.toUnsignedLong(Integer.parseUnsignedInt(defaultValue)));
        } catch (final NumberFormatException e) {
            LOG.debug("Could not interpret \"{}\" as an unsinged integer", defaultValue, e);
        }

        return new BgpSetMedType(Enumeration.forName(defaultValue.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid BgpSetMedType " + defaultValue)));
    }
}
