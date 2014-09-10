package csv

import au.com.bytecode.opencsv.CSVReader
import coop.jcfoodcoop.productupdate.zone7.Zone7Parser.Line

/**
 * @author akrieg
 */
object Read {

    type Lines = Stream[Line]

    def streamRows(reader: CSVReader): Lines = {
        def streamRows:Lines = {
            val row = reader.readNext()
            if (row == null) {
                reader.close()
                Stream.empty
            } else {
                row #:: streamRows
            }
        }
        streamRows
    }

}
