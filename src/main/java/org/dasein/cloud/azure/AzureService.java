package org.dasein.cloud.azure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Services supported in Microsoft Azure.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public enum AzureService {
    COMPUTE, DATABASE, STORAGE, PERSISTENT_VM_ROLE;

    static public @Nullable AzureService fromString(@Nonnull String str) {
        if( str.equalsIgnoreCase("compute") ) {
            return COMPUTE;
        }
        else if( str.equalsIgnoreCase("storage") ) {
            return STORAGE;
        }
        else if(str.equalsIgnoreCase("persistentvmrole") ) {
            return PERSISTENT_VM_ROLE;
        }
        else if( str.equalsIgnoreCase("database") ) {
            return DATABASE;
        }
        return null;
    }

    public @Nonnull String toString() {
        switch( this ) {
            case COMPUTE: return "Compute";
            case STORAGE: return "Storage";
            case PERSISTENT_VM_ROLE: return "PersistentVMRole";
            case DATABASE: return "Database";
        }
        return "Unknown";
    }
}
