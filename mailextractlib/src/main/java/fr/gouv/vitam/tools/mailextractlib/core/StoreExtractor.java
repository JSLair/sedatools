/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.tools.mailextractlib.core;

import fr.gouv.vitam.tools.mailextractlib.nodes.ArchiveUnit;
import fr.gouv.vitam.tools.mailextractlib.store.javamail.JMStoreExtractor;
import fr.gouv.vitam.tools.mailextractlib.store.microsoft.msg.MsgStoreExtractor;
import fr.gouv.vitam.tools.mailextractlib.store.microsoft.pst.PstStoreExtractor;
import fr.gouv.vitam.tools.mailextractlib.store.microsoft.pst.embeddedmsg.PstEmbeddedStoreExtractor;
import fr.gouv.vitam.tools.mailextractlib.utils.DateRange;
import fr.gouv.vitam.tools.mailextractlib.utils.MailExtractLibException;
import fr.gouv.vitam.tools.mailextractlib.utils.MailExtractProgressLogger;
import org.apache.poi.util.IOUtils;

import javax.mail.URLName;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.tools.mailextractlib.utils.MailExtractProgressLogger.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * Abstract factory class for operation context on a defined mailbox or mail
 * file.
 *
 * <p>
 * The {@link #createStoreExtractor createStoreExtractor} call create a
 * StoreExtractor subclass convenient for the declared protocol. This creation
 * specify the target of extraction, the different options of extraction, or
 * listing, and the log level. It makes a first level of connection and
 * compliance check to warranty other methods calls a viable mail box access.
 * <p>
 * The StoreExtractor use sub class extractors to manage different protocols or
 * formats (called schemes). There are defaults sub class for:
 * <ul>
 * <li>IMAP/IMAPS/POP3 (GIMAP experimental) server with user/password login</li>
 * <li>Thunderbird directory containing mbox files and .sbd directory
 * hierarchy</li>
 * <li>mbox file</li>
 * <li>eml file</li>
 * <li>Outlook pst file</li>
 * <li>Msg file</li>
 * </ul>
 * Notice: you have to call {@link #initDefaultExtractors initDefaultExtractors}
 * method before any usage to benefit from these existing extractors, and you
 * can add new ones with {@link #addExtractionRelation addExtractionRelation},
 * and they will be automatically used when needed.
 *
 * <p>
 * The extraction generate on disk a directories/files structure convenient for
 * SEDA archive packet (NF Z44-022), which is:
 * <ul>
 * <li>each folder is extracted as a directory named "Folder#'UniqID': 'name'",
 * with uniqID being a unique ID innerly generated and name being the n first
 * characters of the folder name UTF-8 encoded (n is defined by the option
 * namesLength, default being 12). The folder descriptive metadata is in the
 * file named ArchiveUnitContent.xml or, in V2 model, __ArchiveUnitMetadata (XML/UTF-8 encoded) in its directory. It
 * represent the folder ArchiveUnit with no objects</li>
 * <li>each message is extracted in a directory named "Message#'UniqID':
 * 'name'", with name being the n first characters of the message title UTF-8
 * encoded. It represent the message ArchiveUnit. In this message directory,
 * there's:
 * <ul>
 * <li>the message descriptive metadata file (ArchiveUnitContent.xml or,
 * in V2 model, __ArchiveUnitMetadata),</li>
 * <li>one directory for each special attachments being extractible stores
 * (attached messages or attached files formatted in eml, msg, pst...) (if any)
 * <ul>
 * <li>if simple message, it's named "Message#'UniqID': 'name'", with name being
 * the n first characters of the message title UTF-8 encoded, and contents the
 * message extraction,</li>
 * <li>if complex extraction, it's named "Container#'UniqID': 'name'", with name
 * being the n first characters of the attachment name (if non default to
 * 'infile') UTF-8 encoded, and contents the container descriptive metadata file
 * (ArchiveUnitContent.xml or, in V2 model, __ArchiveUnitMetadata) and
 * the hierarchy of extracted folders, messages, attachments...</li>
 * </ul>
 * <li>one directory by attachment (if any) named "Attachment#'UniqID': 'name'"
 * with the name being the n first characters of the attachment filename. In
 * this folder are :
 * <ul>
 * <li>the attachment descriptive metadata file (ArchiveUnitContent.xml
 * or, in V2 model, __ArchiveUnitMetadata),
 * and</li>
 * <li>the attachment binary file, being a final object, named according the
 * format "__'ObjectType'_'Version'_'filename'" or, in V2 model,
 * "__'ObjectType'_'Version'__'filename'", with in this case ObjectType
 * being "BinaryMaster", version being "1", filename being the attachment file
 * name with extension if any.</li>
 * <li>the text version of attachment binary file, if option is set, named
 * according the format "__'ObjectType'_'Version'_'filename'" or, in V2 model,
 * * "__'ObjectType'_'Version'__'filename'", with in this case
 * ObjectType being "TextContent", version being "1", filename being the
 * attachment file name with extension if any.</li>
 * </ul></li>
 * <li>the message body in eml format file, being a final object, named
 * "__BinaryMaster_1_'messageID'", or in V2 model, "__BinaryMaster_1__'messageID'"
 * with the messageID being the n first characters
 * of the uniq messageID , and</li>
 * <li>the message body in text format file, if option is set, being a final
 * object, named "__TextContent_1_'messageID'", or in V2 model,
 * "__TextContent_1__'messageID'" with the messageID being the n
 * first characters of the uniq messageID.</li>
 * </ul>
 * </ul>
 * Note: any folder's name is modified with starting and ending "__" when the
 * ArchiveUnit, the folder represents, contains an object.
 * <p>
 * For detailed information on descritptive metadata collected see
 * {@link StoreFolder} and {@link StoreMessage}.
 * <p>
 * The extraction or listing operation is logged on console and file
 * (root/username[-timestamp].log - cf args). At the different levels (using
 * {@link MailExtractProgressLogger}) you can have: information
 * about global process and extraction errors (GLOBAL),
 * warning about extraction problems and items dropped (WARNING), list of
 * treated folders (FOLDER), accumulated count of treated messages (MESSAGE_GROUP),
 * list of treated messages (MESSAGE), problems with some expected metadata (MESSAGE_DETAILS).
 * <p>
 * It's also possible (ruled by option) to generate a csv file with one line by
 * extracted message with a selection of metadata, including appointment details
 * <p>
 * Note: Default or generated metadata values are, for now, hardcoded in french
 */
public abstract class StoreExtractor {

    // Map of StoreExtractor classes by scheme


    /**
     * The formatter used for zoned date conversion to String
     */
    public static final DateTimeFormatter ISO_8601 = new DateTimeFormatterBuilder()
                .append(ISO_LOCAL_DATE_TIME)
                .optionalStart()
                .appendOffsetId()
                .optionalStart()
                .toFormatter();

    /**
     * The map of mimetypes/scheme known relations.
     */
    static HashMap<String, String> mimeTypeSchemeMap = new HashMap<>();

    /**
     * The map of droidformat/scheme known relations.
     */
    static HashMap<String, String> droidFormatSchemeMap = new HashMap<>();

    /**
     * The map of scheme/extractor class known relations.
     */
    @SuppressWarnings("rawtypes")
    static HashMap<String, Class> schemeStoreExtractorClassMap = new HashMap<>();

    /**
     * The map of scheme/container extraction (vs single file extraction) known
     * relations.
     */
    static HashMap<String, Boolean> schemeContainerMap = new HashMap<>();

    /**
     * Subscribes all defaults store extractor.
     */
    public static void initDefaultExtractors() {
        JMStoreExtractor.subscribeStoreExtractor();
        MsgStoreExtractor.subscribeStoreExtractor();
        PstStoreExtractor.subscribeStoreExtractor();
        PstEmbeddedStoreExtractor.subscribeStoreExtractor();

        // HMEF calls when extracting TNEF can generate an exception if an attachment extraction is bigger than 1Mo,
        // this change the limit to Integer.MAX_VALUE that is to say 2Go
        // This is also a bypass to a bug in POI 4.0 and 4.1 version when opening msg
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);
    }

    // StoreExtractor definition parameters

    /**
     * Scheme defining specific store extractor (imap| imaps| pop3| thunderbird|
     * mbox| eml| pst| msg| experimental gimap)...)
     */
    protected String scheme;

    /**
     * Hostname of target store in ((hostname|ip)[:port]) *.
     */
    protected String host;

    /**
     * Port of target store in ((hostname|ip)[:port]) *.
     */
    protected int port;

    /**
     * User account name, can be null if not used *.
     */
    protected String user;

    /**
     * Password, can be null if not used *.
     */
    protected String password;

    /**
     * Path of ressource to extract *.
     */
    protected String path;

    /**
     * Path of the folder in the store used as root for extraction, can be null
     * if default root folder.
     */
    private String rootStoreFolderName;

    /**
     * Path of the directory where will be the extraction directory.
     */
    protected String destRootPath;

    /**
     * Name of the extraction directory.
     */
    protected String destName;

    /**
     * Extractor options, flags coded on an int, defined thru constants *.
     */
    protected StoreExtractorOptions options;

    /**
     * Extractor context description.
     */
    protected String description;

    // private field for global statictics
    private long totalRawSize;

    // private field for time statistics
    private Instant start;
    private Instant end;

    // private object extraction root folder in store
    private StoreFolder rootStoreFolder;

    // private root storeExtractor for nested extraction, null if root
    private StoreExtractor rootStoreExtractor;

    // private father element for nested extraction, null if root
    private StoreElement fatherElement;

    // private logger
    private MailExtractProgressLogger logger;

    /**
     * The Global lists ps map.
     * private map of printstreams for global lists extraction
     * (messages, contacts, appointments...)
     */
    protected Map<String, PrintStream> globalListsPSMap;

    /**
     * The extracted elements counters map.
     * private map of counters for extracted elements
     * (messages, contacts, appointments...)
     */
    protected Map<String, Integer> elementsCounterMap;

    /**
     * The sub-extracted elements counters map.
     * private map of counters for extracted elements from inner containers
     * (messages, contacts, appointments...)
     */
    protected Map<String, Integer> subElementsCounterMap;

    /**
     * Add mimetypes, scheme, isContainer, store extractor known relation.
     * <p>
     * This is used by store extractor sub classes to subscribe. When the
     * relation is known it can be used for processing automatically the
     * mimetype files with appropriate store extractors, in code.
     * <p>
     * Warning: the mime type has to be the code returned by tika!
     *
     * @param mimeType    the mime type
     * @param scheme      the scheme
     * @param isContainer the is container
     * @param extractor   the extractor
     */
    @SuppressWarnings("rawtypes")
    public static void addExtractionRelation(String mimeType, String droidFormat, String scheme, boolean isContainer, Class extractor) {
        // if there is a file mimetype for this scheme
        if (mimeType != null)
            mimeTypeSchemeMap.put(mimeType, scheme);
        if (droidFormat != null)
            droidFormatSchemeMap.put(droidFormat, scheme);
        schemeStoreExtractorClassMap.put(scheme, extractor);
        schemeContainerMap.put(scheme, isContainer);
    }

    /**
     * Get protocol if droid format is a mail format ready for extraction.
     *
     * @param droidFormat the Droid format
     * @return the protocol
     */
    public static String getProtocolFromDroidFormat(String droidFormat) {
        return droidFormatSchemeMap.get(droidFormat);
    }

    /**
     * Compose an URL String.
     *
     * @param scheme    Type of local store to extract (thunderbird|pst|eml|mbox) or                  protocol for server access (imap|imaps|pop3...)
     * @param authority Server:port of target account ((hostname|ip)[:port
     * @param user      User account name, can be null if not used
     * @param password  Password, can be null if not used
     * @param path      Path to the ressource
     * @return the string
     */
    public static String composeStoreURL(String scheme, String authority, String user, String password, String path) {
        String result = null;

        try {
            result = scheme + "://";
            if (user != null && !user.isEmpty()) {
                result += URLEncoder.encode(user, "UTF-8");
                if (password != null && !password.isEmpty())
                    result += ":" + URLEncoder.encode(password, "UTF-8");
                result += "@";
            }
            if (authority != null && !authority.isEmpty())
                result += authority;
            else
                result += "localhost";
            if (path != null && !path.isEmpty())
                result += "/" + URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // impossible case with UTF-8
        }
        return result;
    }

    /**
     * Init the PrintStream for a global list generated for a certain type of element (message, folder, appointment, contact...),
     * if not already done with the header in csv format, and return the printstream
     *
     * @param listClass the list class
     * @return the initialized global list ps
     */
    @SuppressWarnings("unchecked")
    public PrintStream getGlobalListPS(Class listClass) {
        String globalListName = null;
        PrintStream result = null;
        try {
            globalListName = (String) listClass.getMethod("getElementName").invoke(null);
            result = globalListsPSMap.get(globalListName);
            if (result == null) {
                String dirname = this.destRootPath
                        + File.separator + this.destName + File.separator;
                Files.createDirectories(Paths.get(dirname));
                result = new PrintStream(dirname + listClass.getMethod("getElementName").invoke(null) + ".csv");
                globalListsPSMap.put(globalListName, result);
                listClass.getMethod("printGlobalListCSVHeader", PrintStream.class).invoke(null, result);
            }
        } catch (IOException | NoSuchMethodException | IllegalAccessException e) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create global list for [" + globalListName + "] csv file", e);
        } catch (InvocationTargetException te) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create global list for [" + globalListName + "] csv file", te.getTargetException());
        }
        return result;
    }

    /**
     * Return the counter for a certain type of extracted element (message, folder, appointment, contact...) if it exists.
     * <p>If not init the counter to 0, and return the counter.
     * <p>If subFlag is true, it actually act on the sub-extracted elements from inner containers counter.
     *
     * @param listClass the list class
     * @param subFlag   the sub extracted flag
     * @return the initialized global list counter
     */
    @SuppressWarnings("unchecked")
    public int getElementCounter(Class listClass, boolean subFlag) {
        String elementName = null;
        Integer result = 0;
        try {
            elementName = (String) listClass.getMethod("getElementName").invoke(null);
            result = (subFlag ? subElementsCounterMap : elementsCounterMap).get(elementName);
            if (result == null) {
                (subFlag ? subElementsCounterMap : elementsCounterMap).put(elementName, 0);
                result = 0;
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create counter for " + (subFlag ? "sub " : "") + "[" + elementName + "] csv file", e);
        } catch (InvocationTargetException te) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create counter for " + (subFlag ? "sub " : "") + "[" + elementName + "] csv file", te.getTargetException());
        }
        return result;
    }

    /**
     * Add a value to the counter for a certain type of extracted element (message, folder, appointment, contact...), and return the counter if it exists.
     * <p>If not init the counter to value, and return the counter.
     * <p>If subFlag is true, it actually act on the sub-extracted elements from inner containers counter.
     *
     * @param value     the value
     * @param listClass the list class
     * @param subFlag   the sub flag
     * @return the int
     */
    @SuppressWarnings("unchecked")
    public int addElementCounter(int value, Class listClass, boolean subFlag) {
        String elementName = null;
        Integer result = 0;
        try {
            elementName = (String) listClass.getMethod("getElementName").invoke(null);
            result = (subFlag ? subElementsCounterMap : elementsCounterMap).get(elementName);
            if (result == null)
                result = value;
            else
                result += value;
            (subFlag ? subElementsCounterMap : elementsCounterMap).put(elementName, result);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create counter for " + (subFlag ? "sub " : "") + "[" + elementName + "] csv file", e);
        } catch (InvocationTargetException te) {
            doProgressLogWithoutInterruption(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: can't create counter for " + (subFlag ? "sub " : "") + "[" + elementName + "] csv file", te.getTargetException());
        }
        return result;
    }

    /**
     * Increment the counter for a certain type of extracted element (message, folder, appointment, contact...), and return the counter if it exists.
     * <p>If not init the counter to 1, and return the counter.
     *
     * @param listClass the list class
     * @return the int
     */
    @SuppressWarnings("unchecked")
    public int incElementCounter(Class listClass) {
        return addElementCounter(1, listClass, false);
    }

    // sub elements accumulated
    static Class[] accumulatedElements = {StoreFolder.class, StoreMessage.class, StoreAppointment.class, StoreContact.class};

    /**
     * Accumulate elements of a container extractor in sub element counters.
     *
     * @param subExtractor the sub extractor
     */
    public void accumulateSubElements(StoreExtractor subExtractor) {
        for (Class c : accumulatedElements) {
            int value = subExtractor.getElementCounter(c, false) +
                    subExtractor.getElementCounter(c, true);
            if (value > 0)
                addElementCounter(value, c, true);
        }
    }

    /**
     * Close the PrintStream in map. Can be override if necessary
     */
    protected void closeGlobalListsPSMap() {
        for (String psName : globalListsPSMap.keySet()) {
            globalListsPSMap.get(psName).close();
        }
    }

    /**
     * Instantiates a new store extractor.
     *
     * @param urlString           the url string
     * @param rootStoreFolderName Path of the extracted folder in the store box, can be null if default root folder
     * @param destPathString      the dest path string
     * @param options             Extractor options
     * @param rootStoreExtractor  the creating store extractor in nested extraction, or null if root one
     * @param fatherElement       the father element in nested extraction, or null if root one
     * @param logger              logger used
     * @throws MailExtractLibException Any unrecoverable extraction exception (access trouble, major                             format problems...)
     */
    protected StoreExtractor(String urlString, String rootStoreFolderName, String destPathString, StoreExtractorOptions options,
                             StoreExtractor rootStoreExtractor, StoreElement fatherElement, MailExtractProgressLogger logger) throws MailExtractLibException {

        URLName url;
        url = new URLName(urlString);

        this.scheme = url.getProtocol();
        this.host = url.getHost();
        this.port = url.getPort();
        try {
            if (url.getUsername() != null)
                this.user = URLDecoder.decode(url.getUsername(), "UTF-8");
            if (url.getPassword() != null)
                this.password = URLDecoder.decode(url.getPassword(), "UTF-8");
            if (url.getFile() != null)
                this.path = URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // not possible
        }
        this.rootStoreFolderName = rootStoreFolderName;
        this.destRootPath = Paths.get(destPathString).toAbsolutePath().normalize().getParent().toString();
        this.destName = Paths.get(destPathString).toAbsolutePath().normalize().getFileName().toString();
        if (options == null)
            this.options = new StoreExtractorOptions();
        else
            this.options = options;

        this.totalRawSize = 0;

        this.rootStoreExtractor = rootStoreExtractor;
        this.fatherElement = fatherElement;
        this.logger = logger;

        this.description = ":p:" + scheme + ":u:" + user;

        globalListsPSMap = new HashMap<>();
        elementsCounterMap = new HashMap<>();
        subElementsCounterMap = new HashMap<>();

        doProgressLogIfDebug(logger,"StoreExtractor ["+this+"] created with url="+urlString+
                " rootFolder="+rootStoreFolderName+" destPath="+destPathString+" rootExtractor="+rootStoreExtractor,null);
    }

    /**
     * Log the context of the StoreExtractor.
     *
     * @throws InterruptedException the interrupted exception
     */
    public void writeTargetLog() throws InterruptedException {

        // if root extractor log extraction context
        if (rootStoreExtractor == null) {
            doProgressLog(logger, MailExtractProgressLogger.GLOBAL,
                    "mailextract :target store with scheme=" + scheme + (host == null || host.isEmpty() ? "" : "  server=" + host)
                            + (port == -1 ? "" : ":" + Integer.toString(port))
                            + (user == null || user.isEmpty() ? "" : " user=" + user)
                            + (password == null || password.isEmpty() ? "" : " password=" + password)
                            + (path == null || path.isEmpty() ? "" : " path=" + path)
                            + (rootStoreFolderName == null || rootStoreFolderName.isEmpty() ? "" : " store folder=" + rootStoreFolderName), null);
            doProgressLog(logger, MailExtractProgressLogger.GLOBAL, "to " + destRootPath + " in " + destName + " directory", null);
            if ((logger != null) && logger.getDebugFlag())
                doProgressLog(logger, MailExtractProgressLogger.GLOBAL, "DEBUG MODE", null);

            boolean first = true;
            String optionsLog = "";
            if (options.keepOnlyDeepEmptyFolders) {
                optionsLog += "keeping all empty folders except root level ones";
                first = false;
            }
            if (options.dropEmptyFolders) {
                if (!first)
                    optionsLog += ", ";
                optionsLog += "droping all empty folders";
                first = false;
            }
            if (options.warningMsgProblem) {
                if (!first)
                    optionsLog += ", ";
                optionsLog += "generate warning when there's a problem on a message (otherwise log at FINEST level)";
                first = false;
            }
            if (!first)
                optionsLog += ", ";
            optionsLog += "with names length=" + Integer.toString(options.namesLength);
            optionsLog += ", ";
            optionsLog += "with log level " + getProgressLogger().getLevelName();

            doProgressLog(logger, MailExtractProgressLogger.GLOBAL, optionsLog, null);
        }
        // if internal extractor give attachment context
    }

    /**
     * Gets the logger created during the store extractor construction, and used
     * in all mailextract classes.
     *
     * <p>
     * For convenience each class which may have some log actions has it's own
     * getProgressLogger method always returning this store extractor logger.
     *
     * @return logger progress logger
     */
    public MailExtractProgressLogger getProgressLogger() {
        return logger;
    }

    private int uniqID = 1;

    /**
     * Checks for dest name.
     *
     * @return true, if successful
     */
    public boolean hasDestName() {
        return !((destName == null) || destName.isEmpty());
    }

    /**
     * Gets a uniq ID in store extractor context.
     * <p>
     * Sequence incremented at each call in root store extractor context to
     * garanty unicity for the whole extraction process even in nested
     * extractions.
     *
     * @return a uniq ID
     */
    public int getUniqID() {
        int id;
        if (rootStoreExtractor == null)
            id = uniqID++;
        else
            id = rootStoreExtractor.getUniqID();
        return id;
    }

    /**
     * Add to total raw size.
     *
     * @param elementSize the element size
     */
    public void addTotalRawSize(long elementSize) {
        totalRawSize += elementSize;
    }

    /**
     * Gets the total raw size of all analyzed elements.
     * <p>
     * The "raw" size is the sum of the size of elements as in the store, the
     * extraction will be larger (up to x2)
     *
     * @return the total raw size
     */
    public long getTotalRawSize() {
        return totalRawSize;
    }

    /**
     * Gets the extraction context description.
     *
     * @return the description String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Is the store extractor the root one in nested extraction.
     *
     * @return the description String
     */
    public boolean isRoot() {
        return rootStoreExtractor == null;
    }

    /**
     * Gets the extraction root folder in store.
     *
     * @return the root StoreFolder
     */
    public StoreFolder getRootFolder() {
        return rootStoreFolder;
    }

    /**
     * Sets the extraction root folder in store.
     *
     * @param rootFolder the new root folder
     */
    public void setRootFolder(StoreFolder rootFolder) {
        rootStoreFolder = rootFolder;
    }

    /**
     * Gets the store extractor options.
     *
     * @return the store extractor options
     */
    public StoreExtractorOptions getOptions() {
        return options;
    }

    /**
     * Gets father element in nested extraction, null if root.
     *
     * @return the father element
     */
    public StoreElement getFatherElement() {
        return fatherElement;
    }

    /**
     * Create a store extractor for the declared scheme in url as a factory
     * creator.
     *
     * @param urlString           the url string
     * @param rootStoreFolderName Path of the extracted folder in the account mail box, can be                       null if default root folder
     * @param destPathString      the dest path string
     * @param options             Options (flag composition of CONST_)
     * @param logger              logger used
     * @return the store extractor, constructed as a non abstract subclass
     * @throws MailExtractLibException Any unrecoverable extraction exception (access trouble, major                             format problems...)
     */
    public static StoreExtractor createStoreExtractor(String urlString, String rootStoreFolderName, String destPathString,
                                                      StoreExtractorOptions options, MailExtractProgressLogger logger) throws MailExtractLibException {
        StoreExtractor storeExtractor;

        storeExtractor = createInternalStoreExtractor(urlString, rootStoreFolderName, destPathString, options, null, logger);

        return storeExtractor;
    }

    /**
     * Create an internal depth store extractor as a factory creator.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static StoreExtractor createInternalStoreExtractor(String urlString, String rootStoreFolderName,
                                                               String destPathString, StoreExtractorOptions options, StoreExtractor
                                                                       rootStoreExtractor, MailExtractProgressLogger logger
    ) throws MailExtractLibException {

        StoreExtractor store;
        URLName url;

        url = new URLName(urlString);

        // get read of leading file separator in folder
        if ((rootStoreFolderName != null) && (!rootStoreFolderName.isEmpty()) && (rootStoreFolderName.substring(0, 1).equals(File.separator)))
            rootStoreFolderName = rootStoreFolderName.substring(1);

        // find the store extractor constructor for scheme in URL
        Class storeExtractorClass = StoreExtractor.schemeStoreExtractorClassMap.get(url.getProtocol());
        if (storeExtractorClass == null) {
            throw new MailExtractLibException("mailextractlib: unknown store type=" + url.getProtocol(), null);
        } else {
            try {
                store = (StoreExtractor) storeExtractorClass.getConstructor(String.class, String.class, String.class,
                        StoreExtractorOptions.class, StoreExtractor.class, MailExtractProgressLogger.class)
                        .newInstance(urlString, rootStoreFolderName, destPathString, options, rootStoreExtractor, logger);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
                    | SecurityException e) {
                throw new MailExtractLibException("mailextractlib: dysfonctional store type=" + url.getProtocol(), e);
            } catch (InvocationTargetException e) {
                Throwable te = e.getCause();
                throw new MailExtractLibException("mailextractlib: dysfonctional store type=" + url.getProtocol(), te);
            }
        }
        return store;

    }

    private static String readableFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Get summary string with time and statistics.
     *
     * @return the string
     */
    public String getSummary() {
        String summary = Duration.between(start, end).toString();
        String elementSummary = "";
        for (Class elementClass : accumulatedElements) {
            String elementName = null;
            try {
                elementName = (String) elementClass.getMethod("getElementName").invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                doProgressLogIfDebug(logger,"GetSummary error",e);
            }
            Integer count = elementsCounterMap.get(elementName);
            if ((count != null) && (count > 0)) {
                if (!elementSummary.isEmpty())
                    elementSummary += ",";
                elementSummary += " " + count + " " + elementName;
            }
        }
        if (elementSummary.isEmpty())
            summary += " empty extraction";
        else
            summary += " writing" + elementSummary;
        String subElementSummary = "";
        for (Class elementClass : accumulatedElements) {
            String elementName = null;
            try {
                elementName = (String) elementClass.getMethod("getElementName").invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                doProgressLogIfDebug(logger,"GetSummary error",e);
            }
            Integer count = subElementsCounterMap.get(elementName);
            if ((count != null) && (count > 0)) {
                if (!subElementSummary.isEmpty())
                    subElementSummary += ",";
                subElementSummary += " " + count + " " + elementName;
            }
        }
        if (subElementSummary.isEmpty())
            summary += " without embedded elements";
        else
            summary += " with" + subElementSummary + " embedded";
        summary += " for a total size of " + readableFileSize(getTotalRawSize());
        return summary;
    }

    /**
     * Extract all folders from the defined root folder (considering drop
     * options).
     *
     * <p>
     * This is a method where the extraction structure and content is partially
     * defined (see also {@link StoreMessage#extractMessage extractMessage} and
     * {@link StoreFolder#extractFolder extractFolder}).
     *
     * @throws MailExtractLibException Any unrecoverable extraction exception (access trouble, major                             format problems...)
     * @throws InterruptedException    the interrupted exception
     */
    public void extractAllFolders() throws MailExtractLibException, InterruptedException {
        String title;

        start = Instant.now();

        writeTargetLog();
        doProgressLog(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: extraction begin", null);

        rootStoreFolder.extractFolderAsRoot(true);

        ArchiveUnit rootNode = rootStoreFolder.getArchiveUnit();
        rootNode.addMetadata("DescriptionLevel", "RecordGrp", true);

        // title generation from context
        if ((user != null) && (!user.isEmpty()))
            title = "Ensemble des messages électroniques et informations associées (contacts, rendez-vous...) envoyés et reçus par le compte " + user;
        else if ((path != null) && (!path.isEmpty()))
            title = "Ensemble des messages électroniques et informations associées (contacts, rendez-vous...) du container " + path;
        else
            title = "Ensemble de messages électroniques et informations associées (contacts, rendez-vous...)";
        if ((host != null) && (!host.isEmpty()) && (!host.equals("localhost")))
            title += " sur le serveur " + host + (port == -1 ? "" : ":" + Integer.toString(port));
        title += " à la date du " + start;
        rootNode.addMetadata("Title", title, true);
        if (rootStoreFolder.dateRange.isDefined()) {
            rootNode.addMetadata("StartDate", DateRange.getISODateString(rootStoreFolder.dateRange.getStart()),
                    true);
            rootNode.addMetadata("EndDate", DateRange.getISODateString(rootStoreFolder.dateRange.getEnd()), true);
        }
        rootNode.write();

        end = Instant.now();
        String summary = "Terminated in " + getSummary();
        doProgressLog(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: " + summary, null);
        System.out.println(summary);
    }

    /**
     * List all folders from the defined root folder (no drop options).
     *
     * <p>
     * Warning: listing with detailed information is a potentially expensive
     * operation, especially when accessing distant account, as all elements are
     * inspected (in the case of a distant account that mean also
     * downloaded...).
     *
     * @param stats true if detailed information (number and raw size of elements              in each folder) is asked for
     * @throws MailExtractLibException Any unrecoverable extraction exception (access trouble, major                             format problems...)
     * @throws InterruptedException    the interrupted exception
     */
    public void listAllFolders(boolean stats) throws MailExtractLibException, InterruptedException {
        String time;
        String tmp;
        Duration d;

        start = Instant.now();

        writeTargetLog();
        doProgressLog(logger, MailExtractProgressLogger.GLOBAL, "mailextractlib: listing begin", null);

        rootStoreFolder.listFolder(stats);

        end = Instant.now();
        System.out.println("--------------------------------------------------------------------------------");

        d = Duration.between(start, end);
        time = String.format("%dm%02ds", d.toMinutes(), d.minusMinutes(d.toMinutes()).getSeconds());
        tmp = String.format("mailextractlib: terminated in %s listing %d folders", time, getElementCounter(StoreFolder.class, false));
        if (stats) {
            tmp += String.format(" with %s messages, for %.2f MBytes",
                    Integer.toString(getElementCounter(StoreMessage.class, false)), ((double) getTotalRawSize()) / (1024.0 * 1024.0));
        }

        doProgressLog(logger, MailExtractProgressLogger.GLOBAL, tmp, null);
        System.out.println(tmp);
    }

    /**
     * Do all end tasks for the StoreExtractor, like deleting temporary files.
     *
     * @throws MailExtractLibException the extraction exception
     */
    public void endStoreExtractor() throws MailExtractLibException {
        closeGlobalListsPSMap();
    }

    /**
     * Checks for magic number.
     *
     * @param content     the content
     * @param magicNumber the magic number
     * @return true, if successful
     */
// Utility function to detect if the four bytes is a defined magic number
    public static boolean hasMagicNumber(byte[] content, byte[] magicNumber) {
        return hasMagicNumber(content, magicNumber, 0);
    }

    // Utility function to detect if the bytes at offset is a defined magic

    /**
     * Checks for magic number.
     *
     * @param content     the content
     * @param magicNumber the magic number
     * @param offset      the offset
     * @return true, if successful
     */
// number
    public static boolean hasMagicNumber(byte[] content, byte[] magicNumber, int offset) {
        if (content.length < magicNumber.length + offset)
            return false;
        for (int i = 0; i < magicNumber.length; i++) {
            if (content[i + offset] != magicNumber[i])
                return false;
        }
        return true;
    }

    /**
     * Gets the attachment in which is the embedded object.
     *
     * @return the attachment
     */
    public abstract StoreAttachment getAttachment();

    /**
     * Tests if this store extractor can generate objects lists
     * (mails, contacts, appointments...).
     *
     * @return the flag true or false
     */
    public abstract boolean canExtractObjectsLists();

    /**
     * Gets the scheme if this content can be managed by this StoreExtractor, or
     * null
     *
     * @param content the content
     * @return the scheme
     */
    public static String getVerifiedScheme(byte[] content) {
        return null;
    }
}