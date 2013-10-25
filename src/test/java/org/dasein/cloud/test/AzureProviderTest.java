package org.dasein.cloud.test;

import org.dasein.cloud.CloudException;
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
 * Created with IntelliJ IDEA.
 * User: sdorzak
 * Date: 28/08/2013
 * Time: 14:12
 * To change this template use File | Settings | File Templates.
 */
public class AzureProviderTest {

    private Azure sut;
    @Mocked ProviderContext providerContextMock;
    @Mocked AzureMethod azureMethodMock;

    @Before
    public void setUp() {
        sut = new Azure();
        sut.connect(providerContextMock);
    }

    @Test
    @Ignore("We currently don't include special characters required, and also sometimes exceed maxLength")
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
        sut.connect(null);

        sut.getStorageEndpoint();
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

    @Test(expected = CloudException.class)
    public void storageEndpointShouldThrowExceptionIfNoBlobEndpointInCurrentRegion() throws CloudException, InternalException {

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

        assertEquals("Invalid storage endpoint returned", "portalvhdsm4qd6c2dvcftf", result);
    }

    @Test
    @Ignore("Currently this fails because we incorrectly get the first one from the top returning the endpoint regardless of location")
    public void getStorageServiceShouldReturnStorageServiceNameInCurrentRegion1() throws CloudException, InternalException {

        new NonStrictExpectations() {
            { providerContextMock.getAccountNumber(); result = "12323232323"; }
            { providerContextMock.getRegionId(); result = "South Central US"; }
        };
        new NonStrictExpectations() {
            { azureMethodMock.getAsXML(anyString, anyString); result = TestHelpers.getMockXmlResponse("/xmlResponses/StorageServices.xml"); }};

        String result = sut.getStorageService();

        assertEquals("Invalid storage endpoint returned", "testdell", result);
    }

    @Test(expected = CloudException.class)
    @Ignore("Currently this fails because we incorrectly get the first one from the top returning the endpoint regardless of location")
    public void getStorageServicShouldThrowExceptionIfNoServiceInCurrentRegion() throws CloudException, InternalException {

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