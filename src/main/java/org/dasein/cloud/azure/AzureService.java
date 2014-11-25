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
