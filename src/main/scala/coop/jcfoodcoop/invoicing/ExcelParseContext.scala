package coop.jcfoodcoop.invoicing

import java.io.Writer

import au.com.bytecode.opencsv.CSVWriter
import org.apache.poi.ss.usermodel._
import org.joda.time.format.DateTimeFormat
import org.scala_tools.time.Imports._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
 * @author akrieg
 */
object ExcelParseContext {
    /** We search for this to find the summary row **/
    val totQtyReceivedRegex = """Total quantity received: (\d+\.\d+)""".r
    val invoiceTotalRegex = """Invoice total""".r
    val jcfcMarkupRegex = """JCFC \(\d\d\.\d\d%\)""".r
    val procFeeRegex = """Processing [fF]ee \((\d+\.\d+)%\)""".r
    val totalRegex = """Total""".r

    val splitRegex = """(\d+) of (\d+)""".r //2 of 12

    val now = new DateTime()

    val ID_PREFIX = DateTimeFormat.forPattern("yyyyMMdd").print(now)

    val DATE = DateTimeFormat.forPattern("MM-dd-yyyy").print(now)
    val DUE_DATE = DateTimeFormat.forPattern("MM-dd-yyyy").print(now + 1.days)


    def getName(nameCell: Cell) = {
        //Weird bug in Excel output where the name has a bunch of html shit at the end.  Strip it out
        def stripHtmlSpan(name: String): String = {
            if (name.contains("<span class")) {
                name.substring(0, name.indexOf("<span"))
            } else {
                name
            }
        }

        var name = stripHtmlSpan(nameCell.getStringCellValue.trim())
        //put last name at end.
        val commaIndex = name.indexOf(",")
        if (commaIndex > 0) {
            name = name.substring(commaIndex + 1).trim() + " " + name.substring(0, commaIndex).trim()
        }

        name
    }

}

class ExcelParseContext(val sourceBook: Workbook, out: Writer) {
    var idCounter = 0
    val csvOut = new CSVWriter(out)


    val formatter = new DataFormatter()


    def parse(vendor: String) {
        csvOut.writeNext(createHeader().toArray)
        for (i <- 0 until sourceBook.getNumberOfSheets) {
            val sheet = sourceBook.getSheetAt(i)
            val order = parseSheet(sheet, vendor)

            for (item <- order.items) {
                val row = writeItemToRow(order, item)
                csvOut.writeNext(row.toArray)
            }
            if (order.markup > 0) {
                csvOut.writeNext(writeItemToRow(order, createMarkupItem(order)).toArray)
            }
            if (order.fees > 0) {
                //Write an extra line for fees
                csvOut.writeNext(writeItemToRow(order, createFeeItem(order)).toArray)
            }

        }
        csvOut.close()
    }

    def createFeeItem(o: Order) = {
        val item = new InvoiceItem()
        item.description = "PayPal Fee"
        item.qty = 1
        item.rate = o.fees
        item.accountCode = 777 //PayPal Expense
        item
    }

    def createMarkupItem(o: Order) = {
        val item = new InvoiceItem()
        item.description = "25% Markup"
        item.qty = 1
        item.rate = o.markup
        item.accountCode = 400 //Sales
        item
    }

    def createHeader(): List[String] = {
        val header = new ListBuffer[String]()
        header += "ContactName"
        header += "EmailAddress"
        header += "POAddressLine1"
        header += "POAddressLine2"
        header += "POAddressLine3"
        header += "POAddressLine4"
        header += "POCity"
        header += "PORegion"
        header += "POPostalCode"
        header += "POCountry"
        header += "InvoiceNumber"
        header += "Reference"

        header += "InvoiceDate"
        header += "DueDate"
        header += "Total"

        header += "Description"
        header += "Quantity"
        header += "UnitAmount"
        header += "Discount"
        header += "AccountCode"
        header += "TaxType" //TaxType
        header += "TaxAmount" //TaxAmount
        header += "TrackingName1" //TrackingName1
        header += "TrackingOption1" //TrackingOption1
        header += "TrackingName2" //TrackingName2
        header += "TrackingOption2" //TrackingOption2
        header.toList

    }

