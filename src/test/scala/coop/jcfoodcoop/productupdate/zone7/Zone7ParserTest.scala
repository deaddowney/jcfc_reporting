package coop.jcfoodcoop.productupdate.zone7

import org.junit.{After, Before, Test}
import au.com.bytecode.opencsv.CSVReader
import java.io.FileReader
import junit.framework.Assert

class Zone7ParserTest {
    val file = "src/test/resources/zone7/out.csv"
    var reader :CSVReader = null

    @Before
    def setup {
        reader = new CSVReader(new FileReader(file))
    }

    @After
    def teardown {
        reader.close()
    }

    @Test
    def testParse = {

        val header = Zone7Parser.readHeader(reader)
        Assert.assertEquals(1, header.product)
    }

    @Test
    def testRun = {
        val stuff = Zone7Parser.parse(file)
        for (thing <- stuff) {
            println (thing)
        }
    }
}