/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.yangtools.concepts.Registration;

public class HandlerRegistryTest {
    private static final String PARSER = "parser";
    private static final String SERIALIZER = "serializer";
    private static final int TYPE = 1;

    @Test
    public void testHandlerRegistry() {
        final HandlerRegistry<Object, String, String> registry = new HandlerRegistry<>();
        final Registration parserReg = registry.registerParser(TYPE, PARSER);
        final Registration serializerReg = registry.registerSerializer(Object.class, SERIALIZER);

        assertNotNull(parserReg);
        assertNotNull(serializerReg);
        assertEquals(SERIALIZER, registry.getSerializer(Object.class));
        assertEquals(SERIALIZER, registry.getAllSerializers().iterator().next());
        assertEquals(PARSER, registry.getParser(TYPE));
        assertNull(registry.getParser(0));
        assertNull(registry.getSerializer(String.class));

        parserReg.close();
        serializerReg.close();
        serializerReg.close();
        assertNull(registry.getParser(TYPE));
        assertNull(registry.getSerializer(Object.class));
    }
}
