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
