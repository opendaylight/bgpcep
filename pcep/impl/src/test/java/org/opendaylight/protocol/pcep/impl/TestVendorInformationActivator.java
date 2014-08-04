/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;

public class TestVendorInformationActivator extends AbstractPCEPExtensionProviderActivator {

    @Override
    protected List<AutoCloseable> startImpl(PCEPExtensionProviderContext context) {
        final List<AutoCloseable> regs = Lists.newArrayList();
        final TestVendorInformationTlvParser parser = new TestVendorInformationTlvParser();
        regs.add(context.registerVendorInformationTlvParser(parser.getEnterpriseNumber(), parser));
        regs.add(context.registerVendorInformationTlvSerializer(TestEnterpriseSpecificInformation.class, parser));

        // Vendor-information object registration
        final TestVendorInformationObjectParser objParser = new TestVendorInformationObjectParser();
        regs.add(context.registerVendorInformationObjectParser(parser.getEnterpriseNumber(), objParser));
        regs.add(context.registerVendorInformationObjectSerializer(TestEnterpriseSpecificInformation.class, objParser));
        return regs;
    }

}
