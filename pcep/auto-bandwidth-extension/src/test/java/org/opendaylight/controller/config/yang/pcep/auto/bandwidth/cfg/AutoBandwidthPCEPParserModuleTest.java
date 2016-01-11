/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.auto.bandwidth.cfg;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.pcep.auto.bandwidth.extension.Activator;

public class AutoBandwidthPCEPParserModuleTest {

    private static final AutoBandwidthPCEPParserModuleFactory FACTORY = new AutoBandwidthPCEPParserModuleFactory();

    private AutoBandwidthPCEPParserModule module;

    @Before
    public void setUp() {
        final DependencyResolver dependencyResolver = Mockito.mock(DependencyResolver.class);
        this.module = (AutoBandwidthPCEPParserModule) FACTORY.createModule("instance", dependencyResolver, null);
    }

    @Test
    public void testCreateInsatnce() {
        final AutoCloseable instance = module.createInstance();
        Assert.assertTrue(instance instanceof Activator);
    }

    @Test
    public void testCustomValidationLowerBound() {
        try  {
            this.module.setBandwidthUsageObjectType((short) 3);
            this.module.customValidation();
        } catch (final JmxAttributeValidationException e) {
            Assert.fail(e.getMessage());
        }

        try {
            this.module.setBandwidthUsageObjectType((short) 2);
            this.module.customValidation();
            Assert.fail();
        } catch (final JmxAttributeValidationException e) {
            Assert.assertTrue(e.getMessage().startsWith("BandwidthUsageObjectType out of range"));
        }

    }

    @Test
    public void testCustomValidationUpperBound() {
        try  {
            this.module.setBandwidthUsageObjectType((short) 15);
            this.module.customValidation();
        } catch (final JmxAttributeValidationException e) {
            Assert.fail(e.getMessage());
        }

        try {
            this.module.setBandwidthUsageObjectType((short) 16);
            this.module.customValidation();
            Assert.fail();
        } catch (final JmxAttributeValidationException e) {
            Assert.assertTrue(e.getMessage().startsWith("BandwidthUsageObjectType out of range"));
        }

    }

}
