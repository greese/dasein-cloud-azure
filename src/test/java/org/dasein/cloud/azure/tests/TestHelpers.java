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

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

/**
 * Created by Vlad_Munthiu on 6/6/2014.
 */
public class TestHelpers {
    public static Document getMockXmlResponse(String responsePath) {
        InputStream input = AzureProviderTests.class.getResourceAsStream(responsePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = null;
        Document document = null;
        try {
            parser = factory.newDocumentBuilder();
            document = parser.parse(input);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return document;
    }

    public static String getStringXmlRequest(String requestPath)    {

        BufferedReader bufferedReader = null;
        InputStream input = AzureProviderTests.class.getResourceAsStream(requestPath);

        StringBuffer stringBuffer = new StringBuffer();
        String line = null;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            while((line =bufferedReader.readLine())!=null){

                stringBuffer.append(line).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return stringBuffer.toString();
    }

    public static String getStringXmlRequestAsSingleLine(String requestPath){
        return getStringXmlRequest(requestPath).replaceAll("\n|\r", "");
    }
}