    def parseSheet(sheet: Sheet, vendor: String): Order = {
        //The spreadsheet format looks like:
        /**
         * Shah, Lucille
         Full name	Code	Qty	Split units	Category	Subcategory	2<sup>nd</sup> Subcategory	Manufacturer	Description	Size	Comment	Order price	Actual price	Actual unit price	Item total
         Shah, Lucille	13352I	0.5	6 of 12	Grocery	Juices and Nectars		Amy and Brian's	Natural Coconut Juice Pulp Free	12 (17.5 oz) per case		20.87	20.87	1.74/(17.5 oz) per case	10.44
         */


        val rowIter = sheet.iterator()
        val row1 = rowIter.next()
        val nameCell = row1.getCell(0)
        val name = ExcelParseContext.getName(nameCell)

        var state = ParseState.BEGIN
        val o = new Order()
        o.vendor = vendor
        o.id = prefix(vendor) + "-" + ExcelParseContext.ID_PREFIX + "-" + idCounter
        idCounter += 1
        o.customer = name

        val items = new ListBuffer[InvoiceItem]()
        o.items = items
        while (rowIter.hasNext) {
            val row = rowIter.next()
            val value = formatter.formatCellValue(row.getCell(0))
            if (state == ParseState.BEGIN && "Full name".equals(value)) {
                state = ParseState.IN_ITEMS //We're now parsing the items in the invoice
            } else {
                val m = ExcelParseContext.totQtyReceivedRegex.findFirstMatchIn(value)
                if (state == ParseState.IN_ITEMS && m.isDefined) {
                    state = ParseState.IN_TOTAL

                    val totalMap = processTotalRows(rowIter)

                    o.invoiceTotal = totalMap.get(ExcelParseContext.invoiceTotalRegex).get
                    o.feeRate = 0.25
                    o.fees = totalMap.getOrElse(ExcelParseContext.procFeeRegex, o.feeRate * o.invoiceTotal)
                    o.markup = totalMap.get(ExcelParseContext.jcfcMarkupRegex).get
                    o.total = totalMap.get(ExcelParseContext.totalRegex).get

                } else if (row.getCell(1) != null && state == ParseState.IN_ITEMS) {
                    items += createInvoiceItem(row)

                }
            }

        }
        o
    }

    /**
     *
     * @param rowIter source of rows in the excel spreadsheet
     * @return map of Regex to Values for further inspection
     */
    def processTotalRows(rowIter: java.util.Iterator[Row]) = {
        val itemMap = mutable.Map[Regex, Double]()
        val patterns = List(ExcelParseContext.invoiceTotalRegex, ExcelParseContext.jcfcMarkupRegex, ExcelParseContext.procFeeRegex, ExcelParseContext.totQtyReceivedRegex, ExcelParseContext.totalRegex)
        while (rowIter.hasNext) {
            val row = rowIter.next
            val description = row.getCell(0).getStringCellValue.trim
            val dollarVal = row.getCell(1).getNumericCellValue
            val matchedP = patterns.find(p => p.findFirstMatchIn(description).isDefined)
            if (matchedP.isDefined) {
                itemMap.put(matchedP.get, dollarVal)
            }
        }
        itemMap
    }

    /**
     * @return first 3 characters of the vendor in uppercase, e.g. Lancaster -> LAN
     */
    def prefix(vendor: String) = {
        vendor.toUpperCase.substring(0, 3)
    }

    def createInvoiceItem(row: Row): InvoiceItem = {
        InvoiceItem.from(row, formatter)

    }

