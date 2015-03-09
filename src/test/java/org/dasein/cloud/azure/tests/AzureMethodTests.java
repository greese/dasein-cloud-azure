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

package org.dasein.cloud.azure.tests;

import mockit.*;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureMethod;
import org.junit.Test;
import sun.misc.IOUtils;
import sun.nio.cs.StandardCharsets;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by Vlad_Munthiu on 6/13/2014.
 */
public class AzureMethodTests
{

    @Mocked ProviderContext mockedProviderContext;

    @Test
    public void genericGetReturnCorrectObject(@Injectable final Azure mockedCloudProvider) throws CloudException, InternalException, UnsupportedEncodingException, URISyntaxException, JAXBException
    {
        final DummyTest expectedDummy = new DummyTest("firstValue", "secondValue");
        final ByteArrayOutputStream outputStream = getDummyAsByteArrayOutputStream(expectedDummy);
        Boolean getAsStreamCalled = false;
        new NonStrictExpectations(){
            {mockedProviderContext.getEndpoint(); result = "myendpoint";}
            { mockedCloudProvider.getContext(); result = mockedProviderContext;}
        };

       new MockUp<AzureMethod>(){
            /*private Azure provider;
            @Mock
            public void $init(Azure azure)
            {
                this.provider = mockedCloudProvider;
            }*/
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull String resource)
            {
                return new ByteArrayInputStream(outputStream.toByteArray());}
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull URI uri)
            {
                return new ByteArrayInputStream(outputStream.toByteArray());}
        };

        final String account = "myaccount";
        final URI uri = new URI("myuri");

        final AzureMethod azureMethod = new AzureMethod(mockedCloudProvider);
        /*new Verifications(){
            { azureMethod.getAsStream("vlad", uri); times = 1;}
        };*/

        DummyTest actuallObject = azureMethod.<DummyTest>get(DummyTest.class, uri);



        assertNotNull("object should not be null when valid XML string is deserialized", actuallObject);
        assertEquals("xml not deserialzied correctly", expectedDummy.getFirstProperty(), actuallObject.getFirstProperty());
        assertEquals("xml not deserialzied correctly", expectedDummy.getSecondProperty(), actuallObject.getSecondProperty());

    }

    @Test(expected = CloudException.class)
    public void genericGetThrowsCloudExceptionWhenInputStreamNull(@Injectable final Azure mockedCloudProvider) throws CloudException, URISyntaxException, InternalException {
        final String dummyXml = "<myroot><node>value</node></myroot>";

        new NonStrictExpectations(){
            {mockedProviderContext.getEndpoint(); result = "myendpoint";}
            { mockedCloudProvider.getContext(); result = mockedProviderContext;}
        };
        new MockUp<AzureMethod>(){
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull String resource) throws UnsupportedEncodingException {return null;}
        };
        new MockUp<AzureMethod>(){
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull URI uri) throws UnsupportedEncodingException {return null;}
        };

        final AzureMethod azureMethod = new AzureMethod(mockedCloudProvider);
        DummyTest actuallObject = azureMethod.<DummyTest>get(DummyTest.class, new URI("myuri"));
    }

    @Test(expected = InternalException.class)
    public void genericGetThrowsInternalExceptionWhenCannotDeserializeTheXml(@Injectable final Azure mockedCloudProvider) throws CloudException, URISyntaxException, InternalException {
        final String dummyXml = "<myroot><node>value</node></myroot>";

        new NonStrictExpectations(){
            {mockedProviderContext.getEndpoint(); result = "myendpoint";}
            { mockedCloudProvider.getContext(); result = mockedProviderContext;}
        };
        new MockUp<AzureMethod>(){
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull String resource) throws UnsupportedEncodingException {return new ByteArrayInputStream(dummyXml.getBytes("UTF-8"));}
        };
        new MockUp<AzureMethod>(){
            @Mock
            InputStream getAsStream(@Nonnull String account, @Nonnull URI uri) throws UnsupportedEncodingException {return new ByteArrayInputStream(dummyXml.getBytes("UTF-8"));}
        };

        final AzureMethod azureMethod = new AzureMethod(mockedCloudProvider);
        DummyTest actuallObject = azureMethod.<DummyTest>get(DummyTest.class, new URI("myuri"));
    }

    private ByteArrayOutputStream getDummyAsByteArrayOutputStream(DummyTest expectedDummy) throws JAXBException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JAXBContext context = JAXBContext.newInstance(DummyTest.class);
        Marshaller m = context.createMarshaller();
        m.marshal(expectedDummy, outputStream);
        return outputStream;
    }
}

@XmlRootElement(name="Dummy")
@XmlAccessorType(XmlAccessType.NONE)
class DummyTest
{
    @XmlElement(name="firstProperty")
    private String firstProperty;
    @XmlElement(name="secondProperty")
    private String secondProperty;

    public DummyTest(){}

    public DummyTest(String firstValue, String secondValue){
        this.firstProperty = firstValue;
        this.secondProperty = secondValue;
    }

    public String getFirstProperty() {
        return firstProperty;
    }

    public void setFirstProperty(String firstProperty) {
        this.firstProperty = firstProperty;
    }

    public String getSecondProperty() {
        return secondProperty;
    }

    public void setSecondProperty(String secondProperty) {
        this.secondProperty = secondProperty;
    }
}
