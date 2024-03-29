/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock;

import java.util.List;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.yangtools.concepts.Registration;

@MetaInfServices
public final class PCCActivator implements PCEPExtensionProviderActivator {
    @Override
    public List<Registration> start(final PCEPExtensionProviderContext context) {
        return List.of(context.registerObjectParser(new PCCEndPointIpv4ObjectParser()));
    }
}
