/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.subobject.XROSubobjectAttribute;

/**
 * Bidirectional mapping for XROSubobjectAttribute and appropriate identifier.
 */
public class XROSubobjectAttributeMapping {
	private static final XROSubobjectAttributeMapping instance = new XROSubobjectAttributeMapping();

	private final Map<XROSubobjectAttribute, Integer> ofCodesMap = new EnumMap<XROSubobjectAttribute, Integer>(XROSubobjectAttribute.class);
	private final Map<Integer, XROSubobjectAttribute> ofCodeIdsMap = new HashMap<Integer, XROSubobjectAttribute>();

	private XROSubobjectAttributeMapping() {
		this.fillIn();
	}

	private void fillIn() {
		this.fillIn(0, XROSubobjectAttribute.INTERFACE);
		this.fillIn(1, XROSubobjectAttribute.NODE);
		this.fillIn(2, XROSubobjectAttribute.SRLG);
	}

	private void fillIn(int identifier, XROSubobjectAttribute ofCode) {
		this.ofCodesMap.put(ofCode, identifier);
		this.ofCodeIdsMap.put(identifier, ofCode);
	}

	public int getFromAttributeEnum(XROSubobjectAttribute ofCode) {
		final Integer ofci = this.ofCodesMap.get(ofCode);
		if (ofci == null)
			throw new NoSuchElementException("Unknown XROSubobjectAttribute type: " + ofCode);
		return ofci;
	}

	public XROSubobjectAttribute getFromAttributeIdentifier(int identifier) {
		final XROSubobjectAttribute ofc = this.ofCodeIdsMap.get(identifier);
		if (ofc == null)
			throw new NoSuchElementException("Unknown XROSubobjectAttribute identifier.");
		return ofc;
	}

	public static XROSubobjectAttributeMapping getInstance() {
		return instance;
	}
}
