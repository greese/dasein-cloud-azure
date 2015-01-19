package org.dasein.cloud.azure;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.util.requester.fluent.DaseinRequest;

/**
 * Created by Vlad_Munthiu on 11/19/2014.
 */
public class AzureRequester extends DaseinRequest {

    public AzureRequester(Azure provider, HttpUriRequest httpUriRequest) throws CloudException {
        this(provider, provider.getAzureClientBuilder(), httpUriRequest);
    }

    private AzureRequester(CloudProvider provider, HttpClientBuilder httpClientBuilder, HttpUriRequest httpUriRequestBuilder) {
        super(provider, httpClientBuilder, httpUriRequestBuilder);
    }
}
