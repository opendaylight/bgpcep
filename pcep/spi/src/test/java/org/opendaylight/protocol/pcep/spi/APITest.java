/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.close.object.CCloseBuilder;

class APITest {
    @Test
    void testDeserializerException() {
        final var ex = new PCEPDeserializerException("Some error message.");
        assertEquals("Some error message.", ex.getMessage());
    }

    @Test
    void testDeserializerExceptionWithCause() {
        final var cause = new IllegalArgumentException();
        final var ex = new PCEPDeserializerException("Some error message.", cause);
        assertEquals("Some error message.", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void testObjectHeader() {
        final var header = new ObjectHeaderImpl(null, true);
        assertEquals("ObjectHeader [objClass=, processed=null, ignored=true]", header.toString());
        assertTrue(header.getIgnore());
        assertNull(header.getProcessingRule());

        assertEquals(new ObjectHeaderImpl(null, true).hashCode(),  header.hashCode());
        assertEquals(new ObjectHeaderImpl(null, true), header);
    }

    @Test
    void testUnknownObject() {
        final var un = new UnknownObject(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS);
        assertFalse(un.getIgnore());
        assertFalse(un.getProcessingRule());
        assertEquals(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS, un.getError());
        assertEquals(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS.getErrorType(),
            un.getErrors().getFirst().getErrorObject().getType());

        final var o = new CCloseBuilder().build();
        final var unknown = new UnknownObject(PCEPErrors.LSP_RSVP_ERROR, o);
        assertEquals(Object.class, unknown.implementedInterface());
        assertSame(o, unknown.getInvalidObject());
    }
}