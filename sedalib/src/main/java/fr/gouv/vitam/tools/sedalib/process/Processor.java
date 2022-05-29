/**
 * Copyright French Prime minister Office/DINSIC/Vitam Program (2015-2019)
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
package fr.gouv.vitam.tools.sedalib.process;

import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Class Processor.
 * <p>
 * Utility class for DataObjectPackage ArchiveUnits analysis and transformation.
 * <p>
 * It can either verify, analyze or transform all ArchiveUnits in the DataObjectPackage,
 * taking them once and only once into account.
 */
public class Processor {

    private Processor() {
        throw new IllegalStateException("Utility class");
    }

    private static int verifyArchiveUnitTree(DataObjectPackage dataObjectPackage,
                                             ArchiveUnit au,
                                             Function<ArchiveUnit, Boolean> test, boolean stopFirstFail) {
        int failCounter = 0;

        if (Boolean.FALSE.equals(test.apply(au))) {
            if (stopFirstFail)
                return 1;
            else failCounter++;
        }
        dataObjectPackage.addTouchedInDataObjectPackageId(au.getInDataObjectPackageId());
        for (ArchiveUnit auChild : au.getChildrenAuList().getArchiveUnitList()) {
            if (!dataObjectPackage.isTouchedInDataObjectPackageId(au.getInDataObjectPackageId())) {
                failCounter += verifyArchiveUnitTree(dataObjectPackage, auChild, test, stopFirstFail);
                if (stopFirstFail && (failCounter != 0))
                    return 1;
            }
        }
        return failCounter;
    }

    /**
     * Verify that ArchiveUnits in the DataObjectPackage do not fail passing the argument test function apply.
     * <p>
     * It can be exhaustive and return the fails count or stopped at the first fail and return 1.
     *
     * @param dataObjectPackage the data object package
     * @param test              the test function
     * @param stopFirstFail     the stop at the first fail flag
     * @return the fails count
     */
    public static int verify(DataObjectPackage dataObjectPackage,
                             Function<ArchiveUnit, Boolean> test, boolean stopFirstFail) {
        int failCounter = 0;

        dataObjectPackage.resetTouchedInDataObjectPackageIdMap();
        for (ArchiveUnit au : dataObjectPackage.getGhostRootAu().getChildrenAuList().getArchiveUnitList()) {
            failCounter += verifyArchiveUnitTree(dataObjectPackage, au, test, stopFirstFail);
            if (stopFirstFail && (failCounter != 0))
                return 1;
        }
        return failCounter;
    }

    private static List<Object> processAllCompliantsInArchiveUnitTree(DataObjectPackage dataObjectPackage,
                                                                      ArchiveUnit au,
                                                                      Function<ArchiveUnit, Boolean> test,
                                                                      Function<ArchiveUnit, Object> analyse,
                                                                      List<Object> resultList) {
        if (Boolean.TRUE.equals(test.apply(au)))
            resultList.add(analyse.apply(au));
        dataObjectPackage.addTouchedInDataObjectPackageId(au.getInDataObjectPackageId());
        for (ArchiveUnit auChild : au.getChildrenAuList().getArchiveUnitList()) {
            if (!dataObjectPackage.isTouchedInDataObjectPackageId(au.getInDataObjectPackageId()))
                processAllCompliantsInArchiveUnitTree(dataObjectPackage, auChild, test, analyse, resultList);
        }
        return resultList;
    }

    /**
     * Get results list of process function on all ArchiveUnits in the DataPackageObject
     * that pass the argument test function apply.
     * <p>
     * This can be use either to analyze the ArchiveUnit and get a result, or to transform the ArchiveUnit
     *
     * @param dataObjectPackage the data object package
     * @param test              the test function
     * @param process           the process function
     * @return the results list
     */
    public static List<Object> processAllCompliants(DataObjectPackage dataObjectPackage,
                                                         Function<ArchiveUnit, Boolean> test,
                                                         Function<ArchiveUnit, Object> process) {
        List<Object> resultList;

        dataObjectPackage.resetTouchedInDataObjectPackageIdMap();
        resultList = new ArrayList<>();
        for (ArchiveUnit au : dataObjectPackage.getGhostRootAu().getChildrenAuList().getArchiveUnitList()) {
            processAllCompliantsInArchiveUnitTree(dataObjectPackage, au,test, process, resultList);
        }
        return resultList;
    }

    /**
     * Get all ArchiveUnits in the DataPackageObject that pass the argument test function apply.
     *
     * @param dataObjectPackage the data object package
     * @param test              the test function
     * @return the fails count
     */
    public static List<ArchiveUnit> getAllCompliants(DataObjectPackage dataObjectPackage,
                                                     Function<ArchiveUnit, Boolean> test) {
        return (List<ArchiveUnit>)processAllCompliants(dataObjectPackage,test, x->x);
     }
}