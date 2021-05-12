/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import java.util.List;
import org.opendaylight.yangtools.concepts.Registration;

public interface BGPExtensionProviderActivator {
    List<? extends Registration> start(BGPExtensionProviderContext context);
}