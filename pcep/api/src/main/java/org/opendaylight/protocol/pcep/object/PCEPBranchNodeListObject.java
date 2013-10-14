/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.List;

import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;

/**
 * Structure of Branch Node list object.
 *
 * @see <a href="http://tools.ietf.org/html/rfc6006#section-3.11.1">Branch Node
 *      Object [RFC6006]</a>
 */
public class PCEPBranchNodeListObject extends PCEPBranchNodeObject {

    /**
     * Constructs Branch Node list object.
     *
     * @param subobjects
     *            List<ExplicitRouteSubobject>
     * @param processed
     *            boolean
     * @param ignored
     *            boolean
     */
    public PCEPBranchNodeListObject(List<ExplicitRouteSubobject> subobjects, boolean processed, boolean ignored) {
	super(subobjects, processed, ignored);
    }
}
