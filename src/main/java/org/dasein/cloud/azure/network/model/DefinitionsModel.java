package org.dasein.cloud.azure.network.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by Vlad_Munthiu on 6/17/2014.
 */

@XmlRootElement(name="Definitions", namespace ="http://schemas.microsoft.com/windowsazure")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefinitionsModel
{
    @XmlElement(name="Definition", namespace ="http://schemas.microsoft.com/windowsazure")
    private List<DefinitionModel> definitions;

    public List<DefinitionModel> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<DefinitionModel> definitions) {
        this.definitions = definitions;
    }
}
