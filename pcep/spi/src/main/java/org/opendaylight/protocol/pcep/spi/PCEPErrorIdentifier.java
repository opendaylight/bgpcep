/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import java.io.Serializable;

/**
 * Caret for combination of Error-type and Error-value.
 */
final class PCEPErrorIdentifier implements Serializable {
    private static final long serialVersionUID = 2434590156751699872L;
    private final short type;
    private final short value;

    PCEPErrorIdentifier(final short type, final short value) {
        this.type = type;
        this.value = value;
    }

    public short getType() {
        return this.type;
    }

    public short getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.type;
        result = prime * result + this.value;
        return result;
    }

    @Override
    public boolean equals(final java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final PCEPErrorIdentifier other = (PCEPErrorIdentifier) obj;
        if (this.type != other.type || this.value != other.value) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "type " + this.type + " value " + this.value;
    }
}
