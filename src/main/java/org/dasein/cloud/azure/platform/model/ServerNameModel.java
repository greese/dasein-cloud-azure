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

import javax.xml.bind.annotation.*;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
@XmlRootElement(name="ServerName", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerNameModel {
    @XmlValue
    private String name;
    @XmlAttribute(name="FullyQualifiedDomainName", namespace ="http://schemas.microsoft.com/sqlazure/2010/12/")
    private String fullyQualifiedName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }
}
