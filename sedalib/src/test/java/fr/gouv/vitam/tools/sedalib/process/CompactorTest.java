package fr.gouv.vitam.tools.sedalib.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.gouv.vitam.tools.sedalib.TestUtilities;
import fr.gouv.vitam.tools.sedalib.UseTestFiles;
import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.DataObjectGroup;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
import fr.gouv.vitam.tools.sedalib.core.json.DataObjectPackageDeserializer;
import fr.gouv.vitam.tools.sedalib.core.json.DataObjectPackageSerializer;
import fr.gouv.vitam.tools.sedalib.inout.importer.DiskToArchiveTransferImporter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactorTest implements UseTestFiles {

    private void eraseAll(String dirOrFile) {
        try {
            Files.delete(Paths.get(dirOrFile));
        } catch (Exception ignored) {
        }
        try {
            FileUtils.deleteDirectory(new File(dirOrFile));
        } catch (Exception ignored) {
        }
    }

    @Test
    void TestCompactor() throws Exception {

        // Given this test directory imported
        DiskToArchiveTransferImporter di = new DiskToArchiveTransferImporter(
                "src/test/resources/PacketSamples/SampleWithoutLinksModelV1", null);
        di.addIgnorePattern("Thumbs.db");
        di.addIgnorePattern("pagefile.sys");
        di.doImport();

        // When Compact the root ArchiveUnit

    }

    @Test
    void TestDiskImportWithLink() throws Exception {

        // do import of test directory
        DiskToArchiveTransferImporter di;
        di = new DiskToArchiveTransferImporter("src/test/resources/PacketSamples/SampleWithLinksModelV2", null);

        di.addIgnorePattern("Thumbs.db");
        di.addIgnorePattern("pagefile.sys");
        di.doImport();

        // assert macro results
        assertEquals(22,di.getArchiveTransfer().getDataObjectPackage().getAuInDataObjectPackageIdMap().size());
        assertEquals(11,di.getArchiveTransfer().getDataObjectPackage().getDogInDataObjectPackageIdMap().size());

        // create jackson object mapper
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataObjectPackage.class, new DataObjectPackageSerializer());
        module.addDeserializer(DataObjectPackage.class, new DataObjectPackageDeserializer());
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // assert one dataObjectGroup using serialization
        String testog = "{\n" +
                "  \"binaryDataObjectList\" : [ {\n" +
                "    \"dataObjectSystemId\" : null,\n" +
                "    \"dataObjectGroupSystemId\" : null,\n" +
                "    \"relationshipsXmlData\" : [ ],\n" +
                "    \"dataObjectGroupReferenceId\" : null,\n" +
                "    \"dataObjectGroupId\" : null,\n" +
                "    \"dataObjectVersion\" : {\n" +
                "      \"type\" : \"StringType\",\n" +
                "      \"elementName\" : \"DataObjectVersion\",\n" +
                "      \"value\" : \"BinaryMaster_1\"\n" +
                "    },\n" +
                "    \"uri\" : null,\n" +
                "    \"messageDigest\" : {\n" +
                "      \"type\" : \"DigestType\",\n" +
                "      \"elementName\" : \"MessageDigest\",\n" +
                "      \"value\" : \"e321b289f1800e5fa3be1b8d01687c8999ef3ecfec759bd0e19ccd92731036755c8f79cbd4af8f46fc5f4e14ad805f601fe2e9b58ad0b9f5a13695c0123e45b3\",\n" +
                "      \"algorithm\" : \"SHA-512\"\n" +
                "    },\n" +
                "    \"size\" : {\n" +
                "      \"type\" : \"IntegerType\",\n" +
                "      \"elementName\" : \"Size\",\n" +
                "      \"value\" : 21232\n" +
                "    },\n" +
                "    \"compressed\" : null,\n" +
                "    \"formatIdentification\" : {\n" +
                "      \"type\" : \"FormatIdentification\",\n" +
                "      \"elementName\" : \"FormatIdentification\",\n" +
                "      \"metadataList\" : [ {\n" +
                "        \"type\" : \"StringType\",\n" +
                "        \"elementName\" : \"FormatLitteral\",\n" +
                "        \"value\" : \"Exchangeable Image File Format (Compressed)\"\n" +
                "      }, {\n" +
                "        \"type\" : \"StringType\",\n" +
                "        \"elementName\" : \"MimeType\",\n" +
                "        \"value\" : \"image/jpeg\"\n" +
                "      }, {\n" +
                "        \"type\" : \"StringType\",\n" +
                "        \"elementName\" : \"FormatId\",\n" +
                "        \"value\" : \"fmt/645\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"fileInfo\" : {\n" +
                "      \"type\" : \"FileInfo\",\n" +
                "      \"elementName\" : \"FileInfo\",\n" +
                "      \"metadataList\" : [ {\n" +
                "        \"type\" : \"StringType\",\n" +
                "        \"elementName\" : \"Filename\",\n" +
                "        \"value\" : \"image001.jpg\"\n" +
                "      }, {\n" +
                "        \"type\" : \"DateTimeType\",\n" +
                "        \"elementName\" : \"LastModified\",\n" +
                "        \"dateTimeString\" : \"2019-01-16T20:03:35.698553Z\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"metadata\" : null,\n" +
                "    \"inDataObjectPackageId\" : \"ID13\",\n" +
                "    \"onDiskPath\" : \"F:\\\\DocumentsPerso\\\\JS\\\\IdeaProjects\\\\sedatools\\\\sedalib\\\\src\\\\test\\\\resources\\\\PacketSamples\\\\SampleWithLinksModelV2\\\\Root\\\\Node 1\\\\Node 1.2\\\\__BinaryMaster_1__image001.jpg\"\n" +
                "  } ],\n" +
                "  \"physicalDataObjectList\" : [ ],\n" +
                "  \"logBook\" : null,\n" +
                "  \"inDataObjectPackageId\" : \"ID12\",\n" +
                "  \"onDiskPath\" : null\n" +
                "}";

        DataObjectGroup og = di.getArchiveTransfer().getDataObjectPackage().getDogInDataObjectPackageIdMap().get("ID12");
        //System.out.println("Value to verify="+mapper.writeValueAsString(og));
        String sog = mapper.writeValueAsString(og);
        Pattern pog = Pattern.compile("\"onDiskPath\" : .*Node 1.2");
        Matcher msog = pog.matcher(sog);
        boolean sogpath = msog.find();
        sog = sog.replaceAll("\"onDiskPath\" : .*", "");
        sog = TestUtilities.LineEndNormalize(sog.replaceAll("\"dateTimeString\" : .*\"", ""));

        Matcher mtestog = pog.matcher(testog);
        boolean testogpath = mtestog.find();
        testog = testog.replaceAll("\"onDiskPath\" : .*", "");
        testog = TestUtilities.LineEndNormalize(testog.replaceAll("\"dateTimeString\" : .*\"", ""));

        assertThat(sog).isEqualTo(testog);
        assertTrue(sogpath & testogpath);

        // assert one archiveUnit using serialization
        String testau = "{\n" + "  \"archiveUnitProfileXmlData\" : null,\n" + "  \"managementXmlData\" : null,\n"
                + "  \"contentXmlData\" : \"<Content>\\n  <DescriptionLevel>RecordGrp</DescriptionLevel>\\n  <Title>Node 2.3 - Many</Title>\\n</Content>\",\n"
                + "  \"childrenAuList\" : {\n"
                + "    \"inDataObjectPackageIdList\" : [\"ID43\",\"ID46\",\"ID49\",\"ID16\",\"ID52\",\"ID55\"]\n"
                + "  },\n" + "  \"dataObjectRefList\" : {\n" + "    \"inDataObjectPackageIdList\" : [ ]\n" + "  },\n"
                + "  \"inDataObjectPackageId\" : \"ID40\",\n"
                + "  \"onDiskPath\" : \"F:\\\\DocumentsPerso\\\\JS\\\\git\\\\sedalib\\\\src\\\\test\\\\ressources\\\\PacketSamples\\\\SampleWithWindowsLinksAndShortcutsModelV2\\\\Root\\\\Node 2\\\\Node 2.3 - Many\"\n"
                + "}";

        Pattern pau = Pattern.compile("\"onDiskPath\" : .*Node 2.3 - Many\"");
        Matcher mtestau = pau.matcher(testau);
        boolean testaupath = mtestau.find();
        testau = TestUtilities.LineEndNormalize(testau.replaceAll("\"onDiskPath\" : .*\"", ""));

        ArchiveUnit au = di.getArchiveTransfer().getDataObjectPackage().getAuInDataObjectPackageIdMap().get("ID40");
        String sau = mapper.writeValueAsString(au);
//		System.out.println(sau);
        Matcher msau = pau.matcher(sau);
        boolean saupath = msau.find();
        sau = TestUtilities.LineEndNormalize(sau.replaceAll("\"onDiskPath\" : .*\"", ""));

        assertTrue(saupath & testaupath);
        assertEquals(sau, testau);
    }

    static Function<String,String> replaced = s->"Replaced";

    @Test
    void TestDiskImportWithLinkIgnoringLinksAndExtractingTitle() throws Exception {

        // do import of test directory
        DiskToArchiveTransferImporter di;
        di = new DiskToArchiveTransferImporter("src/test/resources/PacketSamples/SampleWithLinksModelV2",
                true,replaced,null);

        di.addIgnorePattern("Thumbs.db");
        di.addIgnorePattern("pagefile.sys");
        di.doImport();

        // assert macro results
        assertEquals(22, di.getArchiveTransfer().getDataObjectPackage().getAuInDataObjectPackageIdMap().size());
        assertEquals(11, di.getArchiveTransfer().getDataObjectPackage().getDogInDataObjectPackageIdMap().size());

        // create jackson object mapper
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(DataObjectPackage.class, new DataObjectPackageSerializer());
        module.addDeserializer(DataObjectPackage.class, new DataObjectPackageDeserializer());
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // assert one archiveUnit using serialization
        String testau = "{\n" +
                "\"archiveUnitProfileXmlData\":null,\n" +
                "\"managementXmlData\":null,\n" +
                "\"contentXmlData\":\"<Content>  <DescriptionLevel>RecordGrp</DescriptionLevel>  <Title>Replaced</Title></Content>\",\n" +
                "\"childrenAuList\":{\n" +
                "\"inDataObjectPackageIdList\":[]\n" +
                "},\n" +
                "\"dataObjectRefList\":{\n" +
                "\"inDataObjectPackageIdList\":[]\n" +
                "},\n" +
                "\"inDataObjectPackageId\":\"ID56\",\n" +
                "\n" +
                "}";

        testau = TestUtilities.LineEndNormalize(testau.replaceAll("\"onDiskPath\" : .*\"", ""));

        ArchiveUnit au = di.getArchiveTransfer().getDataObjectPackage().getAuInDataObjectPackageIdMap().get("ID56");
        String sau = mapper.writeValueAsString(au);
        sau = TestUtilities.LineEndNormalize(sau.replaceAll("\"onDiskPath\" : .*\"", ""));

        assertEquals(sau, testau);
    }
}
