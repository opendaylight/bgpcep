/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import javax.annotation.Nullable;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;

/**
 * Provides access to PCEPTopologyProviderRuntimeRegistrator
 */
public interface PCEPTopologyProviderRuntime {
    /**
     * Provides access to PCEPTopologyProviderRuntimeRegistrator
     *
     * @param runtimeRootRegistrator PCEPTopologyProviderRuntimeRegistrator
     */
    void registerRuntimeRootRegistration(@Nullable PCEPTopologyProviderRuntimeRegistrator runtimeRootRegistrator);
}
