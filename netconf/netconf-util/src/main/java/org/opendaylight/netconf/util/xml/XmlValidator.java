/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.util.xml;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;

public class XmlValidator {
	static final Schema schema;

	static {
		final InputStream xmlSchema = XmlValidator.class.getResourceAsStream("/xml.xsd");
		Preconditions.checkNotNull(xmlSchema, "Cannot find xml.xsd");

		final InputStream rfc4714Schema = XmlValidator.class.getResourceAsStream("/rfc4741.xsd");
		Preconditions.checkNotNull(rfc4714Schema, "Cannot find rfc4741.xsd");
		schema = XMLUtil.loadSchema(xmlSchema, rfc4714Schema);
	}

	public static void validate(Document inputDocument) throws SAXException, IOException {
		final Validator validator = schema.newValidator();
		final Source source = new DOMSource(inputDocument);
		validator.validate(source);
	}
}
