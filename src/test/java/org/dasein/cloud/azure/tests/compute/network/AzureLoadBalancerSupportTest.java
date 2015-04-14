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

package org.dasein.cloud.azure.tests.compute.network;

import mockit.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.network.AzureLoadBalancerCapabilities;
import org.dasein.cloud.azure.network.AzureLoadBalancerSupport;
import org.dasein.cloud.azure.network.model.DefinitionModel;
import org.dasein.cloud.azure.network.model.ProfileModel;
import org.dasein.cloud.azure.network.model.ProfilesModel;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.network.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mockit.Deencapsulation.getField;
import static org.junit.Assert.* ;
import static org.junit.Assert.assertEquals;

/**
 * Created by Vlad_Munthiu on 6/10/2014.
 */
public class AzureLoadBalancerSupportTest {

    final String ACCOUNT_NO = "12323232323";
    final String ENDPOINT = "myendpoint";
    final String LOAD_BALANCER_ID = "myloadbalancerid";

    private AzureLoadBalancerSupport lbSupport;
    private ProfileModel expectedProfileModel;
    private DefinitionModel expectedDefinitionModel;
    private ProfilesModel expectedProfilesModel;

    @Mocked ProviderContext providerContextMock;
    @Mocked Azure azureMock;
    @Mocked AzureComputeServices azureComputeMoked;
    @Mocked AzureVM azureVMSupportMoked;

