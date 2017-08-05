/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.api;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev170517.odl.bmp.monitors.BmpMonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.MonitorId;

@Beta
public interface BmpDeployer {
    /**
     * Writes BmpMonitorConfig to Config DS
     *
     * @param bmpConfig containing bmp Monitor configuration
     */
    void writeBmpMonitor(@Nonnull BmpMonitorConfig bmpConfig) throws TransactionCommitFailedException;

    /**
     * Removes BmpMonitorConfig from Config DS
     *
     * @param monitorId Bmp monitor Id
     */
    void deleteBmpMonitor(@Nonnull MonitorId monitorId) throws TransactionCommitFailedException;
}
