/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

public abstract class CommonObjectParser implements ObjectParser {
    private final int objectClass;
    private final int objectType;

    public CommonObjectParser(final int objectClass, final int objectType) {
        this.objectClass = objectClass;
        this.objectType = objectType;
    }

    @Override
    public final int getObjectClass() {
        return this.objectClass;
    }

    @Override
    public final int getObjectType() {
        return this.objectType;
    }
}