    @Before
    public void setUp() {

        new NonStrictExpectations() {
            { azureMock.getContext(); result = providerContextMock; }
            { azureMock.getComputeServices(); result = azureComputeMoked;}
            { azureComputeMoked.getVirtualMachineSupport(); result = azureVMSupportMoked;}
        };

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = ACCOUNT_NO; }
            { providerContextMock.getEndpoint(); result = ENDPOINT;}
        };

        expectedProfileModel = new ProfileModel();
        expectedProfileModel.setName(LOAD_BALANCER_ID);
        expectedProfileModel.setDomainName("myloadbalancerid.trafficmanager.net");

        expectedProfilesModel = new ProfilesModel();
        expectedProfilesModel.setProfiles(new ArrayList<ProfileModel>(Arrays.asList(expectedProfileModel)));

        expectedDefinitionModel = new DefinitionModel();
        expectedDefinitionModel.setStatus("enabled");
        expectedDefinitionModel.setVersion("1");
        DefinitionModel.EndPointModel expectedEndPoint = new DefinitionModel.EndPointModel();
        expectedEndPoint.setDomainName("endpoint1.cloudapp.net");
        expectedEndPoint.setStatus("Enabled");
        expectedEndPoint.setType("CloudService");
        expectedEndPoint.setWeight("1");
        DefinitionModel.PolicyModel expectedPolicyModel = new DefinitionModel.PolicyModel();
        expectedPolicyModel.setLoadBalancingMethod("Performance");
        expectedPolicyModel.setEndPoints(new ArrayList<DefinitionModel.EndPointModel>(Arrays.asList(expectedEndPoint)));
        expectedDefinitionModel.setPolicy(expectedPolicyModel);
        DefinitionModel.MonitorModel expectedMonitorModel = new DefinitionModel.MonitorModel();
        expectedMonitorModel.setProtocol("HTTP");
        expectedMonitorModel.setPort("12345");
        expectedMonitorModel.setIntervalInSeconds("30");
        expectedMonitorModel.setTimeoutInSeconds("10");
        expectedMonitorModel.setToleratedNumberOfFailures("3");
        DefinitionModel.HttpOptionsModel expectedHttpOptions = new DefinitionModel.HttpOptionsModel();
        expectedHttpOptions.setVerb("GET");
        expectedHttpOptions.setRelativePath("MYRELATIVEPATH");
        expectedHttpOptions.setExpectedStatusCode("200");
        expectedMonitorModel.setHttpOptions(expectedHttpOptions);
        expectedDefinitionModel.setMonitors(Arrays.asList(expectedMonitorModel));


        lbSupport = new AzureLoadBalancerSupport(azureMock);
    }

    @Test
    public void testGetDefinition() throws CloudException, InternalException {
        CheckGetMethodCall("getDefinition", DefinitionModel.class, String.format(AzureLoadBalancerSupport.RESOURCE_DEFINITION, LOAD_BALANCER_ID), LOAD_BALANCER_ID);
    }

    @Test
    public void testGetProfile()
    {
        CheckGetMethodCall("getProfile", ProfileModel.class, String.format(AzureLoadBalancerSupport.RESOURCE_PROFILE, LOAD_BALANCER_ID), LOAD_BALANCER_ID);
    }

    @Test
    public void testGetProfiles()
    {
        CheckGetMethodCall("getProfiles", ProfilesModel.class, AzureLoadBalancerSupport.RESOURCE_PROFILES, null);
    }

    private void CheckGetMethodCall(String methodName, final Class expectedClassz, final String expectdResource, String parameter)
    {
        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){ assertEquals(provider, azureMock);}

            @Mock(invocations = 1)
            <T> T get(Class<T> model, String resource){
                assertEquals("AzureMethod#get method not called with correct resource parameter",expectdResource, resource);
                assertEquals("Incorrect type passed in for model", expectedClassz, model);
                return null;
            }
        };

        if(parameter == null)
            Deencapsulation.invoke(lbSupport, methodName);
        else
            Deencapsulation.invoke(lbSupport, methodName, parameter);
    }

    @Test
    public void testRemoveLoadBalancer() throws CloudException, InternalException {

        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){  assertEquals(provider, azureMock);}

            @Mock(invocations = 1)
            String invoke(String expectedMethod, String expectedAccount, String expectedResource, String expectedBody){
                assertEquals(expectedMethod, "DELETE");
                assertEquals(expectedAccount, ACCOUNT_NO);
                assertEquals(expectedResource, String.format(AzureLoadBalancerSupport.RESOURCE_PROFILE, LOAD_BALANCER_ID));
                assertEquals(expectedBody, null);
                return "test";
            }
        };

        lbSupport.removeLoadBalancer(LOAD_BALANCER_ID);
    }

    @Test
    public void testAddServer() throws InternalException, JAXBException, CloudException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
        };

        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){  assertEquals(provider, azureMock);}

            @Mock(invocations = 1)
            String post(String resource, Object definitionResponseModel){
                assertEquals("Post method not called with correct resource string", resource, String.format(AzureLoadBalancerSupport.RESOURCE_DEFINITIONS, LOAD_BALANCER_ID));
                assertEquals("New server not added to the policy definition", ((DefinitionModel)definitionResponseModel).getPolicy().getEndPoints().size(), 2);
                return "test";
            }
        };

        lbSupport.addServers(LOAD_BALANCER_ID, "newserverid");
    }

    @Test
    public void testRemoveServer() throws CloudException, InternalException {

        DefinitionModel.EndPointModel expectedEndPointToRemove = new DefinitionModel.EndPointModel();
        expectedEndPointToRemove.setDomainName("endpointtoremove.cloudapp.net");
        expectedEndPointToRemove.setStatus("Enabled");
        expectedEndPointToRemove.setType("CloudService");
        expectedEndPointToRemove.setWeight("1");

        expectedDefinitionModel.getPolicy().getEndPoints().add(expectedEndPointToRemove);

        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
        };

        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){  assertEquals(provider, azureMock);}

            @Mock(invocations = 1)
            String post(String resource, Object definitionResponseModel){
                assertEquals("Post method not called with correct resource string", resource, String.format(AzureLoadBalancerSupport.RESOURCE_DEFINITIONS, LOAD_BALANCER_ID));
                assertEquals("Server not removed from the policy definition", ((DefinitionModel)definitionResponseModel).getPolicy().getEndPoints().size(), 1);
                assertEquals("A server with the wrong id has been removed", ((DefinitionModel)definitionResponseModel).getPolicy().getEndPoints().get(0).getDomainName(), "endpoint1.cloudapp.net");
                return "test";
            }
        };


        lbSupport.removeServers(LOAD_BALANCER_ID, "endpointtoremove");
    }

    @Test
    public void getLoadBalancerByIdReturnsALoadBalancerWithSameId() throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getProfile", LOAD_BALANCER_ID); result = expectedProfileModel; times = 1;}
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}

        };
        LoadBalancer loadBalancer = lbSupport.getLoadBalancer(LOAD_BALANCER_ID);

        assertNotNull("getLoadBalancer should not return null for valid ids", loadBalancer);
        assertEquals(LOAD_BALANCER_ID, loadBalancer.getProviderLoadBalancerId());
        assertEquals(LOAD_BALANCER_ID, loadBalancer.getName());
        assertEquals(LoadBalancerState.ACTIVE, loadBalancer.getCurrentState());
        assertEquals(LoadBalancerAddressType.DNS, loadBalancer.getAddressType());
        assertEquals(ACCOUNT_NO, loadBalancer.getProviderOwnerId());
    }

    @Test
    @Ignore("This should pass when merge to develop due to bug fixed in LoadBalancerHealthCheck.getInstance")
    public void testlistLBHealthChecks(@Injectable final HealthCheckFilterOptions optionsMoked) throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getProfiles"); result = expectedProfilesModel; times = 1;}
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
            {optionsMoked.matches(withInstanceOf(LoadBalancerHealthCheck.class)); result = true;}
        };

        List<LoadBalancerHealthCheck> actualHCs = (List<LoadBalancerHealthCheck>) lbSupport.listLBHealthChecks(optionsMoked);

        assertNotNull(actualHCs);
        assertEquals(actualHCs.size(), expectedDefinitionModel.getMonitors().size());

        LoadBalancerHealthCheck actualHC = actualHCs.get(0);
        DefinitionModel.MonitorModel expectedMonitor = expectedDefinitionModel.getMonitors().get(0);
        assertEquals(actualHC.getProviderLBHealthCheckId(), LOAD_BALANCER_ID);
        assertEquals(actualHC.getProtocol(), LoadBalancerHealthCheck.HCProtocol.HTTP);
        assertEquals(String.valueOf(actualHC.getPort()), expectedMonitor.getPort());
        assertEquals(String.valueOf(actualHC.getInterval()), expectedMonitor.getIntervalInSeconds());
        assertEquals(String.valueOf(actualHC.getTimeout()), expectedMonitor.getTimeoutInSeconds());
        //uncomment this when merge to develop
        //assertEquals(String.valueOf(actualHC.getUnhealthyCount()), expectedMonitor.getToleratedNumberOfFailures());
        //assertEquals(actualHC.getHealthyCount(), 0);
        assertEquals(actualHC.getPath(), expectedMonitor.getHttpOptions().getRelativePath());
    }

    @Test
    @Ignore("This should pass when merge to develop due to bug fixed in LoadBalancerHealthCheck.getInstance")
    public void testGetLoadBalancerHealthCheck() throws CloudException, InternalException {

        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
        };

        LoadBalancerHealthCheck expectedLBHC = lbSupport.getLoadBalancerHealthCheck(LOAD_BALANCER_ID, LOAD_BALANCER_ID);
        DefinitionModel.MonitorModel expectedMonitor = expectedDefinitionModel.getMonitors().get(0);
        assertEquals(expectedLBHC.getProviderLBHealthCheckId(), LOAD_BALANCER_ID);
        assertEquals(expectedLBHC.getProtocol(), LoadBalancerHealthCheck.HCProtocol.HTTP);
        assertEquals(String.valueOf(expectedLBHC.getPort()), expectedMonitor.getPort());
        assertEquals(String.valueOf(expectedLBHC.getInterval()), expectedMonitor.getIntervalInSeconds());
        assertEquals(String.valueOf(expectedLBHC.getTimeout()), expectedMonitor.getTimeoutInSeconds());
        //uncomment this when merge to develop
        //assertEquals(String.valueOf(expectedLBHC.getUnhealthyCount()), expectedMonitor.getToleratedNumberOfFailures());
        //assertEquals(expectedLBHC.getHealthyCount(), 0);
        assertEquals(expectedLBHC.getPath(), expectedMonitor.getHttpOptions().getRelativePath());
    }

    @Test
    public void testListEndpoints() throws CloudException, InternalException {
        final VirtualMachine vm = new VirtualMachine();
        vm.setPublicDnsAddress("endpoint1.cloudapp.net");
        vm.setProviderVirtualMachineId("virtualmachineid");

        new NonStrictExpectations(lbSupport){
            {azureVMSupportMoked.listVirtualMachines(); result = new ArrayList<VirtualMachine>(Arrays.asList(vm));}
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
        };

        ArrayList<LoadBalancerEndpoint> actualEndPoints = (ArrayList<LoadBalancerEndpoint>) lbSupport.listEndpoints(LOAD_BALANCER_ID);
        assertNotNull(actualEndPoints);
        assertEquals(actualEndPoints.size(), expectedDefinitionModel.getPolicy().getEndPoints().size());
        DefinitionModel.EndPointModel expectedFirstEndPoint = expectedDefinitionModel.getPolicy().getEndPoints().get(0);
        assertEquals(actualEndPoints.get(0).getEndpointValue(), "virtualmachineid");
        assertEquals(actualEndPoints.get(0).getCurrentState(), LbEndpointState.ACTIVE);
        assertEquals(actualEndPoints.get(0).getEndpointType(), LbEndpointType.VM);
    }

    @Test
    public void capabilitiesAreInitializesAndReturnCorrectly() throws CloudException, InternalException {
        AzureLoadBalancerCapabilities capabilities = getField(lbSupport, "capabilities");
        LoadBalancerCapabilities acutalCapabilities = lbSupport.getCapabilities();

        CloudProvider provider = getField(capabilities, "provider");

        assertEquals("azure capabilities initialized with incorrect provider", azureMock, provider);
        assertEquals("getCapabilities doesn't return current capabilities", capabilities, acutalCapabilities);
    }

    @Test
    public void isSubscribedReturnsTrue(@Injectable final Azure azureProvider) throws CloudException, InternalException{
        assertTrue("isSubscribed metthod doesn't return the correct value", lbSupport.isSubscribed());
    }

    @Test
    public void testListLBReturnsEmptyArrayWhenNullProfilesAreRetrieved() throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getProfiles"); result = null; times = 1;}
        };
        Iterable<LoadBalancer> loadBalancers = lbSupport.listLoadBalancers();

        int actualCount = 0;
        while(loadBalancers.iterator().hasNext()) {
            actualCount++;
            loadBalancers.iterator().next();
        }

        assertEquals(0, actualCount);
    }

    @Test
    public void testListLBReturnsEmptyArrayWhenEmptyProfilesCollectionIsRetrieved() throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getProfiles"); result = new ProfilesModel(); times = 1;}
        };
        Iterable<LoadBalancer> loadBalancers = lbSupport.listLoadBalancers();

        int actualCount = 0;
        while(loadBalancers.iterator().hasNext()) {
            actualCount++;
            loadBalancers.iterator().next();
        }

        assertEquals(0, actualCount);
    }

    @Test
    public void testListLBReturnsCorrectListOfLoadBalancers() throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getProfiles"); result = expectedProfilesModel; times = 1;}
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
        };
        ArrayList<LoadBalancer> loadBalancers = (ArrayList<LoadBalancer>) lbSupport.listLoadBalancers();

        assertEquals(1, loadBalancers.size());

        assertNotNull("LoadBalancer should not be null", loadBalancers.get(0));
        assertEquals(LOAD_BALANCER_ID, loadBalancers.get(0).getProviderLoadBalancerId());
        assertEquals(LOAD_BALANCER_ID, loadBalancers.get(0).getName());
        assertEquals(LoadBalancerState.ACTIVE, loadBalancers.get(0).getCurrentState());
        assertEquals(LoadBalancerAddressType.DNS, loadBalancers.get(0).getAddressType());
        assertEquals(ACCOUNT_NO, loadBalancers.get(0).getProviderOwnerId());
    }

    @Test
    public void testModifyLBHealthCheck() throws CloudException, InternalException {
        new NonStrictExpectations(lbSupport){
            {invoke(lbSupport, "getDefinition", LOAD_BALANCER_ID); result = expectedDefinitionModel; times = 1;}
            {invoke(lbSupport, "getLoadBalancerHealthCheck", LOAD_BALANCER_ID, String.class); result = null; times = 1;}
        };

        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){  assertEquals(provider, azureMock);}

            @Mock(invocations = 1)
            String post(String resource, Object definitionResponseModel){
                assertEquals("Post method not called with correct resource string", resource, String.format(AzureLoadBalancerSupport.RESOURCE_DEFINITIONS, LOAD_BALANCER_ID));
                assertEquals("Incorrect number of definitions found", ((DefinitionModel)definitionResponseModel).getMonitors().size(), 1);

                DefinitionModel.MonitorModel actualMonitor = ((DefinitionModel)definitionResponseModel).getMonitors().get(0);
                DefinitionModel.MonitorModel expectedMonitor = expectedDefinitionModel.getMonitors().get(0);
                assertEquals(actualMonitor.getProtocol(), "HTTPS");
                assertEquals(actualMonitor.getPort(), "3333");
                assertEquals(actualMonitor.getIntervalInSeconds(), expectedMonitor.getIntervalInSeconds());
                assertEquals(actualMonitor.getTimeoutInSeconds(), expectedMonitor.getTimeoutInSeconds());
                assertEquals(actualMonitor.getToleratedNumberOfFailures(), expectedMonitor.getToleratedNumberOfFailures());
                assertEquals(actualMonitor.getHttpOptions().getRelativePath(), "testpath");

                return "test";
            }
        };

        HealthCheckOptions options = new HealthCheckOptions();
        options.setProtocol(LoadBalancerHealthCheck.HCProtocol.HTTPS);
        options.setPort(3333);
        options.setPath("testpath");

        lbSupport.modifyHealthCheck(LOAD_BALANCER_ID, options);
    }

    @Test
    public void testCreateLoadBalancer() throws CloudException, InternalException {
        new MockUp<AzureMethod>(){
            @Mock(invocations = 1)
            void $init(Azure provider){  assertEquals(provider, azureMock);}

            @Mock(invocations = 2)
            String post(String resource, Object model){
                if(resource.equalsIgnoreCase(String.format(AzureLoadBalancerSupport.RESOURCE_DEFINITIONS, LOAD_BALANCER_ID)))
                {
                    DefinitionModel postedDefinitionModel = (DefinitionModel)model;
                    assertNotNull(postedDefinitionModel);

                    assertEquals(postedDefinitionModel.getDnsOptions().getTimeToLiveInSeconds(), "300");

                    assertEquals(postedDefinitionModel.getMonitors().size(), 1);
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getTimeoutInSeconds(), "10");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getIntervalInSeconds(), "30");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getToleratedNumberOfFailures(), "3");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getPort(), "12345");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getProtocol(), "HTTP");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getHttpOptions().getVerb(), "GET");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getHttpOptions().getRelativePath(), "MYRELATIVEPATH");
                    assertEquals(postedDefinitionModel.getMonitors().get(0).getHttpOptions().getExpectedStatusCode(), "200");

                    assertEquals(postedDefinitionModel.getPolicy().getLoadBalancingMethod(), "Performance");

                    assertEquals(postedDefinitionModel.getPolicy().getEndPoints().size(), 1);
                    assertEquals(postedDefinitionModel.getPolicy().getEndPoints().get(0).getDomainName(), "endpoint1.cloudapp.net");
                    assertEquals(postedDefinitionModel.getPolicy().getEndPoints().get(0).getStatus(), "Enabled");
                    assertEquals(postedDefinitionModel.getPolicy().getEndPoints().get(0).getType(), "CloudService");

                }
                else if(resource.equalsIgnoreCase(AzureLoadBalancerSupport.RESOURCE_PROFILES))
                {
                    ProfileModel profileModel = (ProfileModel)model;
                    assertNotNull(profileModel);

                    assertEquals(profileModel.getDomainName(), String.format("%s.%s", LOAD_BALANCER_ID, AzureLoadBalancerSupport.TRAFFIC_MANAGER_DNS_NAME));
                    assertEquals(profileModel.getName(), LOAD_BALANCER_ID);
                }
                else
                {
                    fail("Post method called with wrong resource parameter");
                }
                return "test";
            }
        };

        HealthCheckOptions hcOptions = new HealthCheckOptions();
        hcOptions.setProtocol(LoadBalancerHealthCheck.HCProtocol.HTTP);
        hcOptions.setPort(12345);
        hcOptions.setPath("MYRELATIVEPATH");

        LbListener lbListener = LbListener.getInstance(LbAlgorithm.SOURCE, "", LbProtocol.HTTP, 12345, 12345);

        LoadBalancerCreateOptions options = LoadBalancerCreateOptions.getInstance(LOAD_BALANCER_ID, "testdescription");
        options.withHealthCheckOptions(hcOptions).withVirtualMachines("endpoint1.cloudapp.net").havingListeners(lbListener);

        String lbName = lbSupport.createLoadBalancer(options);

        assertEquals(lbName, LOAD_BALANCER_ID);
    }
}
