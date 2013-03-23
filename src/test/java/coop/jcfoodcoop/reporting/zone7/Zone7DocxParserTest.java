package coop.jcfoodcoop.reporting.zone7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Test;

import static coop.jcfoodcoop.reporting.zone7.Zone7DocxParser.PER_PRICE_PATTERN;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author akrieg
 */
public class Zone7DocxParserTest {

    @Test
    public final void testParse() throws Exception {
        File outfile = File.createTempFile("frodo", "txt");
        Zone7DocxParser parser = new Zone7DocxParser();
        parser.parse("src/test/resources/zone7.docx", "zone7.csv");

        BufferedReader reader = new BufferedReader(new FileReader(outfile));
        String firstLine = reader.readLine();
        assertEquals("code,category,sub_category,sub_category2,manufacturer,product,short_description,size,case_units,each_size,unit_weight,case_weight,wholesale_price,price,sale_price,unit_price,retail_price,price_per_weight,is_priced_by_weight,valid_price,taxed,upc,origin,image_url,thumb_url,num_available,valid_order_increment,valid_split_increment,last_updated,last_updated_by,last_ordered,num_orders", firstLine);
        assertEquals("jfdsfsd", reader.readLine());
    }


    @Test
    public final void testPerDollarMatch() throws Exception {
        assertEquals(": ([^/]* / \\$\\d+\\.?\\d*)", PER_PRICE_PATTERN.pattern());
        String toMatch = "Fresh, Dry-Harvested Red: 8.5 lbs. / $25.50, $23.80 ea. for 3+";

        assertTrue(PER_PRICE_PATTERN.matcher(toMatch).find());

    }

    @Test
    public final void testDollarPerMatch() throws Exception {
        assertEquals("(\\$\\d+\\.?\\d* / [^\\.,~\\(]*)", Zone7DocxParser.PRICE_PER_PATTERN.pattern());


    }
}