    def writeItemToRow(order: Order, item: InvoiceItem): List[String] = {
        val row = new ListBuffer[String]()
        row += order.customer
        row += "" //Email
        row += "" //POAddress1
        row += "" //POAddress2
        row += "" //POAddress3
        row += "" //POAddress4
        row += "" //POCity
        row += "" //PORegion
        row += "" //POPostalCode
        row += "" //POCountry

        row += order.id //InvoiceNumber
        row += order.vendor // Reference
        row += ExcelParseContext.DATE //InvoiceDate
        row += ExcelParseContext.DUE_DATE // Due Date
        row += "" // Total
        row += item.description // Description
        row += String.valueOf(item.qty) //Quantity
        row += String.valueOf(item.rate) //UnitAmount
        row += "" //Discount
        row += String.valueOf(item.accountCode) //AccountCode
        row += "Tax Exempt" //TaxType
        row += "0" //TaxAmount
        row += "" //TrackingName1
        row += "" //TrackingOption1
        row += "" //TrackingName2
        row += "" //TrackingOption2
        row.toList

    }


}


object InvoiceItem {

    def from(row: Row, formatter: DataFormatter): InvoiceItem = {
        val item = new InvoiceItem()
        val CODE_INDEX: Int = 1
        val QTY_INDEX: Int = 2
        val SPLIT_INDEX: Int = 3

        val MANUFACTURER_INDEX: Int = 7
        val DESCRIPTION_INDEX: Int = 8
        val ACTUAL_PX_INDEX: Int = 12
        val ITEM_TOTAL_INDEX: Int = 14

        item.product = formatter.formatCellValue(row.getCell(CODE_INDEX)) //Code
        //Add manufacturer to description
        item.description = formatter.formatCellValue(row.getCell(MANUFACTURER_INDEX)) + " " +
            formatter.formatCellValue(row.getCell(DESCRIPTION_INDEX))
        item.qty = row.getCell(QTY_INDEX).getNumericCellValue
        val actualPriceString = formatter.formatCellValue(row.getCell(ACTUAL_PX_INDEX))
        var actualPrice = 0.0
        var splitQty = 0

        val cell: Cell = row.getCell(SPLIT_INDEX)
        if (cell != null) {
            val value: String = cell.getStringCellValue
            val splitMatcher = ExcelParseContext.splitRegex.findFirstMatchIn(value)

            if (splitMatcher.isDefined) {
                splitQty = splitMatcher.get.group(1).toInt
            }
        }
        //Sometime n/a shows up here
        if (!"Out- of- stock".equals(actualPriceString) && actualPriceString != null && !actualPriceString.equals("")) {
            if ("n/a" == actualPriceString) {
                actualPrice = 0.0
            } else {
                actualPrice = actualPriceString.toDouble
            }
        }
        val itemTotCell: Cell = row.getCell(ITEM_TOTAL_INDEX)
        item.total = if (itemTotCell.getCellType == org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC) {
            itemTotCell.getNumericCellValue
        } else {
            0
        }

        if (actualPrice == 0.0 && splitQty > 0) {
            item.rate = item.total / splitQty
        } else {
            item.rate = actualPrice
        }

        //Food club has frequent rounding issues.  We do the best we can to stay in sync with
        //their totals
        if (splitQty == 0) {
            if (Math.abs((item.total * 100) - (item.rate * item.qty * 100)) >= 1) {
                item.rate = item.total / item.qty
            }
        }
        item
    }

}

class InvoiceItem {

    var product: String = ""
    var description: String = ""
    var qty: Double = 0.0
    var rate: Double = 0.0
    var total: Double = 0.0
    var accountCode = 400 //Sales Account


    override def toString: String = {
        "product=${product} description='${description}' qty = '${qty}' rate='${rate} total=${total}"
    }

}

class Order {
    var customer: String = ""
    var items: ListBuffer[InvoiceItem] = ListBuffer.empty
    var fees: Double = 0.0
    var feeRate: Double = 0.0
    var invoiceTotal: Double = 0.0
    var markup: Double = 0.0
    var total: Double = 0.0


    var id: String = ""
    var vendor: String = ""
}

object ParseState extends scala.Enumeration {

    type ParseState = Value

    val BEGIN, //Initial State
    IN_ITEMS, //Parsing the various items
    IN_TOTAL = Value //Parsing the total section
}