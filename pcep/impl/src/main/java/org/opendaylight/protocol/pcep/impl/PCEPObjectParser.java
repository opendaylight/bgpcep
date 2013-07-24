/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Interface for {@link org.opendaylight.protocol.pcep.PCEPObject PCEPObject} subobjects
 */
public interface PCEPObjectParser {
	public abstract PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException;

	public abstract byte[] put(PCEPObject obj);
}
