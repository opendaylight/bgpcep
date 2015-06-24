/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Used as a reason when one of the regular reasons was the cause of the termination of a session.
 */
public final class PCEPCloseTermination extends PCEPTerminationReason {

    /**
     * Creates new Termination.
     *
     * @param reason reason for termination
     */
    public PCEPCloseTermination(final TerminationReason reason) {
        super(reason);
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("reason", super.reason);
    }
}
