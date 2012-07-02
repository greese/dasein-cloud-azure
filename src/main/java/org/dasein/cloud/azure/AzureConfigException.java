package org.dasein.cloud.azure;

import org.dasein.cloud.InternalException;

/**
 * Created by IntelliJ IDEA.
 * User: greese
 * Date: 5/19/12
 * Time: 7:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class AzureConfigException extends InternalException {
    public AzureConfigException(String message) {
        super(message);
    }
    
    public AzureConfigException(Throwable cause) {
        super(cause);
    }
}
