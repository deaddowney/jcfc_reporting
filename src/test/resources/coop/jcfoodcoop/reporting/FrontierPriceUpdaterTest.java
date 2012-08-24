package coop.jcfoodcoop.reporting;

import org.junit.Test;

/**
 * @author akrieg
 */
public class FrontierPriceUpdaterTest {

    @Test
    public final void testUpdate() throws Exception {
        final String sourceFilePath = "src/test/resources/FrontierExcelCatalog.csv";
        FrontierPriceUpdater updater = new FrontierPriceUpdater(
                sourceFilePath,
                "src/test/resources/FrontierCurrentPricesMarch.csv");
        updater.update(sourceFilePath.substring(0, sourceFilePath.length()-4)+".new.csv");
    }

}
