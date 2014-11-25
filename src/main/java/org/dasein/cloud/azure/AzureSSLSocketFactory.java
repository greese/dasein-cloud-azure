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

package org.dasein.cloud.azure;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.dasein.cloud.InternalException;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * Custom socket factory for handling X509 authentication with Azure using an in-memory key store.
 * @author George Reese (george.reese@imaginary.com)
 * @author Tim Freeman (tim.freeman@enstratus.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureSSLSocketFactory extends SSLSocketFactory {

    public AzureSSLSocketFactory(AzureX509 creds) throws InternalException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        super("TLS", creds.getKeystore(), AzureX509.PASSWORD, null, null, null, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }
}
