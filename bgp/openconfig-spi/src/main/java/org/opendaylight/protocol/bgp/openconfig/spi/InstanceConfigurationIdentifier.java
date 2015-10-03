/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Identifier of an InstanceConfiguration
 *
 */
public final class InstanceConfigurationIdentifier implements Identifier {

    private static final long serialVersionUID = 1L;

    private final String name;

    public InstanceConfigurationIdentifier(final String name) {
        this.name = Preconditions.checkNotNull(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InstanceConfiguration)) {
            return false;
        }
        final InstanceConfigurationIdentifier other = (InstanceConfigurationIdentifier) obj;
        if (!Objects.equals(name, other.name)) {
            return false;
        }
        return true;
    }



}
