/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectIdentifier.ObjectClass;
import org.opendaylight.protocol.pcep.impl.object.PCEPBranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIPv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIPv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExistingPathBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNonBranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPP2MPEndPointsIPv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPP2MPEndPointsIPv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestedPathBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSecondaryExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSecondaryRecordRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPUnreachedIPv4DestinationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPUnreachedIPv6DestinationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.object.PCEPBranchNodeListObject;
import org.opendaylight.protocol.pcep.object.PCEPBranchNodeObject;
import org.opendaylight.protocol.pcep.object.PCEPCloseObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExcludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPExistingPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPGlobalConstraintsObject;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLoadBalancingObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.object.PCEPNonBranchNodeListObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPP2MPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPSecondaryExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPSecondaryRecordRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;
import org.opendaylight.protocol.pcep.object.PCEPUnreachedDestinationObject;

/**
 * Factory for subclasses of {@link org.opendaylight.protocol.pcep.PCEPObject PCEPObject}
 */
public class PCEPObjectFactory {

    private static final Logger logger = LoggerFactory.getLogger(PCEPObjectFactory.class);

    /**
     * Map of parsers for subobjects of {@link org.opendaylight.protocol.pcep.PCEPObject
     * PCEPObject}
     */
    private static class MapOfParsers extends HashMap<PCEPObjectIdentifier, PCEPObjectParser> {
	private static final long serialVersionUID = 1L;

	private final static MapOfParsers instance = new MapOfParsers();

	private MapOfParsers() {
	    this.fillInMap();
	}

