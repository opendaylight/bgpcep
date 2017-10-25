/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

public class Stateful07TopologySessionListenerFactory implements TopologySessionListenerFactory {
    @Override
    public final TopologySessionListener createTopologySessionListener(final ServerSessionManager manager) {
        return new Stateful07TopologySessionListener(manager);
    }
}
