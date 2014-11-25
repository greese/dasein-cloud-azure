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

package org.dasein.cloud.azure.tests;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;

import mockit.NonStrictExpectations;
import mockit.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.junit.* ;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.Calendar;

import static org.junit.Assert.* ;
import static org.junit.Assert.assertEquals;

/**
 * Created by Vlad_Munthiu on 6/6/2014.
 */
public class AzureProviderTests {

    private Azure sut;
    @Mocked ProviderContext providerContextMock;
    @Mocked AzureMethod azureMethodMock;

    @Before
    public void setUp() {

        sut = new Azure();
        new NonStrictExpectations() {
            { new MockUp<CloudProvider>() {
                @Mock ProviderContext getContext(){return providerContextMock;}
            };};
        };
    }

    @Test
    public void generatedTokenShouldBeBetweenMinAndMax() {
        int maxLength = 15;
        int minLength = 8;

        // I'd like to test that we're never exceeding maxLength
        // repeating it 1k times makes me feel better but it' ugly as hell and not really dependable
        // (we could probably mock out the random number generator but not sure if it's worth added complexity)
        // any ideas are welcome
        for(int i=0;i<1000;i++){
            String token = sut.generateToken(minLength, maxLength);

            assertTrue("Token too long", token.length() <= maxLength);
            assertTrue("Token too short", token.length() >= minLength);
            assertTrue("Token must have 2 uppercase letters", token.matches(".*[A-Z]{1}.*[A-Z]{1}.*"));
            assertTrue("Token must have 2 digits", token.matches(".*[0-9]{1}.*[0-9]{1}.*") );
            assertTrue("Token must have 2 special chars", token.matches(".*[\\!\\@\\#\\$\\%\\^\\&\\*\\_\\=\\+\\-\\/]{1}.*[\\!\\@\\#\\$\\%\\^\\&\\*\\_\\=\\+\\-\\/]{1}.*") );
        }
    }

    @Test
    public void cloudNameShouldBeAzure(){
        assertEquals("Invalid cloud name", "Azure", sut.getCloudName());
    }

    @Test
    public void providerNameShouldBeMicrosoft(){
        assertEquals("Invalid provider name", "Microsoft", sut.getProviderName());
    }

    @Test
    public void parsingTimestampShouldReturnCorrectValue() throws CloudException, InternalException {
        long result = sut.parseTimestamp("2012-07-10T12:18:04Z");

        Calendar cal = Calendar.getInstance();
        cal.set(2012, Calendar.JULY, 10, 12, 18, 4);
        cal.set(Calendar.MILLISECOND, 0);

        long expected = cal.getTime().getTime();

        assertEquals("Invalid timestamp value returned", expected, result);
    }

    @Test
    public void parsingTimestampWithMillisecondsShouldReturnCorrectValue() throws CloudException, InternalException {
        long result = sut.parseTimestamp("2012-07-10T12:18:04.333Z");

        Calendar cal = Calendar.getInstance();
        cal.set(2012, Calendar.JULY, 10, 12, 18, 4);
        cal.set(Calendar.MILLISECOND, 333);

        long expected = cal.getTime().getTime();

        assertEquals("Invalid timestamp value returned", expected, result);
    }

    @Test
    public void parsingEmptyTimestampShouldReturnZero() throws CloudException, InternalException {
        long result = sut.parseTimestamp("");

        long expected = 0l;

        assertEquals("Invalid timestamp value returned", expected, result);
    }

    @Test(expected = AzureConfigException.class)
    public void storageEndpointShouldThrowExceptionIfThereIsNoContext() throws CloudException, InternalException {
        final Azure sutNew = new Azure();
        new NonStrictExpectations() {
            { new MockUp<CloudProvider>() {
                @Mock ProviderContext getContext(){return null;}
            };};
        };
        sutNew.getStorageEndpoint();
    }

    @Test(expected = CloudException.class)
    public void storageEndpointShouldThrowExceptionIfThereIsNoXmlResponse() throws CloudException, InternalException {
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = null; }};

        sut.getStorageEndpoint();
    }

    @Test
    public void storageEndpointShouldReturnBlobEndpointInCurrentRegion() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "East US"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageEndpoint();

        assertEquals("Invalid storage endpoint returned", "http://portalvhdsm4qd6c2dvcftf.blob.core.windows.net/", result);
    }

    @Test
    public void storageEndpointShouldReturnBlobEndpointInCurrentRegion1() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "South Central US"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageEndpoint();

        assertEquals("Invalid storage endpoint returned", "http://testdell.blob.core.windows.net/", result);
    }

    @Test
    public void storageEndpointShouldReturnNullIfNoBlobEndpointInCurrentRegion() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "East EU"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageEndpoint();

        assertEquals("Invalid storage endpoint returned", null, result);
    }

    @Test(expected = CloudException.class)
    public void getStorageServiceShouldThrowExceptionIfThereIsNoXmlResponse() throws CloudException, InternalException {
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = null; }};

        sut.getStorageService();
    }

    @Test
    public void getStorageServiceShouldReturnStorageServiceNameInCurrentRegion() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "East US"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageService();

        assertEquals("Invalid storage service returned", "portalvhdsm4qd6c2dvcftf", result);
    }

    @Test
    public void getStorageServiceShouldReturnStorageServiceNameInCurrentRegion1() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "South Central US"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageService();

        assertEquals("Invalid storage service returned", "testdell", result);
    }

    @Test
    public void getStorageServicShouldReturnNullIfNoServiceInCurrentRegion() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "East EU"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageService();

        assertEquals("Invalid storage service returned", null, result);
    }



}