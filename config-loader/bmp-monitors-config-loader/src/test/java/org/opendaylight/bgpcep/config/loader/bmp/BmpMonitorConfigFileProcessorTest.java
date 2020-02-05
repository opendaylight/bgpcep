/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.config.loader.bmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.bgpcep.config.loader.bmp.BmpMonitorConfigFileProcessor.ODL_BMP_MONITORS_IID;
import static org.opendaylight.protocol.util.CheckUtil.checkNotPresentConfiguration;
import static org.opendaylight.protocol.util.CheckUtil.checkPresentConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.config.loader.impl.AbstractConfigLoader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.config.rev200120.OdlBmpMonitors;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BmpMonitorConfigFileProcessorTest extends AbstractConfigLoader {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void configFileTest() throws Exception {
        checkNotPresentConfiguration(getDataBroker(), ODL_BMP_MONITORS_IID);

        assertNotNull(ClassLoader.getSystemClassLoader().getResource("initial/odl-bmp-monitors-config.xml"));
        final BmpMonitorConfigFileProcessor processor = new BmpMonitorConfigFileProcessor(this.configLoader,
                getDataBroker());
        processor.init();
        checkPresentConfiguration(getDataBroker(), ODL_BMP_MONITORS_IID);

        assertEquals(SchemaPath.create(true, OdlBmpMonitors.QNAME),
                processor.getSchemaPath());
        processor.close();
    }
}
