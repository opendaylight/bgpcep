/*
 * Copyright (c) 2017 AT&T AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider.session.stats;

import com.google.common.annotations.Beta;

/**
 * Provide access to topology session stats without expose variables.
 */
@Beta
public interface TopologySessionStats {

    boolean isSessionSynchronized();

    boolean isInitiationCapability();

    boolean isStatefulCapability();

    boolean isLspUpdateCapability();
}
