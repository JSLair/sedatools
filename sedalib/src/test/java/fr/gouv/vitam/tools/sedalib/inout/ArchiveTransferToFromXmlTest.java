package fr.gouv.vitam.tools.sedalib.inout;

import fr.gouv.vitam.tools.sedalib.UseTestFiles;
import fr.gouv.vitam.tools.sedalib.core.GlobalMetadata;
import fr.gouv.vitam.tools.sedalib.inout.importer.DiskToArchiveTransferImporter;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.xml.SEDAXMLStreamWriter;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiveTransferToFromXmlTest implements UseTestFiles {

    private static String readFileToString(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @Test
    void testToFromSedaXml()
            throws IllegalArgumentException, SEDALibException, XMLStreamException, IOException, InterruptedException {

        // do import of test directory
        DiskToArchiveTransferImporter di = new DiskToArchiveTransferImporter(
                "src/test/resources/PacketSamples/SampleWithLinksModelV2", null);
        di.addIgnorePattern("Thumbs.db");
        di.addIgnorePattern("pagefile.sys");

        di.doImport();

        di.getArchiveTransfer().setGlobalMetadata(new GlobalMetadata());
        di.getArchiveTransfer().getGlobalMetadata().comment = "2eme SIP";
        di.getArchiveTransfer().getGlobalMetadata().messageIdentifier = "MessageIdentifier0";
        di.getArchiveTransfer().getGlobalMetadata().archivalAgreement = "ArchivalAgreement0";
        di.getArchiveTransfer().getGlobalMetadata().codeListVersionsXmlData = "<CodeListVersions>\n"
                + "    <ReplyCodeListVersion>ReplyCodeListVersion0</ReplyCodeListVersion>\n"
                + "<MessageDigestAlgorithmCodeListVersion>MessageDigestAlgorithmCodeListVersion0</MessageDigestAlgorithmCodeListVersion>\n"
                + "<MimeTypeCodeListVersion>MimeTypeCodeListVersion0</MimeTypeCodeListVersion>\n"
                + "<EncodingCodeListVersion>EncodingCodeListVersion0</EncodingCodeListVersion>\n"
                + "<FileFormatCodeListVersion>FileFormatCodeListVersion0</FileFormatCodeListVersion>\n"
                + "<CompressionAlgorithmCodeListVersion>CompressionAlgorithmCodeListVersion0</CompressionAlgorithmCodeListVersion>\n"
                + "<DataObjectVersionCodeListVersion>DataObjectVersionCodeListVersion0</DataObjectVersionCodeListVersion>\n"
                + "<StorageRuleCodeListVersion>StorageRuleCodeListVersion0</StorageRuleCodeListVersion>\n"
                + "<AppraisalRuleCodeListVersion>AppraisalRuleCodeListVersion0</AppraisalRuleCodeListVersion>\n"
                + "<AccessRuleCodeListVersion>AccessRuleCodeListVersion0</AccessRuleCodeListVersion>\n"
                + "<DisseminationRuleCodeListVersion>DisseminationRuleCodeListVersion0</DisseminationRuleCodeListVersion>\n"
                + "<ReuseRuleCodeListVersion>ReuseRuleCodeListVersion0</ReuseRuleCodeListVersion>\n"
                + "<ClassificationRuleCodeListVersion>ClassificationRuleCodeListVersion0</ClassificationRuleCodeListVersion>\n"
                + "<AuthorizationReasonCodeListVersion>AuthorizationReasonCodeListVersion0</AuthorizationReasonCodeListVersion>\n"
                + "<RelationshipCodeListVersion>RelationshipCodeListVersion0</RelationshipCodeListVersion>\n"
                + "  </CodeListVersions>";
        di.getArchiveTransfer().getDataObjectPackage()
                .setManagementMetadataXmlData("<ManagementMetadata>\n"
                        + "      <AcquisitionInformation>Acquisition Information</AcquisitionInformation>\n"
                        + "<LegalStatus>Public Archive</LegalStatus>\n"
                        + "<OriginatingAgencyIdentifier>Service_producteur</OriginatingAgencyIdentifier>\n"
                        + "<SubmissionAgencyIdentifier>Service_versant</SubmissionAgencyIdentifier>\n"
                        + "    </ManagementMetadata>");
        di.getArchiveTransfer().getGlobalMetadata().archivalAgencyIdentifier = "Identifier4";
        di.getArchiveTransfer().getGlobalMetadata().transferringAgencyIdentifier = "Identifier5";

        // flat
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SEDAXMLStreamWriter xmlWriter = new SEDAXMLStreamWriter(baos, 2);
        di.getArchiveTransfer().toSedaXml(xmlWriter, false, null);
        xmlWriter.close();
        String generatedFlatManifest = baos.toString(StandardCharsets.UTF_8).replaceAll("<LastModified>.*</LastModified>\n", "");

        // hierarchical
        baos.reset();
        xmlWriter = new SEDAXMLStreamWriter(baos, 2);
        di.getArchiveTransfer().toSedaXml(xmlWriter, true, null);
        xmlWriter.close();
        String generatedHierarchicalManifest = baos.toString(StandardCharsets.UTF_8).replaceAll("<LastModified>.*</LastModified>\n", "");

        String fileManifest = readFileToString("src/test/resources/PacketSamples/SampleWithLinkFlatManifest.xml");
        generatedFlatManifest = generatedFlatManifest.substring(generatedFlatManifest.indexOf("MessageIdentifier"));
        fileManifest = fileManifest.substring(fileManifest.indexOf("MessageIdentifier"));
//WARNING: if Git is not set to respect LF this test will fail
        assertThat(generatedFlatManifest).isEqualToNormalizingNewlines(fileManifest);

        fileManifest = readFileToString("src/test/resources/PacketSamples/SampleWithLinkHierarchicalManifest.xml");
        generatedHierarchicalManifest = generatedHierarchicalManifest
                .substring(generatedHierarchicalManifest.indexOf("MessageIdentifier"));
        fileManifest = fileManifest.substring(fileManifest.indexOf("MessageIdentifier"));
//WARNING: if Git is not set to respect LF this test will fail
		assertThat(generatedHierarchicalManifest).isEqualToNormalizingNewlines(fileManifest);
    }
}
