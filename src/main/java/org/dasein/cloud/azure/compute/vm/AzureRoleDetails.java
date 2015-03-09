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

package org.dasein.cloud.azure.compute.vm;

/**
 * Created by vmunthiu on 12/11/2014.
 */
public class AzureRoleDetails{
    private String serviceName;
    private String deploymentName;
    private String roleName;

    private AzureRoleDetails(String serviceName, String deploymentName, String roleName){
        this.serviceName = serviceName;
        this.deploymentName = deploymentName;
        this.roleName = roleName;
    }

    public static AzureRoleDetails fromString(String id){
        String[] parts = id.split(":");
        String serviceName, deploymentName, roleName;

        if (parts.length == 3)    {
            serviceName = parts[0];
            deploymentName = parts[1];
            roleName= parts[2];
        }
        else if( parts.length == 2 ) {
            serviceName = parts[0];
            deploymentName = parts[1];
            roleName = serviceName;
        }
        else {
            serviceName = id;
            deploymentName = id;
            roleName = id;
        }

        return new AzureRoleDetails(serviceName, deploymentName, roleName);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getRoleName() {
        return roleName;
    }
}
