/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import javax.annotation.Nonnull;

/**
 * Factory for generating PCEP Session Listeners. Used by a server.
 */
public interface PCEPSessionListenerFactory {
    /**
     * Returns one session listener.
     *
     * @return specific session listener
     */
    @Nonnull
    PCEPSessionListener getSessionListener();
}
