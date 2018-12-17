/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ObjectHeader;
import org.opendaylight.yangtools.yang.binding.DataContainer;

/**
 * Header parser for PCEP object.
 */
public class ObjectHeaderImpl implements ObjectHeader {

    private final Boolean processed;
    private final Boolean ignored;

    public ObjectHeaderImpl(final Boolean processed, final Boolean ignore) {
        this.processed = processed;
        this.ignored = ignore;
    }

    @Override
    public Class<? extends DataContainer> getImplementedInterface() {
        return ObjectHeader.class;
    }

    @Override
    public Boolean isIgnore() {
        return this.ignored;
    }

    @Override
    public Boolean isProcessingRule() {
        return this.processed;
    }

    @Override
    public String toString() {
        final String objectHeader = "ObjectHeader [objClass="
                + ", processed=" + this.processed
                + ", ignored=" + this.ignored
                + "]";
        return objectHeader;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.ignored == null ? 0 : this.ignored.hashCode());
        result = prime * result + (this.processed == null ? 0 : this.processed.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ObjectHeaderImpl other = (ObjectHeaderImpl) obj;
        if (this.ignored == null) {
            if (other.ignored != null) {
                return false;
            }
        } else if (!this.ignored.equals(other.ignored)) {
            return false;
        }
        if (this.processed == null) {
            if (other.processed != null) {
                return false;
            }
        } else if (!this.processed.equals(other.processed)) {
            return false;
        }
        return true;
    }
}
