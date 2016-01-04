/*
 * Copyright (c) 2014 TATA Consultancy Services.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.sdniwrapper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NetworkCapabilitiesQOS {

    @XmlElement
    private String receivePackets;
    @XmlElement
    private String transmitPackets;
    @XmlElement
    private String collisionCount;
    @XmlElement
    private String receiveFrameError;
    @XmlElement
    private String receiveOverRunError;
    @XmlElement
    private String receiveCrcError;
    @XmlElement
    private String node;
    @XmlElement
    private String port;
    @XmlElement
    private String controller;


    public NetworkCapabilitiesQOS() {
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getReceivePackets() {
        return receivePackets;
    }

    public void setReceivePackets(String receivePackets) {
        this.receivePackets = receivePackets;
    }

    public String getTransmitPackets() {
        return transmitPackets;
    }

    public void setTransmitPackets(String transmitPackets) {
        this.transmitPackets = transmitPackets;
    }

    public String getCollisionCount() {
        return collisionCount;
    }

    public void setCollisionCount(String collisionCount) {
        this.collisionCount = collisionCount;
    }

    public String getReceiveFrameError() {
        return receiveFrameError;
    }

    public void setReceiveFrameError(String receiveFrameErrors) {
        this.receiveFrameError = receiveFrameErrors;
    }

    public String getReceiveOverRunError() {
        return receiveOverRunError;
    }

    public void setReceiveOverRunError(String receiveOverRunErrors) {
        this.receiveOverRunError = receiveOverRunErrors;
    }

    public String getReceiveCrcError() {
        return receiveCrcError;
    }

    public void setReceiveCrcError(String receiveCrcErrors) {
        this.receiveCrcError = receiveCrcErrors;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("receivePackets ").append(receivePackets);
        sb.append(", transmitPackets ").append(transmitPackets);
        sb.append(", collisionCount ").append(collisionCount);
        sb.append(", receiveFrameError ").append(receiveFrameError);
        sb.append(", receiveOverRunError ").append(receiveOverRunError);
        sb.append(", receiveCrcError ").append(receiveCrcError);
        sb.append(", node ").append(node);
        sb.append(", port ").append(port);
        return sb.toString();
    }
}
