package org.dasein.cloud.azure;

import org.dasein.cloud.CloudException;

public class AzureConfigException extends CloudException {
    public AzureConfigException(String message) {
        super(message);
    }
    
    public AzureConfigException(Throwable cause) {
        super(cause);
    }
}
