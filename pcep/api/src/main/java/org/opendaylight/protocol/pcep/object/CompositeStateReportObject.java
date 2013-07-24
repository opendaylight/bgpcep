/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPObject;

/**
 * Structure that combines set of related objects.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-crabbe-pce-stateful-pce-02#section-6.1">The
 *      PCRpt Message</a> - &lt;state-report&gt;</br>
 */
public class CompositeStateReportObject {

	private final PCEPLspObject lsp;

	private List<CompositeRptPathObject> paths;

	/**
	 * Constructs basic composite object only with mandatory objects.
	 *
	 * @param lsp
	 *            PCEPLspObject
	 */
	public CompositeStateReportObject(PCEPLspObject lsp) {
		this(lsp, null);
	}

	/**
	 * Constructs composite object with optional objects.
	 *
	 * @param lsp
	 *            PCEPLspObject
	 * @param paths
	 *            List<CompositeRptPathObject> Can't be null.
	 */
	public CompositeStateReportObject(PCEPLspObject lsp, List<CompositeRptPathObject> paths) {
		if (lsp == null)
			throw new IllegalArgumentException("LSP Object is mandatory.");
		this.lsp = lsp;
		if (paths != null)
			this.paths = paths;
		else
			this.paths = Collections.emptyList();
	}

	/**
	 * Creates this object from a list of PCEPObjects.
	 * @param objects List<PCEPObject> list of PCEPObjects from whose this
	 * object should be created.
	 * @return CompositeStateReportObject
	 */
	public static CompositeStateReportObject getCompositeFromList(List<PCEPObject> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List can not be null or empty.");
		}

		PCEPLspObject lsp = null;
		if (objects.get(0) instanceof PCEPLspObject) {
			lsp = (PCEPLspObject) objects.get(0);
			objects.remove(lsp);
		} else
			return null;

		final List<CompositeRptPathObject> paths = new ArrayList<CompositeRptPathObject>();

		if (!objects.isEmpty()) {
			CompositeRptPathObject path = CompositeRptPathObject.getCompositeFromList(objects);
			while (path != null) {
				paths.add(path);
				if (objects.isEmpty())
					break;
				path = CompositeRptPathObject.getCompositeFromList(objects);
			}
		}

		return new CompositeStateReportObject(lsp, paths);
	}

	/**
	 * Gets list of all objects, which are in appropriate order.
	 *
	 * @return List<PCEPObject>
	 */
	public List<PCEPObject> getCompositeAsList() {
		final List<PCEPObject> list = new ArrayList<PCEPObject>();
		list.add(this.lsp);
		if (this.paths != null && !this.paths.isEmpty())
			for (final CompositeRptPathObject cpo : this.paths)
				list.addAll(cpo.getCompositeAsList());
		return list;
	}

	/**
	 * Gets {@link PCEPLspObject}.
	 *
	 * @return PCEPLspObject. Can't be null.
	 */
	public PCEPLspObject getLsp() {
		return this.lsp;
	}

	/**
	 * Gets list of {@link CompositeRptPathObject}.
	 *
	 * @return List<CompositeRptPathObject>. May be null.
	 */
	public List<CompositeRptPathObject> getPaths() {
		return this.paths;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.lsp == null) ? 0 : this.lsp.hashCode());
		result = prime * result + ((this.paths == null) ? 0 : this.paths.hashCode());
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
		final CompositeStateReportObject other = (CompositeStateReportObject) obj;
		if (this.lsp == null) {
			if (other.lsp != null)
				return false;
		} else if (!this.lsp.equals(other.lsp))
			return false;
		if (this.paths == null) {
			if (other.paths != null)
				return false;
		} else if (!this.paths.equals(other.paths))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompositeStateReportObject [lsp=");
		builder.append(this.lsp);
		builder.append(", paths=");
		builder.append(this.paths);
		builder.append("]");
		return builder.toString();
	}
}
