/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;

/**
 * PCEP objects are identified with a couple <class, type>.
 */
public class PCEPObjectIdentifier {
	/**
	 * Class identifier for {@link org.opendaylight.protocol.pcep.PCEPObject PCEPObject}
	 */
	public static enum ObjectClass {
		OPEN(1),
		RP(2),
		NO_PATH(3),
		END_POINTS(4),
		BANDWIDTH(5),
		METRIC(6),
		ERO(7),
		RRO(8),
		LSPA(9),
		IRO(10),
		SVEC(11),
		NOTIFICATION(12),
		ERROR(13),
		LOAD_BALANCING(14),
		CLOSE(15),
		XRO(17),
		OBJCETIVE_FUNCTION(21),
		GLOBAL_CONSTRAINTS(24),
		UNREACHED_DESTINATION(28),
		SERO(29),
		SRRO(30),
		BRANCH_NODE(31),
		LSP(32);

		private final int identifier;

		ObjectClass(final int identifier) {
			this.identifier = identifier;
		}

		public int getIdentifier() {
			return this.identifier;
		}

		public static ObjectClass getFromInt(int identifier) throws PCEPDocumentedException {
			for (final ObjectClass type_e : ObjectClass.values()) {
				if (type_e.getIdentifier() == identifier)
					return type_e;
			}

			throw new PCEPDocumentedException("Unrecognized object class " + identifier, PCEPErrors.UNRECOGNIZED_OBJ_CLASS);
		}
	}

	private final int objectType;

	private final ObjectClass objectClass;

	public PCEPObjectIdentifier(ObjectClass objectClass, int objectType) {
		this.objectType = objectType;
		this.objectClass = objectClass;
	}

	public int getObjectType() {
		return this.objectType;
	}

	public ObjectClass getObjectClass() {
		return this.objectClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.objectClass == null) ? 0 : this.objectClass.hashCode());
		result = prime * result + this.objectType;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		final PCEPObjectIdentifier other = (PCEPObjectIdentifier) obj;
		if (this.objectClass != other.objectClass)
			return false;
		if (this.objectType != other.objectType)
			return false;
		return true;
	}
}
