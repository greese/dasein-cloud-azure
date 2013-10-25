package org.dasein.cloud.test.compute.vm;

import mockit.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureLocation;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.VMScalingOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.test.TestHelpers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Created with IntelliJ IDEA.
 * User: sdorzak
 * Date: 06/09/2013
 * Time: 11:32
 * To change this template use File | Settings | File Templates.
 */
public class AzureVmTest {
    @Tested AzureVM sut;
    @Mocked ProviderContext providerContextMock;
    @Mocked AzureMethod azureMethodMock;
    @Mocked Azure azureMock;
    @Mocked AzureLocation azureLocationMock;
    @Mocked DataCenter dataCenterMock;

    final String ACCOUNT_NO = "12323232323";
    final String REGION = "East US";
    final String SERVICE_NAME = "svctest1425";
    final String DEPLOYMENT_NAME = "depltest1425";
    final String ROLE_NAME = "roletest1425";
    final String VM_NAME = "dmtest1425";
    final String VM_ID = SERVICE_NAME + ":" + DEPLOYMENT_NAME + ":" + ROLE_NAME;

    @Before
    public void setUp() {

        new NonStrictExpectations() {
            { azureMock.getContext(); result = providerContextMock; }
            { azureMock.getDataCenterServices(); result = azureLocationMock; }
            { Azure.getLogger(AzureVM.class); result = Logger.getLogger(anyString); }
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getRegionId(); result = REGION; }
        };

        sut = new AzureVM(azureMock);
    }


    @Test
    public void getVirtualMachineShouldReturnVirtualMachineObject() throws CloudException, InternalException {


        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(ACCOUNT_NO, AzureVM.HOSTED_SERVICES + "/" + SERVICE_NAME); result = TestHelpers.getMockXmlResponse("/xmlResponses/HostedService.xml"); }
            { azureMethodMock.getAsXML(ACCOUNT_NO, AzureVM.HOSTED_SERVICES + "/"+SERVICE_NAME+"/deployments/" + DEPLOYMENT_NAME); result = TestHelpers.getMockXmlResponse("/xmlResponses/HostedService-Deployment.xml"); }
        };

        final Collection<DataCenter> dcs = new ArrayList<DataCenter>(Arrays.asList(dataCenterMock));

        new NonStrictExpectations() {
            { dataCenterMock.getProviderDataCenterId(); result = REGION; }
            { dataCenterMock.getRegionId(); result = REGION; }
            { azureLocationMock.listDataCenters(REGION); result = dcs; }
            { azureLocationMock.getDataCenter(anyString); result = dataCenterMock; }
        };

        VirtualMachine result = sut.getVirtualMachine(VM_ID);

        assertNotNull("Virtual machine shouldn't be null", result);
        assertEquals("Invalid vm service name", SERVICE_NAME, result.getTag("serviceName"));         //TODO: replace these magic strings with enums
        assertEquals("Invalid vm deployment name", DEPLOYMENT_NAME, result.getTag("deploymentName"));
        assertEquals("Invalid vm role name", ROLE_NAME, result.getTag("roleName"));

    }

    @Test
    public void startShouldPostCorrectRequest()throws CloudException, InternalException{

        sut = new AzureVmPartialMock(azureMock);

        final String expectedPost = TestHelpers.getStringXmlRequest("/xmlRequests/StartRoleOperation.xml");

        new NonStrictExpectations() {
            { azureMethodMock.post(ACCOUNT_NO,
                    String.format("%1$s/%2$s/deployments/%3$s/roleInstances/%4$s/Operations", AzureVM.HOSTED_SERVICES, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME),
                    expectedPost); times = 1; }
        };

        sut.start(VM_ID);
    }


    @Test
    @Ignore("This test will fail because we do not update the xml correctly - what gets posted is ExtraSmall instead of Large")
    public void alterVmShouldPostCorrectRequest()throws CloudException, InternalException{

        final VirtualMachine vm = new VirtualMachine();
        vm.addTag("serviceName", SERVICE_NAME);
        vm.addTag("deploymentName", DEPLOYMENT_NAME);
        vm.addTag("roleName", ROLE_NAME);

        sut = new AzureVmPartialMock(azureMock);

        final String vmUrl = String.format("%1$s/%2$s/deployments/%3$s/roles/%4$s", AzureVM.HOSTED_SERVICES, SERVICE_NAME, DEPLOYMENT_NAME, ROLE_NAME);

        final String role_request_id = "role-request-id-123456";
        final String disk_request_id = "disk-request-id-123456";

        new MockUp<System>() {
            @Mock @SuppressWarnings("unused") long currentTimeMillis() { return 1378777034215l; }
        };

        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(ACCOUNT_NO, vmUrl); result = TestHelpers.getMockXmlResponse("/xmlResponses/HostedService-Role-ExtraSmall.xml"); }

            //request xml should be changed to Large from ExtraSmall
            { azureMethodMock.invoke("PUT", ACCOUNT_NO, vmUrl, TestHelpers.getStringXmlRequestAsSingleLine("/xmlRequests/HostedService-Role-Large.xml")); result = role_request_id; times = 1; }
            { azureMethodMock.getOperationStatus(role_request_id); result = 200; times = 1;}

            //because we're modfying disks too - the correct request should be sent
            { azureMock.getStorageEndpoint(); result = "http://portalvhdsc8vf26rnz7rnm.blob.core.windows.net/";}
            { azureMethodMock.post(ACCOUNT_NO, vmUrl+ "/DataDisks", TestHelpers.getStringXmlRequestAsSingleLine("/xmlRequests/DataVirtualHardDisk.xml")); result = disk_request_id; times = 1; }
            { azureMethodMock.getOperationStatus(disk_request_id); result = 200; times = 1;}
        };

        VMScalingOptions options = VMScalingOptions.getInstance("Large:[20]");

        sut.alterVirtualMachine(VM_ID, options);
    }

    @Test
    @Ignore("work in progress")
    public void listVirtualMachineStatusShouldReturnCorrectList()throws CloudException, InternalException{
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(ACCOUNT_NO, AzureVM.HOSTED_SERVICES); result = TestHelpers.getMockXmlResponse("/xmlResponses/HostedServices.xml"); }

            { azureLocationMock.getDataCenter(REGION); result = dataCenterMock; }
            { dataCenterMock.getRegionId(); result = REGION; }
        };

        Iterable<ResourceStatus> result = sut.listVirtualMachineStatus();


    }


    /*
        Using partial mock works as well eg.
            new NonStrictExpectations(sut) {
                { invoke(sut, "getVirtualMachine", VM_ID); result = vm; times = 1;}
            };

        but it messes up the debug lines, so until they fix-it I think it's better to use this way
    */
    class AzureVmPartialMock extends AzureVM{
        public AzureVmPartialMock(Azure provider) {
            super(provider);
        }

        public VirtualMachine getVirtualMachine(String vmId){
            final VirtualMachine vm = new VirtualMachine();
            vm.addTag("serviceName", SERVICE_NAME);
            vm.addTag("deploymentName", DEPLOYMENT_NAME);
            vm.addTag("roleName", ROLE_NAME);

            return vm;
        }
    }
}
