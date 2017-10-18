/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.spi;

import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev171207.RouterId;

/**
 * Represent monitored router.
 *
 */
public interface BmpRouter extends BmpSessionListener, AutoCloseable {

    /**
     * Returns router's identifier, represented by router's remote IP address.
     * @return router identifier.
     */
    RouterId getRouterId();

}
