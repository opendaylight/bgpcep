/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.auto.bandwidth.extension;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;

public class ActivatorTest {

    @Test
    public void testStartImplPCEPExtensionProviderContext() throws PCEPDeserializerException {
        final Activator activator = new Activator(1);
        final PCEPExtensionProviderContext ctx = ServiceLoaderPCEPExtensionProviderContext.create();
        final List<AutoCloseable> registrations = activator.startImpl(ctx);
        Assert.assertEquals(4, registrations.size());
        activator.close();
    }

}
