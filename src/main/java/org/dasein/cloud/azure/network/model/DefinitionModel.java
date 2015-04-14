/**
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azure.network.model;

import org.apache.http.client.methods.HttpOptions;

import javax.xml.bind.annotation.*;
import java.security.Policy;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 6/16/2014.
 */

@XmlRootElement(name="Definition", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefinitionModel {

    @XmlElement(name="DnsOptions", namespace ="http://schemas.microsoft.com/windowsazure")
    private DnsOptions dnsOptions;
    @XmlElement(name="Status", namespace ="http://schemas.microsoft.com/windowsazure")
    private String status;
    @XmlElement(name="Version", namespace ="http://schemas.microsoft.com/windowsazure")
    private String version;
    @XmlElementWrapper(name = "Monitors", namespace = "http://schemas.microsoft.com/windowsazure")
    @XmlElement(name="Monitor", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<MonitorModel> monitors;
    @XmlElement(name="Policy", namespace ="http://schemas.microsoft.com/windowsazure")
    private PolicyModel policy;

    public DnsOptions getDnsOptions() {
        return dnsOptions;
    }

    public void setDnsOptions(DnsOptions dnsOptions) {
        this.dnsOptions = dnsOptions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<MonitorModel> getMonitors() {
        return monitors;
    }

    public void setMonitors(List<MonitorModel> monitors) {
        this.monitors = monitors;
    }

    public PolicyModel getPolicy() {
        return policy;
    }

    public void setPolicy(PolicyModel policy) {
        this.policy = policy;
    }

    @XmlRootElement(name="DnsOptions", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class DnsOptions {
        @XmlElement(name="TimeToLiveInSeconds", namespace ="http://schemas.microsoft.com/windowsazure")
        private String timeToLiveInSeconds;

        public String getTimeToLiveInSeconds() {
            return timeToLiveInSeconds;
        }

        public void setTimeToLiveInSeconds(String timeToLiveInSeconds) {
            this.timeToLiveInSeconds = timeToLiveInSeconds;
        }
    }

    @XmlRootElement(name="Endpoint", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EndPointModel {
        @XmlElement(name="DomainName", namespace ="http://schemas.microsoft.com/windowsazure")
        private String domainName;
        @XmlElement(name="Status", namespace ="http://schemas.microsoft.com/windowsazure")
        private String status;
        @XmlElement(name="MonitorStatus", namespace ="http://schemas.microsoft.com/windowsazure")
        private String monitorStatus;
        @XmlElement(name="Type", namespace ="http://schemas.microsoft.com/windowsazure")
        private String type;
        @XmlElement(name="Location", namespace ="http://schemas.microsoft.com/windowsazure")
        private String location;
        @XmlElement(name="Weight", namespace ="http://schemas.microsoft.com/windowsazure")
        private String weight;

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMonitorStatus() {
            return monitorStatus;
        }

        public void setMonitorStatus(String monitorStatus) {
            this.monitorStatus = monitorStatus;
        }

        public String getType() { return type; }

        public void setType(String type) { this.type = type; }

        public String getLocation() { return location; }

        public void setLocation(String location) { this.location = location; }

        public String getWeight() { return weight; }

        public void setWeight(String weight) { this.weight = weight; }
    }

    @XmlRootElement(name="HttpOptions", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HttpOptionsModel {
        @XmlElement(name="Verb", namespace ="http://schemas.microsoft.com/windowsazure")
        private String verb;
        @XmlElement(name="RelativePath", namespace ="http://schemas.microsoft.com/windowsazure")
        private String relativePath;
        @XmlElement(name="ExpectedStatusCode", namespace ="http://schemas.microsoft.com/windowsazure")
        private String expectedStatusCode;

        public String getVerb() {
            return verb;
        }

        public void setVerb(String verb) {
            this.verb = verb;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }

        public String getExpectedStatusCode() {
            return expectedStatusCode;
        }

        public void setExpectedStatusCode(String expectedStatusCode) {
            this.expectedStatusCode = expectedStatusCode;
        }
    }

    @XmlRootElement(name="Monitor", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MonitorModel {
        @XmlElement(name="IntervalInSeconds", namespace ="http://schemas.microsoft.com/windowsazure")
        private String intervalInSeconds;
        @XmlElement(name="TimeoutInSeconds", namespace ="http://schemas.microsoft.com/windowsazure")
        private String timeoutInSeconds;
        @XmlElement(name="ToleratedNumberOfFailures", namespace ="http://schemas.microsoft.com/windowsazure")
        private String toleratedNumberOfFailures;
        @XmlElement(name="Protocol", namespace ="http://schemas.microsoft.com/windowsazure")
        private String protocol;
        @XmlElement(name="Port", namespace ="http://schemas.microsoft.com/windowsazure")
        private String port;
        @XmlElement(name="HttpOptions", namespace ="http://schemas.microsoft.com/windowsazure")
        private HttpOptionsModel httpOptions;

        public String getIntervalInSeconds() {
            return intervalInSeconds;
        }

        public void setIntervalInSeconds(String intervalInSeconds) {
            this.intervalInSeconds = intervalInSeconds;
        }

        public String getTimeoutInSeconds() {
            return timeoutInSeconds;
        }

        public void setTimeoutInSeconds(String timeoutInSeconds) {
            this.timeoutInSeconds = timeoutInSeconds;
        }

        public String getToleratedNumberOfFailures() {
            return toleratedNumberOfFailures;
        }

        public void setToleratedNumberOfFailures(String toleratedNumberOfFailures) {
            this.toleratedNumberOfFailures = toleratedNumberOfFailures;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public HttpOptionsModel getHttpOptions() {
            return httpOptions;
        }

        public void setHttpOptions(HttpOptionsModel httpOptions) {
            this.httpOptions = httpOptions;
        }
    }

    @XmlRootElement(name="Policy", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PolicyModel {
        @XmlElement(name="LoadBalancingMethod", namespace ="http://schemas.microsoft.com/windowsazure")
        private String loadBalancingMethod;
        @XmlElementWrapper(name = "Endpoints", namespace = "http://schemas.microsoft.com/windowsazure")
        @XmlElement(name="Endpoint", namespace ="http://schemas.microsoft.com/windowsazure")
        private List<EndPointModel> endPoints;
        @XmlElement(name="MonitorStatus", namespace ="http://schemas.microsoft.com/windowsazure")
        private String monitorStatus;

        public String getLoadBalancingMethod() {
            return loadBalancingMethod;
        }

        public void setLoadBalancingMethod(String loadBalancingMethod) {
            this.loadBalancingMethod = loadBalancingMethod;
        }

        public List<EndPointModel> getEndPoints() {
            return endPoints;
        }

        public void setEndPoints(List<EndPointModel> endPoints) {
            this.endPoints = endPoints;
        }

        public String getMonitorStatus() {
            return monitorStatus;
        }

        public void setMonitorStatus(String monitorStatus) {
            this.monitorStatus = monitorStatus;
        }
    }
}
