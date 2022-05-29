package fr.gouv.vitam.tools.sedalib.process;

import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.BinaryDataObject;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
import fr.gouv.vitam.tools.sedalib.metadata.SEDAMetadata;
import fr.gouv.vitam.tools.sedalib.metadata.compactor.Document;
import fr.gouv.vitam.tools.sedalib.metadata.compactor.FileObject;
import fr.gouv.vitam.tools.sedalib.metadata.compactor.SubDocument;
import fr.gouv.vitam.tools.sedalib.metadata.content.Content;
import fr.gouv.vitam.tools.sedalib.metadata.namedtype.TextType;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger.doProgressLog;

public class Compactor {

    private static final String DESCRIPTION_LEVEL = "DescriptionLevel";


    /**
     * The data Object package containing archive unit to compact.
     */
    private DataObjectPackage dataObjectPackage;

    /**
     * The compacted documents tree management class and var.
     */
    class TreeNode {
        String id;
        Object value;
        List<TreeNode> childs;

        TreeNode(String id) {
            this.id = id;
            this.value = null;
            this.childs = new ArrayList<>();
        }
    }

    private TreeNode documentsRecordGroupTreeRoot;

    /**
     * The compacted documents list.
     */
    private List<Document> documentsList;

    /**
     * The compacted files management class and list var.
     */
    class CompactedFile {
        String compactedFilename;
        Path onDiskPath;

        CompactedFile(String compactedFilename, Path onDiskPath) {
            this.compactedFilename = compactedFilename;
            this.onDiskPath = onDiskPath;
        }
    }

    private List<CompactedFile> compactedFilesList;

    /**
     * The document data object version and content metada filter.
     */
    private List<String> documentObjectVersionFilter;
    private Map<String, Integer> documentContentMetadataFilter;

    /**
     * The sub document data object version and content metada filter.
     */
    private List<String> subDocumentObjectVersionFilter;
    private Map<String, Integer> subDocumentContentMetadataFilter;

    /**
     * The processed document, sub-docuement and archive unit counters.
     */
    private int documentCounter;
    private int subDocumentCounter;
    private int treenodeArchiveUnitCounter;
    private int leafArchiveUnitCounter;
    private int treenodeWithDroppedMetadata;
    private int leafWithDroppedMetadata;
    private int treenodeWithDroppedFile;
    private int leafWithDroppedFile;
    private int droppedCounted;

    /**
     * The archive unit to campact
     */
    private ArchiveUnit archiveUnit;

    /**
     * The start and end instants, for duration computation.
     */
    private Instant start, end;

    /**
     * The progress logger.
     */
    private final SEDALibProgressLogger sedaLibProgressLogger;

    /**
     * Instantiates a new Compactor.
     *
     * @param sedaLibProgressLogger the progress logger or null if no progress log expected
     */
    private Compactor(SEDALibProgressLogger sedaLibProgressLogger) {
        this.dataObjectPackage = null;
        this.sedaLibProgressLogger = sedaLibProgressLogger;
        this.documentObjectVersionFilter = List.of("BinaryContent");
        this.documentContentMetadataFilter = Map.of(DESCRIPTION_LEVEL, (Integer) 0, "Title", (Integer) 0);
        this.subDocumentObjectVersionFilter = List.of("BinaryContent");
        this.subDocumentContentMetadataFilter = Map.of(DESCRIPTION_LEVEL, 0, "Title", 0);
        this.documentsRecordGroupTreeRoot = null;
        this.documentsList = null;
        this.compactedFilesList = null;
        this.documentCounter = 0;
        this.subDocumentCounter = 0;
        this.treenodeArchiveUnitCounter = 0;
        this.leafArchiveUnitCounter = 0;
        this.treenodeWithDroppedMetadata = 0;
        this.leafWithDroppedMetadata = 0;
        this.treenodeWithDroppedFile = 0;
        this.leafWithDroppedFile = 0;
        this.droppedCounted = 0;
    }

    /**
     * Instantiates a new DataObjectPackage to disk exporter.
     *
     * @param archiveUnit           the archive unit
     * @param sedaLibProgressLogger the progress logger
     */
    public Compactor(ArchiveUnit archiveUnit, SEDALibProgressLogger sedaLibProgressLogger) {
        this(sedaLibProgressLogger);
        this.archiveUnit = archiveUnit;
        this.dataObjectPackage = archiveUnit.getDataObjectPackage();
    }

