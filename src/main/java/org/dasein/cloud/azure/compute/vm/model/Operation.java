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

package org.dasein.cloud.azure.compute.vm.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Vlad_Munthiu on 8/14/2014.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class Operation {

    @XmlElement(name="OperationType", namespace ="http://schemas.microsoft.com/windowsazure")
    protected String operationType;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    @XmlRootElement(name="StartRoleOperation", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StartRoleOperation extends Operation
    {
        public StartRoleOperation()
        {
            this.operationType = "StartRoleOperation";
        }
    }

    @XmlRootElement(name="RestartRoleOperation", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RestartRoleOperation extends Operation
    {
        public RestartRoleOperation()
        {
            this.operationType = "RestartRoleOperation";
        }
    }

    @XmlRootElement(name="ShutdownRoleOperation", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ShutdownRoleOperation extends Operation
    {
        @XmlElement(name="PostShutdownAction", namespace ="http://schemas.microsoft.com/windowsazure")
        private String postShutdownAction;

        public ShutdownRoleOperation()
        {
            this.operationType = "ShutdownRoleOperation";
        }

        public String getPostShutdownAction() {
            return postShutdownAction;
        }

        public void setPostShutdownAction(String postShutdownAction) {
            this.postShutdownAction = postShutdownAction;
        }
    }

    @XmlRootElement(name="CaptureRoleAsVMImageOperation", namespace ="http://schemas.microsoft.com/windowsazure")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CaptureRoleAsVMImageOperation extends Operation
    {
        @XmlElement(name="OSState", namespace ="http://schemas.microsoft.com/windowsazure")
        private String osState;
        @XmlElement(name="VMImageName", namespace ="http://schemas.microsoft.com/windowsazure")
        private String vmImageName;
        @XmlElement(name="VMImageLabel", namespace ="http://schemas.microsoft.com/windowsazure")
        private String vmImageLabel;
        @XmlElement(name="Description", namespace ="http://schemas.microsoft.com/windowsazure")
        private String description;
        @XmlElement(name="Language", namespace ="http://schemas.microsoft.com/windowsazure")
        private String language;
        @XmlElement(name="ImageFamily", namespace ="http://schemas.microsoft.com/windowsazure")
        private String imageFamily;
        @XmlElement(name="RecommendedVMSize", namespace ="http://schemas.microsoft.com/windowsazure")
        private String recommendedVMSize;

        public CaptureRoleAsVMImageOperation()
        {
            this.operationType = "CaptureRoleAsVMImageOperation";
        }

        public String getOsState() {
            return osState;
        }

        public void setOsState(String osState) {
            this.osState = osState;
        }

        public String getVmImageName() {
            return vmImageName;
        }

        public void setVmImageName(String vmImageName) {
            this.vmImageName = vmImageName;
        }

        public String getVmImageLabel() {
            return vmImageLabel;
        }

        public void setVmImageLabel(String vmImageLabel) {
            this.vmImageLabel = vmImageLabel;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getImageFamily() {
            return imageFamily;
        }

        public void setImageFamily(String imageFamily) {
            this.imageFamily = imageFamily;
        }

        public String getRecommendedVMSize() {
            return recommendedVMSize;
        }

        public void setRecommendedVMSize(String recommendedVMSize) {
            this.recommendedVMSize = recommendedVMSize;
        }
    }
}
