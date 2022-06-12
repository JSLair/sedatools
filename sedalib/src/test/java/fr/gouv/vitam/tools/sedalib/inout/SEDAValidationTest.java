package fr.gouv.vitam.tools.sedalib.inout;

import fr.gouv.vitam.tools.sedalib.TestUtilities;
import fr.gouv.vitam.tools.sedalib.UseTestFiles;
import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.BinaryDataObject;
import fr.gouv.vitam.tools.sedalib.core.GlobalMetadata;
import fr.gouv.vitam.tools.sedalib.core.SEDA2Version;
import fr.gouv.vitam.tools.sedalib.inout.importer.DiskToArchiveTransferImporter;
import fr.gouv.vitam.tools.sedalib.inout.importer.SIPToArchiveTransferImporter;
import fr.gouv.vitam.tools.sedalib.metadata.namedtype.StringType;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.xml.SEDAXMLStreamWriter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SEDAValidationTest implements UseTestFiles {

	@Test
	void testSedaXmlValidationOK()
			throws IllegalArgumentException, SEDALibException, InterruptedException {

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

        // validation
        assertAll(() -> di.getArchiveTransfer().sedaSchemaValidate(null));
    }

    @Test
    void testSedaXmlArchiveTransferGenerationAndValidationForSedaVersion2()
            throws IllegalArgumentException, SEDALibException, InterruptedException {

        // do import of test directory
        SEDA2Version.setSeda2Version(2);
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
                + "<HoldRuleCodeListVersion>HoldRuleCodeListVersion0</HoldRuleCodeListVersion>\n"
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

        ArchiveUnit au = di.getArchiveTransfer().getDataObjectPackage().getArchiveUnitById("ID11");
        BinaryDataObject bdo = (BinaryDataObject) au.getTheDataObjectGroup().getBinaryDataObjectList().get(0);
        bdo.dataObjectProfile = new StringType("DataObjectProfile", "Test");

        // validation
        assertAll(() -> di.getArchiveTransfer().sedaSchemaValidate(null));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             SEDAXMLStreamWriter xmlWriter = new SEDAXMLStreamWriter(baos, 2)) {
            di.getArchiveTransfer().toSedaXml(xmlWriter, true, null);
            String atout = baos.toString(StandardCharsets.UTF_8);
            assertThat(atout).contains("fr:gouv:culture:archivesdefrance:seda:v2.2");
            assertThat(atout).contains("DataObjectProfile");
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(e);
        }
        SEDA2Version.setSeda2Version(1);
    }

	@Test
	void testSedaXmlValidationKO()
			throws IllegalArgumentException, SEDALibException, InterruptedException {

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

		ArchiveUnit au = di.getArchiveTransfer().getDataObjectPackage().getArchiveUnitById("ID38");
		au.setContentXmlData("");

		// validation
		assertThatThrownBy(() -> di.getArchiveTransfer().sedaSchemaValidate(null))
				.hasMessageContaining("The content of element 'ArchiveUnit' is not complete");
	}

	@Test
	void testSedaRNGProfileValidationOK()
			throws IllegalArgumentException, SEDALibException, InterruptedException {
		// given
		TestUtilities.eraseAll("target/tmpJunit/OK_468.zip-tmpdir");
		SIPToArchiveTransferImporter si = new SIPToArchiveTransferImporter(
				"src/test/resources/PacketSamples/OK_468.zip", "target/tmpJunit/OK_468.zip-tmpdir", null);
		si.doImport();

		// when validate then no exception
		assertAll(() -> si.getArchiveTransfer()
				.sedaProfileValidate("src/test/resources/PacketSamples/profile.rng",null));
	}

	@Test
	void testSedaRNGProfileValidationKO()
			throws IllegalArgumentException, SEDALibException, InterruptedException {
		// given
		TestUtilities.eraseAll("target/tmpJunit/KO_468.zip-tmpdir");
		SIPToArchiveTransferImporter si = new SIPToArchiveTransferImporter(
				"src/test/resources/PacketSamples/KO_468.zip", "target/tmpJunit/KO_468.zip-tmpdir", null);
		si.doImport();

		// when validate then no exception
		assertThatThrownBy(() -> si.getArchiveTransfer()
				.sedaProfileValidate("src/test/resources/PacketSamples/profile.rng",null))
				.hasMessageContaining("\"Title\" invalid; must be equal to \"Versement de la matrice cadastrale numérique\"");
	}

    // TODO testWithXSD
}