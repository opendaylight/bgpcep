/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;


/**
 * PCEPRequestMessage validator. Validates message integrity.
 */
// FIXME: merge with parser
class PCEPRequestMessageValidator {

	// @Override
	// public List<Message> validate(final List<Object> objects) {
	// if (objects == null)
	// throw new IllegalArgumentException("Passed list can't be null.");
	//
	// final List<Message> msgs = Lists.newArrayList();
	// final List<CompositeRequestSvecObject> svecList = new ArrayList<CompositeRequestSvecObject>();
	//
	// CompositeRequestSvecObject svecComp;
	// while (!objects.isEmpty()) {
	// try {
	// if ((svecComp = getValidSvecComposite(objects)) == null)
	// break;
	// } catch (final PCEPDocumentedException e) {
	// msgs.add(new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
	// return msgs;
	// }
	//
	// svecList.add(svecComp);
	// }
	//
	// while (!objects.isEmpty()) {
	// final List<CompositeRequestObject> requests = new ArrayList<CompositeRequestObject>();
	// PCEPRequestParameterObject rpObj = null;
	// boolean requestRejected = false;
	//
	// if (objects.get(0) instanceof PCEPRequestParameterObject) {
	// rpObj = (PCEPRequestParameterObject) objects.get(0);
	// objects.remove(rpObj);
	// if (!rpObj.isProcessed()) {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(rpObj, new PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	// requestRejected = true;
	// }
	//
	// } else {
	// // if RP obj is missing return error only;
	// msgs.clear();
	// msgs.add(new PCEPErrorMessage(new PCEPErrorObject(PCEPErrors.RP_MISSING)));
	// return msgs;
	// }
	//
	// PCEPEndPointsObject<?> endPoints = null;
	// if (objects.get(0) instanceof PCEPEndPointsObject<?>) {
	// endPoints = (PCEPEndPointsObject<?>) objects.get(0);
	// objects.remove(0);
	// if (!endPoints.isProcessed()) {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new
	// PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	// requestRejected = true;
	// }
	// } else {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new
	// PCEPErrorObject(PCEPErrors.END_POINTS_MISSING))));
	// requestRejected = true;
	// }
	//
	// // ignore all continual end-points objects
	// while (!objects.isEmpty() && objects.get(0) instanceof PCEPEndPointsObject<?>) {
	// objects.remove(0);
	// }
	//
	// PCEPClassTypeObject classType = null;
	// PCEPLspObject lsp = null;
	// PCEPLspaObject lspa = null;
	// PCEPRequestedPathBandwidthObject bandwidth = null;
	// final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
	// PCEPReportedRouteObject rro = null;
	// PCEPExistingPathBandwidthObject rroBandwidth = null;
	// PCEPIncludeRouteObject iro = null;
	// PCEPLoadBalancingObject loadBalancing = null;
	//
	// int state = 1;
	// while (!objects.isEmpty()) {
	// final Object obj = objects.get(0);
	// if (obj instanceof UnknownObject) {
	// if (((UnknownObject) obj).isProcessingRule()) {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new PCEPErrorObject(((UnknownObject)
	// obj).getError()))));
	// requestRejected = true;
	// }
	//
	// objects.remove(0);
	// continue;
	// }
	// switch (state) {
	// case 1:
	// state = 2;
	// if (obj instanceof PCEPClassTypeObject) {
	// classType = (PCEPClassTypeObject) obj;
	// if (!classType.isProcessed()) {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new
	// PCEPErrorObject(PCEPErrors.P_FLAG_NOT_SET))));
	// requestRejected = true;
	// }
	// break;
	// }
	// case 2:
	// state = 3;
	// if (obj instanceof PCEPLspObject) {
	// lsp = (PCEPLspObject) obj;
	// break;
	// }
	// case 3:
	// state = 4;
	// if (obj instanceof PCEPLspaObject) {
	// lspa = (PCEPLspaObject) obj;
	// break;
	// }
	// case 4:
	// state = 5;
	// if (obj instanceof PCEPRequestedPathBandwidthObject) {
	// bandwidth = (PCEPRequestedPathBandwidthObject) obj;
	// break;
	// }
	// case 5:
	// state = 6;
	// if (obj instanceof PCEPMetricObject) {
	// metrics.add((PCEPMetricObject) obj);
	// state = 5;
	//
	// break;
	// }
	// case 6:
	// state = 8;
	// if (obj instanceof PCEPReportedRouteObject) {
	// rro = (PCEPReportedRouteObject) obj;
	// state = 7;
	// break;
	// }
	// case 7:
	// state = 8;
	// if (obj instanceof PCEPExistingPathBandwidthObject) {
	// rroBandwidth = (PCEPExistingPathBandwidthObject) obj;
	// break;
	// }
	// case 8:
	// state = 9;
	// if (obj instanceof PCEPIncludeRouteObject) {
	// iro = (PCEPIncludeRouteObject) obj;
	// break;
	// }
	// case 9:
	// if (obj instanceof PCEPLoadBalancingObject) {
	// loadBalancing = (PCEPLoadBalancingObject) obj;
	// break;
	// }
	// state = 10;
	// }
	//
	// if (state == 10) {
	// break;
	// }
	//
	// objects.remove(obj);
	// }
	//
	// if (rpObj.isReoptimized() && bandwidth != null && bandwidth.getBandwidth() != new Bandwidth(new byte[] { 0 }) &&
	// rro == null) {
	// msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new
	// PCEPErrorObject(PCEPErrors.RRO_MISSING))));
	// requestRejected = true;
	// }
	//
	// if (!requestRejected) {
	// requests.add(new CompositeRequestObject(rpObj, endPoints, classType, lsp, lspa, bandwidth, metrics, rro,
	// rroBandwidth, iro, loadBalancing));
	// msgs.add(new PCEPRequestMessage(Collections.unmodifiableList(svecList), Collections.unmodifiableList(requests)));
	// }
	// }
	//
	// return msgs;
	// }
	//
	// private static CompositeRequestSvecObject getValidSvecComposite(final List<Object> objects) throws
	// PCEPDocumentedException {
	// if (objects == null || objects.isEmpty()) {
	// throw new IllegalArgumentException("List cannot be null or empty.");
	// }
	//
	// PCEPSvecObject svec = null;
	// if (objects.get(0) instanceof PCEPSvecObject) {
	// svec = (PCEPSvecObject) objects.get(0);
	// objects.remove(svec);
	// } else
	// return null;
	//
	// PCEPObjectiveFunctionObject of = null;
	// PCEPGlobalConstraintsObject gc = null;
	// PCEPExcludeRouteObject xro = null;
	// final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();
	//
	// int state = 1;
	// while (!objects.isEmpty()) {
	// final Object obj = objects.get(0);
	//
	// if (obj instanceof UnknownObject && ((UnknownObject) obj).isProcessingRule()) {
	// throw new PCEPDocumentedException("Unknown object in SVEC list.", ((UnknownObject) obj).getError());
	// }
	//
	// switch (state) {
	// case 1:
	// state = 2;
	// if (obj instanceof PCEPObjectiveFunctionObject) {
	// of = (PCEPObjectiveFunctionObject) obj;
	// break;
	// }
	// case 2:
	// state = 3;
	// if (obj instanceof PCEPGlobalConstraintsObject) {
	// gc = (PCEPGlobalConstraintsObject) obj;
	// break;
	// }
	// case 3:
	// state = 4;
	// if (obj instanceof PCEPExcludeRouteObject) {
	// xro = (PCEPExcludeRouteObject) obj;
	// break;
	// }
	// case 4:
	// state = 5;
	// if (obj instanceof PCEPMetricObject) {
	// metrics.add((PCEPMetricObject) obj);
	// state = 4;
	//
	// break;
	// }
	// }
	//
	// if (state == 5)
	// break;
	//
	// objects.remove(obj);
	// }
	//
	// return new CompositeRequestSvecObject(svec, of, gc, xro, metrics);
	// }
	//
	// private static PCEPRequestParameterObject copyRP(final PCEPRequestParameterObject origRp, final boolean
	// processed) {
	// return new PCEPRequestParameterObject(origRp.isLoose(), origRp.isBidirectional(), origRp.isReoptimized(),
	// origRp.isMakeBeforeBreak(), origRp.isReportRequestOrder(), origRp.isSuplyOFOnResponse(),
	// origRp.isFragmentation(), origRp.isP2mp(), origRp.isEroCompression(), origRp.getPriority(),
	// origRp.getRequestID(), origRp.getTlvs(), processed, origRp.isIgnored());
	// }
}
