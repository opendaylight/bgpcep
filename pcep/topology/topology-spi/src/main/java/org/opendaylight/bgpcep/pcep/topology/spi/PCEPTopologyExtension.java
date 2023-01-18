/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.spi;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * An extension attaching to a PCEP-capable topology instance.
 */
public interface PCEPTopologyExtension {

    ListenableFuture<Empty> shutdown();
}