    private static String getExtension(String fileName) {
        if (fileName == null)
            return "";
        int i = fileName.lastIndexOf('.');
        return i < 0 ? "" : fileName.substring(i + 1);
    }

    private static String getExtendedCompactedFileName(String radical, Path onDiskPath) {
        String extension;
        extension = getExtension(onDiskPath.getFileName().toString());
        if (!extension.isEmpty())
            return radical + "." + extension;
        else return radical;
    }

    /**
     * Set object version filters.
     *
     * @param documentObjectVersionFilter    the document object version filter
     * @param subDocumentObjectVersionFilter the sub document object version filter
     */
    public void setObjectVersionFilters(List<String> documentObjectVersionFilter,
                                        List<String> subDocumentObjectVersionFilter) {
        this.documentObjectVersionFilter = documentObjectVersionFilter;
        this.subDocumentObjectVersionFilter = subDocumentObjectVersionFilter;
    }

    /**
     * Set metadata filters.
     *
     * @param documentContentMetadataFilter    the document content metadata filter
     * @param subDocumentContentMetadataFilter the sub document content metadata filter
     */
    public void setMetadataFilters(Map<String, Integer> documentContentMetadataFilter,
                                   Map<String, Integer> subDocumentContentMetadataFilter) {
        this.documentContentMetadataFilter = documentContentMetadataFilter;
        this.subDocumentContentMetadataFilter = subDocumentContentMetadataFilter;
    }

    private SEDAMetadata truncateTextType(SEDAMetadata sm, int limit) throws SEDALibException {
        if ((sm instanceof TextType) &&
                (((TextType) sm).getValue().length() > limit)) {
            sm = new TextType(sm.getXmlElementName(),
                    ((TextType) sm).getValue().substring(0, limit));
            droppedCounted++;
        } else
            throw new SEDALibException("Tentative pendant le compactage de troncature " +
                    "d'une métadonnée [" + sm.getXmlElementName() + "] qui n'est pas de type TextType");
        return sm;
    }

    private Content filterContentMetadata(boolean documentFlag, Content originContent) throws SEDALibException {
        Map<String, Integer> contentMetadataFilter;
        Content resultContent = new Content();

        contentMetadataFilter = (documentFlag ? documentContentMetadataFilter : subDocumentContentMetadataFilter);
        for (SEDAMetadata sm : originContent.metadataList) {
            Integer limit = contentMetadataFilter.get(sm.getXmlElementName());
            if (limit != null) {
                if (limit == 0)
                    resultContent.metadataList.add(sm);
                else
                    resultContent.metadataList.add(truncateTextType(sm, limit));
            } else
                droppedCounted++;
        }
        return resultContent;
    }

    private SubDocument getCompactedSubDocumentArchiveUnit(ArchiveUnit au, String parentAuURI) throws SEDALibException {
        SubDocument subDocument;
        String auURI, compactedFileURI;

        subDocument = new SubDocument(filterContentMetadata(false, au.getContent()));
        auURI = parentAuURI + File.separator + "SubDocument" + subDocumentCounter;
        subDocumentCounter++;
        for (BinaryDataObject bdo : au.getTheDataObjectGroup().getBinaryDataObjectList()) {
            String radical = bdo.dataObjectVersion.getValue().split("_")[0];
            if (subDocumentObjectVersionFilter.contains(radical)) {
                compactedFileURI = getExtendedCompactedFileName(auURI + "-" + bdo.dataObjectVersion.getValue(),
                        bdo.getOnDiskPath());
                subDocument.addMetadata(new FileObject(bdo, auURI));
                compactedFilesList.add(new CompactedFile(compactedFileURI,
                        bdo.getOnDiskPath()));
            } else
                leafWithDroppedFile++;
        }
        for (ArchiveUnit auChild : au.getChildrenAuList().getArchiveUnitList()) {
            subDocument.addMetadata(getCompactedSubDocumentArchiveUnit(auChild, auURI));
        }
        leafArchiveUnitCounter++;
        return subDocument;
    }

