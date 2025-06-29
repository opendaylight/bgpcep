/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.close.object.CCloseBuilder;

public class APITest {

    @Test
    public void testDeserializerException() {
        final PCEPDeserializerException e = new PCEPDeserializerException("Some error message.");
        assertEquals("Some error message.", e.getMessage());

        final PCEPDeserializerException e1 = new PCEPDeserializerException("Some error message.",
            new IllegalArgumentException());
        assertEquals("Some error message.", e1.getMessage());
        assertTrue(e1.getCause() instanceof IllegalArgumentException);
    }

    @Test
    public void testObjectHeader() {
        ObjectHeaderImpl header = new ObjectHeaderImpl(null, true);
        assertEquals("ObjectHeader [objClass=, processed=null, ignored=true]", header.toString());
        assertTrue(header.getIgnore());
        assertNull(header.getProcessingRule());

        assertEquals(new ObjectHeaderImpl(null, true).hashCode(),  header.hashCode());
        assertEquals(new ObjectHeaderImpl(null, true), header);
    }

    @Test
    public void testUnknownObject() {
        UnknownObject un = new UnknownObject(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS);
        assertFalse(un.getIgnore());
        assertFalse(un.getProcessingRule());
        assertEquals(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS, un.getError());
        assertEquals(PCEPErrors.CT_AND_SETUP_PRIORITY_DO_NOT_FORM_TE_CLASS.getErrorType(),
            un.getErrors().get(0).getErrorObject().getType());

        final Object o = new CCloseBuilder().build();
        UnknownObject unknown = new UnknownObject(PCEPErrors.LSP_RSVP_ERROR, o);
        assertEquals(Object.class, unknown.implementedInterface());
        assertEquals(o, unknown.getInvalidObject());
    }
}