	private void fillInMap() {
	    this.put(new PCEPObjectIdentifier(ObjectClass.OPEN, 1), new PCEPOpenObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.RP, 1), new PCEPRequestParameterObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.NO_PATH, 1), new PCEPNoPathObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.BANDWIDTH, 1), new PCEPRequestedPathBandwidthObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.BANDWIDTH, 2), new PCEPExistingPathBandwidthObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.METRIC, 1), new PCEPMetricObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.END_POINTS, 1), new PCEPEndPointsIPv4ObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.END_POINTS, 2), new PCEPEndPointsIPv6ObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.LSPA, 1), new PCEPLspaObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.SVEC, 1), new PCEPSvecObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.NOTIFICATION, 1), new PCEPNotificationObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.ERROR, 1), new PCEPErrorObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.CLOSE, 1), new PCEPCloseObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.LOAD_BALANCING, 1), new PCEPLoadBalancingObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.LSP, 1), new PCEPLspObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.ERO, 1), new PCEPExplicitRouteObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.RRO, 1), new PCEPReportedRouteObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.IRO, 1), new PCEPIncludeRouteObjectParser());

	    /* GCO extension */
	    this.put(new PCEPObjectIdentifier(ObjectClass.XRO, 1), new PCEPExcludeRouteObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.OBJCETIVE_FUNCTION, 1), new PCEPObjectiveFunctionObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.GLOBAL_CONSTRAINTS, 1), new PCEPGlobalConstraintsObjectParser());

	    /* RFC6006 */
	    this.put(new PCEPObjectIdentifier(ObjectClass.END_POINTS, 3), new PCEPP2MPEndPointsIPv4ObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.END_POINTS, 4), new PCEPP2MPEndPointsIPv6ObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.UNREACHED_DESTINATION, 1), new PCEPUnreachedIPv4DestinationObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.UNREACHED_DESTINATION, 2), new PCEPUnreachedIPv6DestinationObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.SERO, 1), new PCEPSecondaryExplicitRouteObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.SRRO, 1), new PCEPSecondaryRecordRouteObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.BRANCH_NODE, 1), new PCEPBranchNodeListObjectParser());
	    this.put(new PCEPObjectIdentifier(ObjectClass.BRANCH_NODE, 2), new PCEPNonBranchNodeListObjectParser());
	}

	public static MapOfParsers getInstance() {
	    return instance;
	}
    }

    private static PCEPObject parse(final byte[] bytes, final PCEPObjectHeader header) throws PCEPDocumentedException, PCEPDeserializerException {
		if (bytes == null)
		    throw new IllegalArgumentException("Array of bytes is mandatory.");
		if (header == null)
		    throw new IllegalArgumentException("PCEPObjectHeader is mandatory.");

		logger.trace("Attempt to parse object from bytes: {}", ByteArray.bytesToHexString(bytes));

		/*
		 * if PCEPObjectIdentifier.getObjectClassFromInt() don't throws
		 * exception and if Map.get() returns null, we know that we can't
		 * recognize OBJ TYPE.
		 */
		final PCEPObjectParser objParserClass = MapOfParsers.getInstance().get(
			new PCEPObjectIdentifier(PCEPObjectIdentifier.ObjectClass.getFromInt(header.objClass), header.objType));
		if (objParserClass == null) {
			logger.debug("Object could not be parsed. Header: {}. Body bytes: {}", header, Arrays.toString(bytes));
		    throw new PCEPDocumentedException("Unrecognized object type: " + header.objType + " for object class: " + header.objClass,
			    PCEPErrors.UNRECOGNIZED_OBJ_TYPE);
		}
		final PCEPObject obj = objParserClass.parse(bytes, header.processed, header.ignored);
		logger.trace("Object was parsed. {}", obj);
		return obj;
    }

    public static List<PCEPObject> parseObjects(final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		int offset = 0;
		final List<PCEPObject> objs = new ArrayList<PCEPObject>();

		while (bytes.length - offset > 0) {
		    if (bytes.length - offset < PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH)
			throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + (bytes.length - offset) + " Expected: >= "
				+ PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH + ".");

		    final PCEPObjectHeader header = PCEPObjectHeader.parseHeader(Arrays.copyOfRange(bytes, offset, offset
			    + PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH));

		    if (bytes.length - offset < header.objLength)
			throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + (bytes.length - offset) + " Expected: >= " + header.objLength
				+ ".");

		    // copy bytes for deeper parsing
		    final byte[] bytesToPass = ByteArray.subByte(bytes, offset + PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH, header.objLength
			    - PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH);

		    offset += header.objLength;

		    // if obj is not-supported or unrecognized and p flag si set
		    // adds UnknownObject to list for validation purposes
		    try {
			objs.add(PCEPObjectFactory.parse(bytesToPass, header));
		    } catch (final PCEPDocumentedException e) {
			if (e.getError() == PCEPErrors.UNRECOGNIZED_OBJ_CLASS | e.getError() == PCEPErrors.UNRECOGNIZED_OBJ_TYPE
				| e.getError() == PCEPErrors.NOT_SUPPORTED_OBJ_CLASS | e.getError() == PCEPErrors.NOT_SUPPORTED_OBJ_TYPE) {
			    objs.add(new UnknownObject(header.processed, header.ignored, e.getError()));
			} else {
			    throw e;
			}
	    }
	}

	return objs;
    }

    public static byte[] put(final List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty())
		    throw new IllegalArgumentException("List<PCEPObject> is mandatory and can't be empty.");

		final List<byte[]> listBytes = new ArrayList<byte[]>();

		byte[] bytes;
		int size = 0;
		for (final PCEPObject obj : objects) {
		    bytes = put(obj);
		    size += bytes.length;
		    listBytes.add(bytes);
		}

		final byte[] retBytes = new byte[size];

		int offset = 0;
		for (final byte[] bs : listBytes) {
		    ByteArray.copyWhole(bs, retBytes, offset);
		    offset += bs.length;
		}

		return retBytes;
    }

    private static byte[] put(final PCEPObject obj) {

		byte[] objBody;
		ObjectClass objClass;
		int objType = 1;

		if (obj instanceof PCEPOpenObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.OPEN;
		} else if (obj instanceof PCEPRequestParameterObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.RP;
		} else if (obj instanceof PCEPNoPathObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.NO_PATH;
		} else if (obj instanceof PCEPRequestedPathBandwidthObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.BANDWIDTH;
		} else if (obj instanceof PCEPExistingPathBandwidthObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.BANDWIDTH;
		    objType = 2;
		} else if (obj instanceof PCEPMetricObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.METRIC;
		} else if (obj instanceof PCEPEndPointsObject<?>) {
		    objClass = PCEPObjectIdentifier.ObjectClass.END_POINTS;
		    if (((PCEPEndPointsObject<?>) obj).getSourceAddress() instanceof IPv6Address) {
			objType = 2;
		    } else if (!(((PCEPEndPointsObject<?>) obj).getSourceAddress() instanceof IPv4Address))
			throw new IllegalArgumentException("Unknown instance of Source Address.");

		} else if (obj instanceof PCEPP2MPEndPointsObject<?>) {
		    objClass = PCEPObjectIdentifier.ObjectClass.END_POINTS;
		    objType = 3;
		    if (((PCEPP2MPEndPointsObject<?>) obj).getSourceAddress() instanceof IPv6Address) {
			objType = 4;
		    } else if (!(((PCEPP2MPEndPointsObject<?>) obj).getSourceAddress() instanceof IPv4Address))
			throw new IllegalArgumentException("Unknown instance of Source Address.");

		} else if (obj instanceof PCEPUnreachedDestinationObject<?>) {
		    objClass = PCEPObjectIdentifier.ObjectClass.UNREACHED_DESTINATION;
		    if (((PCEPUnreachedDestinationObject<?>) obj).getUnreachedDestinations().get(0) instanceof IPv6Address) {
			objType = 2;
		    } else if (!(((PCEPUnreachedDestinationObject<?>) obj).getUnreachedDestinations().get(0) instanceof IPv4Address))
			throw new IllegalArgumentException("Unknown instance of Source Address.");

		} else if (obj instanceof PCEPLspaObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.LSPA;
		} else if (obj instanceof PCEPSvecObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.SVEC;
		    objType = 1;
		} else if (obj instanceof PCEPNotificationObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.NOTIFICATION;
		} else if (obj instanceof PCEPErrorObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.ERROR;
		} else if (obj instanceof PCEPCloseObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.CLOSE;
		} else if (obj instanceof PCEPLoadBalancingObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.LOAD_BALANCING;
		} else if (obj instanceof PCEPLspObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.LSP;
		} else if (obj instanceof PCEPExplicitRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.ERO;
		} else if (obj instanceof PCEPReportedRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.RRO;
		} else if (obj instanceof PCEPIncludeRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.IRO;
		} else if (obj instanceof PCEPExcludeRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.XRO;
		} else if (obj instanceof PCEPObjectiveFunctionObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.OBJCETIVE_FUNCTION;
		} else if (obj instanceof PCEPGlobalConstraintsObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.GLOBAL_CONSTRAINTS;
		} else if (obj instanceof PCEPBranchNodeObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.BRANCH_NODE;
		    if (obj instanceof PCEPNonBranchNodeListObject) {
			objType = 2;
		    } else if (!(obj instanceof PCEPBranchNodeListObject))
			throw new IllegalArgumentException("Unknown instance of PCEPBranchNodeObject.");
		} else if (obj instanceof PCEPSecondaryExplicitRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.SERO;
		} else if (obj instanceof PCEPSecondaryRecordRouteObject) {
		    objClass = PCEPObjectIdentifier.ObjectClass.SRRO;
		} else
		    throw new IllegalArgumentException("Unknown instance of PCEPObject.");

		final PCEPObjectParser objParserClass = MapOfParsers.getInstance().get(new PCEPObjectIdentifier(objClass, objType));
		objBody = objParserClass.put(obj);

		final byte[] objHeader = PCEPObjectHeader.putHeader(new PCEPObjectHeader(objClass.getIdentifier(), objType, objBody.length
			+ PCEPObjectHeader.COMMON_OBJECT_HEADER_LENGTH, obj.isProcessed(), obj.isIgnored()));

		assert objBody.length % 4 == 0 : "Wrong length of PCEPObjectBody. Passed object has length: " + objBody.length + " that is not multiple of 4.";

		final byte[] retBytes = new byte[objHeader.length + objBody.length];
		ByteArray.copyWhole(objHeader, retBytes, 0);
		ByteArray.copyWhole(objBody, retBytes, PCEPObjectHeader.OBJ_BODY_OFFSET);

		return retBytes;
    }
}
