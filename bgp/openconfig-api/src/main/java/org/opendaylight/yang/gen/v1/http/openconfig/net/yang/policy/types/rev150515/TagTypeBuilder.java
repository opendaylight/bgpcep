/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev150515;

import com.google.common.primitives.UnsignedInts;


/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class TagTypeBuilder {

    public static TagType getDefaultInstance(final java.lang.String defaultValue) {
        try {
            final long parseUnsignedInt = UnsignedInts.parseUnsignedInt(defaultValue);
            return new TagType(parseUnsignedInt);
        } catch (final NumberFormatException e) {
            return new TagType(defaultValue);
        }
    }

}
