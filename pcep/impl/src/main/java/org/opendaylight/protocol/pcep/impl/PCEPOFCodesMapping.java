/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.opendaylight.protocol.pcep.PCEPOFCodes;

/**
 * Mapping of enumerable objective function codes to integral identifiers and
 * vice-versa.
 */
public class PCEPOFCodesMapping {
	private static final PCEPOFCodesMapping instance = new PCEPOFCodesMapping();

	private final Map<PCEPOFCodes, Integer> ofCodesMap = new EnumMap<PCEPOFCodes, Integer>(PCEPOFCodes.class);
	private final Map<Integer, PCEPOFCodes> ofCodeIdsMap = new HashMap<Integer, PCEPOFCodes>();

	private PCEPOFCodesMapping() {
		this.fillIn();
	}

	private void fillIn() {
		this.fillIn(1, PCEPOFCodes.MCP);
		this.fillIn(2, PCEPOFCodes.MLP);
		this.fillIn(3, PCEPOFCodes.MBP);
		this.fillIn(4, PCEPOFCodes.MBC);
		this.fillIn(5, PCEPOFCodes.MLL);
		this.fillIn(6, PCEPOFCodes.MCC);
		this.fillIn(7, PCEPOFCodes.SPT);
		this.fillIn(8, PCEPOFCodes.MCT);
	}

	private void fillIn(int identifier, PCEPOFCodes ofCode) {
		this.ofCodesMap.put(ofCode, identifier);
		this.ofCodeIdsMap.put(identifier, ofCode);
	}

	public int getFromOFCodesEnum(PCEPOFCodes ofCode) {
		final Integer ofci = this.ofCodesMap.get(ofCode);
		if (ofci == null)
			throw new NoSuchElementException("Unknown PCEPOFCodes type: " + ofCode);
		return ofci;
	}

	public PCEPOFCodes getFromCodeIdentifier(int identifier) {
		final PCEPOFCodes ofc = this.ofCodeIdsMap.get(identifier);
		if (ofc == null)
			throw new NoSuchElementException("Unknown PCEPOFCode identifier.");
		return ofc;
	}

	public static PCEPOFCodesMapping getInstance() {
		return instance;
	}
}
