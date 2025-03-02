package fr.gouv.vitam.tools.mailextractlib.core;

import fr.gouv.vitam.tools.mailextractlib.nodes.ArchiveUnit;
import fr.gouv.vitam.tools.mailextractlib.utils.MailExtractLibException;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Formatter;
import java.util.List;

import static fr.gouv.vitam.tools.mailextractlib.core.StoreExtractor.ISO_8601;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

public abstract class StoreAppointment extends StoreElement {

    public static final int MESSAGE_STATUS_UNKNOWN = 0;
    public static final int MESSAGE_STATUS_LOCAL = 1;
    public static final int MESSAGE_STATUS_REQUEST = 2;
    public static final int MESSAGE_STATUS_RESPONSE_YES = 3;
    public static final int MESSAGE_STATUS_RESPONSE_MAY = 4;
    public static final int MESSAGE_STATUS_RESPONSE_NO = 5;

    public static final String[] MESSAGE_STATUS_TEXT = {"Unknown", "Local", "Request", "Resp.Yes", "Resp.May", "Resp.No"};

    protected String uniqId;

    protected String subject, location;
    protected String from;
    protected String toAttendees;
    protected String ccAttendees;
    protected ZonedDateTime startTime;
    protected ZonedDateTime endTime;
    protected String miscNotes;
    protected String otherMiscNotes;
    protected ZonedDateTime modificationTime;

    protected List<StoreAttachment> attachments;

    // for appointment organized with other people
    protected int sequenceNumber;
    protected int messageStatus;

    // for recurrent appointment
    protected String recurencePattern;
    protected ZonedDateTime startRecurrenceTime;
    protected ZonedDateTime endRecurrenceTime;
    protected List<StoreAppointment> exceptions;

    // for exceptions in recurring appointment
    protected boolean isRecurrenceDeletion;
    protected ZonedDateTime exceptionDate;

    /**
     * Instantiates a new appointment.
     *
     * @param storeFolder Mail box folder containing this appointment
     */
    protected StoreAppointment(StoreFolder storeFolder) {
        super(storeFolder);
    }

    @Override
    public String getLogDescription() {
        String result = "appointment " + getStoreExtractor().getElementCounter(this.getClass(), false);
        if (subject != null)
            result += " [" + subject + "/";
        else
            result += " [no subject/";
        result += getDateInDefinedTimeZone(startTime) + " - " + getDateInDefinedTimeZone(endTime) + "]";
        return result;
    }

    /**
     * Analyze contact to collect appointment information (protocol
     * specific).
     * <p>
     * This is the method for sub classes, where all appointment
     * information has to be extracted in standard representation out of the
     * inner native representation.
     *
     * @throws MailExtractLibException Any unrecoverable extraction exception (access trouble, major format problems...)
     * @throws InterruptedException    the interrupted exception
     */
    public abstract void analyzeAppointment() throws MailExtractLibException, InterruptedException;

    /**
     * Gets element name used for the csv file name construction.
     *
     * @return the element name
     */
    public static String getElementName() {
        return "appointments";
    }

    /**
     * Print the header for contacts list csv file
     *
     * @param ps the dedicated print stream
     */
    public static void printGlobalListCSVHeader(PrintStream ps) {
        ps.println("ID;Subject;Location;From;ToAttendees;CcAttendees;StartTime;EndTime;" +
                "MiscNotes;uniqID;SequenceNumber;ModificationTime;Folder;MessageStatus;" +
                "isRecurrent;RecurrencePattern;StartRecurrenceTime;EndRecurrenceTime;" +
                "ExceptionFromId;ExceptionDate;isDeletion;hasAttachment");
    }

    /**
     * Extract to the appointments list and all associated attachements and exceptions
     *
     * @param writeFlag the write flag
     * @param father    the father
     * @throws InterruptedException    the interrupted exception
     * @throws MailExtractLibException the mail extract lib exception
     */
    public void extractAppointment(boolean writeFlag, StoreAppointment father) throws InterruptedException, MailExtractLibException {
        if (writeFlag) {
            if (storeFolder.getStoreExtractor().getOptions().extractElementsList)
                writeToAppointmentsList(father);
            if (storeFolder.getStoreExtractor().getOptions().extractElementsContent) {
                if ((attachments != null) && (!attachments.isEmpty())) {
                    ArchiveUnit attachmentNode = new ArchiveUnit(storeFolder.storeExtractor, storeFolder.storeExtractor.destRootPath +
                            File.separator + storeFolder.storeExtractor.destName + File.separator + "appointments", "AppointmentAttachments#" + listLineId);
                    attachmentNode.addMetadata("DescriptionLevel", "RecordGrp", true);
                    attachmentNode.addMetadata("Title", "Appointment Attachments #" + listLineId, true);
                    attachmentNode.addMetadata("Description", "Appointment attachments extracted for " + subject + "[" + startTime + "-" + endTime + "]", true);
                    attachmentNode.addMetadata("StartDate", getDateInUTCTimeZone(startTime), true);
                    attachmentNode.addMetadata("EndDate", getDateInUTCTimeZone(endTime), true);
                    attachmentNode.write();
                    StoreAttachment.extractAttachments(attachments, attachmentNode, writeFlag);
                }
            }
            if (exceptions != null)
                for (StoreAppointment a : exceptions)
                    a.extractAppointment(writeFlag, this);
        }
    }

