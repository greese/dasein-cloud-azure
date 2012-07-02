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
