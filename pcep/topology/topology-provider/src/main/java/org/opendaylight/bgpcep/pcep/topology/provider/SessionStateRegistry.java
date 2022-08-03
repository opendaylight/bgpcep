/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Topology Node Sessions stats handler. It is resposible for dispatching update requests to each registrant.
 */
interface SessionStateRegistry {
    /**
     * Register session to Session stats Registry handler.
     *
     * @param updater A {@link SessionStateUpdater}
     */
    @CheckReturnValue
    @NonNull ObjectRegistration<SessionStateUpdater> bind(@NonNull SessionStateUpdater updater);
}
