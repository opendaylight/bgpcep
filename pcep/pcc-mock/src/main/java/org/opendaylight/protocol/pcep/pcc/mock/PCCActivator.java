/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;

public class PCCActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();
        regs.add(context.registerObjectParser(PCCEndPointIpv4ObjectParser.CLASS, PCCEndPointIpv4ObjectParser.TYPE,
                new PCCEndPointIpv4ObjectParser()));
        return regs;
    }

}
