/**
 * RegisteredDate * Copyright French Prime minister Office/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@programmevitam.fr
 * <p>
 * This software is developed as a validation helper tool, for constructing Submission Information Packages (archives
 * sets) in the Vitam program whose purpose is to implement a digital archiving back-office system managing high
 * volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA archiveDeliveryRequestReply the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.tools.sedalib.metadata.namedtype;

import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.xml.SEDAXMLEventReader;
import fr.gouv.vitam.tools.sedalib.xml.SEDAXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedHashMap;

/**
 * The Class AnyXMLType.
 * <p>
 * For abstract not determined SEDA metadata, take all the xml block
 */
public class AnyXMLType extends NamedTypeMetadata {

    /** The raw xml. */
    private String rawXml;

    /**
     * Instantiates a new XML block type.
     */
    public AnyXMLType() {
        this(null, null);
    }

    /**
     * Instantiates a new XML block type.
     *
     * @param elementName the XML element name
     */
    public AnyXMLType(String elementName) {
        this(elementName, null);
    }

    /**
     * Instantiates a new XML block type.
     *
     * @param elementName the XML element name
     * @param rawXml       the raw Xml String
     */
    public AnyXMLType(String elementName, String rawXml) {
        super(elementName);
        this.rawXml = rawXml;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.gouv.vitam.tools.sedalib.metadata.SEDAMetadata#toSedaXml(fr.gouv.vitam.
     * tools.sedalib.xml.SEDAXMLStreamWriter)
     */
    @Override
    public void toSedaXml(SEDAXMLStreamWriter xmlWriter) throws SEDALibException {
        try {
            xmlWriter.writeRawXMLBlockIfNotEmpty(rawXml);
        } catch (XMLStreamException e) {
            throw new SEDALibException("Erreur d'écriture XML dans un élément de type AnyXMLType ["+getXmlElementName()+"]", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.gouv.vitam.tools.sedalib.metadata.SEDAMetadata#toCsvList()
     */
    public LinkedHashMap<String, String> toCsvList() throws SEDALibException {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("",rawXml);
        return result;
    }

    /**
     * Import the metadata content in XML expected form from the SEDA Manifest.
     *
     * @param xmlReader       the SEDAXMLEventReader reading the SEDA manifest
     * @return true, if it finds something convenient, false if not
     * @throws SEDALibException if the XML can't be read or the SEDA scheme is not respected, for example
     */
    public boolean fillFromSedaXml(SEDAXMLEventReader xmlReader) throws SEDALibException {
        try {
            XMLEvent event = xmlReader.peekUsefullEvent();
            elementName = event.asStartElement().getName().getLocalPart();
            rawXml = xmlReader.nextBlockAsStringIfNamed(elementName);
        } catch (XMLStreamException | IllegalArgumentException e) {
            throw new SEDALibException(
                    "Erreur de lecture XML dans un élément de type AnyXMLType", e);
        }
        return true;
    }

    // Getters and setters

    /**
     * Gets raw xml.
     *
     * @return the raw xml
     */
    public String getRawXml() {
        return rawXml;
    }

    /**
     * Sets raw xml.
     *
     * @param rawXml the raw xml
     */
    public void setRawXml(String rawXml) {
        this.rawXml = rawXml;
    }
}
