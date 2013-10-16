/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;

public interface EROSubobjectHandlerRegistry {
	public AutoCloseable registerSubobjectParser(int subobjectType, EROSubobjectParser parser);

	public EROSubobjectParser getSubobjectParser(int subobjectType);

	public AutoCloseable registerSubobjectSerializer(Class<? extends CSubobject> subobjectClass, EROSubobjectSerializer serializer);

	public EROSubobjectSerializer getSubobjectSerializer(Subobjects subobject);
}
