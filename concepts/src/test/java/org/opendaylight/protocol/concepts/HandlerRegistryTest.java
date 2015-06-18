/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.concepts;

import org.junit.Assert;
import org.junit.Test;

public class HandlerRegistryTest {

    private static final String PARSER = "parser";
    private static final String SERIALIZER = "serializer";
    private static final int TYPE = 1;

    @Test
    public void testHandlerRegistry() {
        final HandlerRegistry<Object, String, String> registry = new HandlerRegistry<>();
        final AbstractRegistration parserReg = registry.registerParser(TYPE, PARSER);
        final AbstractRegistration serializerReg = registry.registerSerializer(Object.class, SERIALIZER);

        Assert.assertNotNull(parserReg);
        Assert.assertNotNull(serializerReg);
        Assert.assertEquals(SERIALIZER, registry.getSerializer(Object.class));
        Assert.assertEquals(SERIALIZER, registry.getAllSerializers().iterator().next());
        Assert.assertEquals(PARSER, registry.getParser(TYPE));
        Assert.assertNull(registry.getParser(0));
        Assert.assertNull(registry.getSerializer(String.class));

        parserReg.close();
        serializerReg.close();
        serializerReg.close();
        Assert.assertNull(registry.getParser(TYPE));
        Assert.assertNull(registry.getSerializer(Object.class));
    }

}
