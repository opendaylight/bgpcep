/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Caret for combination of Error-type and Error-value.
 */
final class PCEPErrorIdentifier implements Serializable {
    private static final long serialVersionUID = 2434590156751699872L;

    private final @NonNull Uint8 type;
    private final @NonNull Uint8 value;

    PCEPErrorIdentifier(final Uint8 type, final Uint8 value) {
        this.type = requireNonNull(type);
        this.value = requireNonNull(value);
    }

    @NonNull Uint8  getType() {
        return type;
    }

    @NonNull Uint8  getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public boolean equals(final java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PCEPErrorIdentifier)) {
            return false;
        }
        final PCEPErrorIdentifier other = (PCEPErrorIdentifier) obj;
        return type.equals(other.type) && value.equals(other.value);
    }

    @Override
    public String toString() {
        return "type " + this.type + " value " + this.value;
    }
}