    private Document getCompactedDocumentArchiveUnit(ArchiveUnit au, TreeNode parentTreeNode) throws SEDALibException {
        Document document;
        String auURI, compactedFileURI;

        document = new Document(parentTreeNode.id,
                filterContentMetadata(true, au.getContent()));
        auURI = parentTreeNode.id + "-" + "Document" + documentCounter;
        for (BinaryDataObject bdo : au.getTheDataObjectGroup().getBinaryDataObjectList()) {
            String radical = bdo.dataObjectVersion.getValue().split("_")[0];
            if (documentObjectVersionFilter.contains(radical)) {
                compactedFileURI = getExtendedCompactedFileName(auURI + "-" + bdo.dataObjectVersion.getValue(),
                        bdo.getOnDiskPath());
                document.addMetadata(new FileObject(bdo, auURI));
                compactedFilesList.add(new CompactedFile(compactedFileURI,
                        bdo.getOnDiskPath()));
            } else leafWithDroppedFile++;
        }
        subDocumentCounter = 1;
        for (ArchiveUnit auChild : au.getChildrenAuList().getArchiveUnitList()) {
            document.addMetadata(getCompactedSubDocumentArchiveUnit(auChild, auURI));
        }
        documentCounter++;
        leafArchiveUnitCounter++;
        return document;
    }

    private TreeNode addTreeNodeChild(ArchiveUnit au, TreeNode parentTreeNode) throws SEDALibException {
        TreeNode curTreeNode;

        curTreeNode = new TreeNode("Tree" + treenodeArchiveUnitCounter);
        curTreeNode.value = au.getContent();
        if (parentTreeNode == null)
            documentsRecordGroupTreeRoot = curTreeNode;
        else parentTreeNode.childs.add(curTreeNode);
        if (!au.getDataObjectRefList().getDataObjectList().isEmpty())
            treenodeWithDroppedFile++;
        return curTreeNode;
    }

    private void recurseArchiveUnit(ArchiveUnit au, TreeNode parentTreeNode) throws SEDALibException {
        if (dataObjectPackage.isTouchedInDataObjectPackageId(au.getInDataObjectPackageId()))
            return;
        dataObjectPackage.addTouchedInDataObjectPackageId(au.getInDataObjectPackageId());
        if (au.getContent().getSimpleMetadata(DESCRIPTION_LEVEL).equals("RecordGroup")) {
            treenodeArchiveUnitCounter++;
            TreeNode curTreeNode = addTreeNodeChild(au, parentTreeNode);
            if ((au.getManagement() != null) || (au.getArchiveUnitProfile() != null))
                treenodeWithDroppedMetadata++;
            for (ArchiveUnit auChild : au.getChildrenAuList().getArchiveUnitList())
                recurseArchiveUnit(auChild, curTreeNode);
        } else
            documentsList.add(getCompactedDocumentArchiveUnit(au, parentTreeNode));
    }

    public void doCompact() throws SEDALibException, InterruptedException {
        Date d = new Date();
        start = Instant.now();
        String log = "Début du compactage de l'ArchiveUnit [" + archiveUnit.getInDataObjectPackageId() + "]\n";
        log += " date=" + DateFormat.getDateTimeInstance().format(d);
        doProgressLog(sedaLibProgressLogger, SEDALibProgressLogger.GLOBAL, log, null);

        this.documentsRecordGroupTreeRoot = null;
        this.documentsList = new ArrayList<>();
        this.compactedFilesList = new ArrayList<>();
        this.documentCounter = 0;
        this.subDocumentCounter = 0;
        this.treenodeArchiveUnitCounter = 0;
        this.leafArchiveUnitCounter = 0;
        this.treenodeWithDroppedMetadata = 0;
        this.leafWithDroppedMetadata = 0;
        this.leafWithDroppedFile = 0;

        recurseArchiveUnit(archiveUnit, null);
        System.out.println(documentsList);
        System.out.println(documentsRecordGroupTreeRoot);

        doProgressLog(sedaLibProgressLogger, SEDALibProgressLogger.GLOBAL, "sedalib: export d'un ArchiveTransfer dans une hiérarchie sur disque terminé", null);
        end = Instant.now();
    }

    /**
     * Gets the summary of the export process.
     *
     * @return the summary String
     */
    public String getSummary() {
        String result = "Compactage de l'ArchiveUnit [" + archiveUnit.getInDataObjectPackageId() + "]\n";
        if (leafWithDroppedMetadata != 0)
            result += "  avec " + leafWithDroppedMetadata + " item dont des métadonnées ont été éliminées\n";
        if ((start != null) && (end != null))
            result += "effectué en " + Duration.between(start, end).toString().substring(2) + "\n";
        return result;
    }
}