    private String getDateInUTCTimeZone(ZonedDateTime date) {
        String result;
        if (date != null)
            result = date.withZoneSameInstant(ZoneId.of("UTC")).format(ISO_8601);
        else
            result = "";
        return result;
    }

    private String getDateInDefinedTimeZone(ZonedDateTime date) {
        String result;
        if (date != null)
            result = date.format(ISO_8601);
        else
            result = "";
        return result;
    }

    private String normalizeUniqId(String uniqId) {
        String result = uniqId;
        byte[] hash;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(MD5);
            hash = md.digest(uniqId.getBytes(StandardCharsets.US_ASCII));
            Formatter formatter = new Formatter();
            for (final byte b : hash) {
                formatter.format("%02x", b);
            }
            result = formatter.toString();
            formatter.close();
        } catch (NoSuchAlgorithmException ignored) {
            //ignore
        }
        return result;
    }

    private void writeToAppointmentsList(StoreAppointment father) {
        PrintStream ps = storeFolder.getStoreExtractor().getGlobalListPS(this.getClass());
        ps.format("\"%d\";", listLineId);
        ps.format("\"%s\";", filterHyphenForCsv(subject));
        ps.format("\"%s\";", filterHyphenForCsv(location));
        ps.format("\"%s\";", filterHyphenForCsv(from));
        ps.format("\"%s\";", filterHyphenForCsv(toAttendees));
        ps.format("\"%s\";", filterHyphenForCsv(ccAttendees));
        ps.format("\"%s\";", getDateInDefinedTimeZone(startTime));
        ps.format("\"%s\";", getDateInDefinedTimeZone(endTime));
        ps.format("\"%s\";", filterHyphenForCsv(miscNotes));
        ps.format("\"%s\";", normalizeUniqId(uniqId));
        ps.format("\"%d\";", sequenceNumber);
        ps.format("\"%s\";", getDateInDefinedTimeZone(modificationTime));
        ps.format("\"%s\";", filterHyphenForCsv(storeFolder.getFullName()));
        ps.format("\"%s\";", MESSAGE_STATUS_TEXT[messageStatus]);
        ps.format("\"%s\";", ((recurencePattern != null) && (!recurencePattern.isEmpty()) ? "X" : ""));
        ps.format("\"%s\";", filterHyphenForCsv(recurencePattern));
        ps.format("\"%s\";", getDateInDefinedTimeZone(startRecurrenceTime));
        ps.format("\"%s\";", getDateInDefinedTimeZone(endRecurrenceTime));
        ps.format("\"%s\";", (father != null ? father.listLineId : ""));
        ps.format("\"%s\";", getDateInDefinedTimeZone(exceptionDate));
        ps.format("\"%s\";", (isRecurrenceDeletion ? "X" : ""));
        ps.format("\"%s\"", ((attachments != null) && (!attachments.isEmpty()) ? Integer.toString(attachments.size()) : ""));
        ps.println("");
        ps.flush();
    }

    @Override
    public void processElement(boolean writeFlag) throws InterruptedException, MailExtractLibException {
        if (storeFolder.getStoreExtractor().getOptions().extractAppointments) {
            listLineId = storeFolder.getStoreExtractor().incElementCounter(this.getClass());
            analyzeAppointment();
            StoreAttachment.detectStoreAttachments(attachments);
            extractAppointment(writeFlag, null);
        }
    }

    @Override
    public void listElement(boolean statsFlag) throws InterruptedException, MailExtractLibException {
        if (storeFolder.getStoreExtractor().getOptions().extractAppointments) {
            listLineId = storeFolder.getStoreExtractor().incElementCounter(this.getClass());
            analyzeAppointment();
            if (statsFlag)
                extractAppointment(false, null);
        }
    }
}
