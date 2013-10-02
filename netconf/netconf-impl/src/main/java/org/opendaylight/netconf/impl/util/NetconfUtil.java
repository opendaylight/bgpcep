package org.opendaylight.netconf.impl.util;

import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.util.xml.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;

// TODO purge nulls
public class NetconfUtil {

	private static final Logger logger = LoggerFactory.getLogger(NetconfUtil.class);

	public static NetconfMessage createMessage(final File f) {
		try {
			return createMessage(new FileInputStream(f));
		} catch (final FileNotFoundException e) {
			logger.warn("File {} not found.", f, e);
		}
		return null;
	}

	public static NetconfMessage createMessage(final InputStream is) {
		Document doc = null;
		try {
			doc = XMLUtil.parse(is);
		} catch (final IOException e) {
			logger.warn("Error ocurred while parsing stream.", e);
		} catch (final SAXException e) {
			logger.warn("Error ocurred while final parsing stream.", e);
		}
		return (doc == null) ? null : new NetconfMessage(doc);
	}
}
