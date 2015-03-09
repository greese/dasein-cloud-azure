/**
 * Copyright (C) 2013-2015 Dell, Inc
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

package org.dasein.cloud.azure.platform.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vmunthiu on 1/14/2015.
 */

/*
<ServiceResource>
<Name>AutomatedSqlExport_vladtestdb_20150114T100004Z</Name>
<Type>Microsoft.SqlAzure.RecoverableDatabase</Type>
<State>Recoverable</State>
<SelfLink>https://management.core.windows.net/b9d9ea22-2a54-4b66-8d59-9156b3331ae9/services/sqlservers/servers/boghorr5it/recoverabledatabases/AutomatedSqlExport_vladtestdb_20150114T100004Z</SelfLink>
<ParentLink>https://management.core.windows.net/b9d9ea22-2a54-4b66-8d59-9156b3331ae9/services/sqlservers/servers/boghorr5it</ParentLink>
<EntityId>AutomatedSqlExport_vladtestdb_20150114T100004Z,2015-01-14T10-05-34</EntityId>
<ServerName>boghorr5it</ServerName>
<Edition>Standard</Edition>
<LastAvailableBackupDate>2015-01-14T10:05:34.0000000Z</LastAvailableBackupDate>
 </ServiceResource>
*/
@XmlRootElement(name="ServiceResource", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecoverableDatabaseModel {
    @XmlElement(name="Name", namespace ="http://schemas.microsoft.com/windowsazure")
    private String name;
    @XmlElement(name="Type", namespace ="http://schemas.microsoft.com/windowsazure")
    private String type;
    @XmlElement(name="State", namespace ="http://schemas.microsoft.com/windowsazure")
    private String state;
    @XmlElement(name="EntityId", namespace ="http://schemas.microsoft.com/windowsazure")
    private String entityId;
    @XmlElement(name="ServerName", namespace ="http://schemas.microsoft.com/windowsazure")
    private String serverName;
    @XmlElement(name="LastAvailableBackupDate", namespace ="http://schemas.microsoft.com/windowsazure")
    private String lastAvailableBackupDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getLastAvailableBackupDate() {
        return lastAvailableBackupDate;
    }

    public void setLastAvailableBackupDate(String lastAvailableBackupDate) {
        this.lastAvailableBackupDate = lastAvailableBackupDate;
    }
}
