package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by vmunthiu on 1/28/2015.
 */
@XmlRootElement(name="Servers", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServersModel {
    @XmlElement(name="Server", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private List<ServerModel> servers;

    public List<ServerModel> getServers() { return servers; }

    public void setServers(List<ServerModel> servers) { this.servers = servers; }
}
