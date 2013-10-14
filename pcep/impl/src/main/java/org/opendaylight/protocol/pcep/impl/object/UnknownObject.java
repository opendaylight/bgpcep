/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;

public class UnknownObject extends PCEPObject {

	private final PCEPErrors error;

	public UnknownObject(boolean processed, boolean ignored, PCEPErrors error) {
		super(processed, ignored);

		this.error = error;
	}

	public PCEPErrors getError() {
		return this.error;
	}

}
