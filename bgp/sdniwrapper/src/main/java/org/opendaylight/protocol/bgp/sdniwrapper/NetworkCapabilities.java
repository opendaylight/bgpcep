/*
 * Copyright (c) 2014 TATA Consultancy Services.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.sdniwrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class NetworkCapabilities {

    @XmlElement
    private List<String> link = new ArrayList<String>();
    @XmlElement
    private List<String> bandwidth = new ArrayList<String>();
    @XmlElement
    private List<String> latency = new ArrayList<String>();
    @XmlElement
    private List<String> macAddressList = new ArrayList<String>();
    @XmlElement
    private List<String> ipAddressList = new ArrayList<String>();
    @XmlElement
    private List<String> controller = new ArrayList<String>();
    @XmlElement
    private List<String> node = new ArrayList<String>();
    @XmlElement
    private List<String> host = new ArrayList<String>();
    @XmlElement
    private String timeStamp = new String();


    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public List<String> getLink() {
        return link;
    }

    public void setLink(List<String> links) {
        this.link = links;
    }

    public void addLink(String links) {
        link.add(links);
    }

    public List<String> getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(List<String> bandwidths) {
        this.bandwidth = bandwidths;
    }

    public void addBandwidth(String bandwidth) {
        this.bandwidth.add(bandwidth);
    }

    public List<String> getLatencies() {
        return latency;
    }

    public void setLatency(List<String> latencies) {
        this.latency = latencies;
    }

    public void addLatency(String latency) {
        this.latency.add(latency);
    }

    public void addController(String controller) {
        this.controller.add(controller);
    }

    public void addNode(String node) {
        this.node.add(node);
        Collections.sort(this.node);
    }

    public void addHost(String host) {
        this.host.add(host);
    }

    public List<String> getController() {
        return controller;
    }

    public void setController(List<String> controllers) {
        this.controller = controllers;
    }

    public List<String> getNode() {
        return node;
    }

    public void setNode(List<String> nodes) {
        this.node = nodes;
    }

    public List<String> getHost() {
        return host;
    }

    public void setHost(List<String> hosts) {
        this.host = hosts;
    }

    public void addMacAddress(String macAddress1) {
        macAddressList.add(macAddress1);
        Collections.sort(this.macAddressList);
    }

    public void addIpAddress(String ipAddress1) {
        ipAddressList.add(ipAddress1);
    }

    public List<String> getMacAddressList() {
        return macAddressList;
    }

    public void setMacAddressList(List<String> macAddress) {
        this.macAddressList = macAddress;
    }

    public List<String> getIpAddressList() {
        return ipAddressList;
    }

    public void setIpAddressList(List<String> ipAddress) {
        this.ipAddressList = ipAddress;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Controllers ").append(controller);
        sb.append(", Links ").append(link);
        sb.append(", Nodes ").append(node);
        sb.append(", Hosts ").append(host);
        sb.append(", Bandwidths ").append(bandwidth);
        sb.append(", Latencies ").append(latency);
        sb.append(", MacAddressList ").append(macAddressList);
        sb.append(", IpAddressList ").append(ipAddressList);
        return sb.toString();
    }
}
