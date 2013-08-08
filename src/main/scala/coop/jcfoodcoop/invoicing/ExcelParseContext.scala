package coop.jcfoodcoop.invoicing

import org.apache.poi.ss.usermodel._
import au.com.bytecode.opencsv.CSVWriter
import java.io.Writer
import org.joda.time.format.DateTimeFormat
import org.scala_tools.time.Imports._
import scala.collection.mutable.ListBuffer

/**
 * @author akrieg
 */
object ExcelParseContext {
    /** We search for this to find the summary row **/
    val totQtyReceivedRegex = """Total quantity received: (\d+\.\d+)""".r
    val procFeeRegex = """Processing fee \((\d+\.\d+)%\)""".r

    val now = new DateTime()

    val ID_PREFIX = DateTimeFormat.forPattern("yyyyMMdd").print(now)

    val DATE = DateTimeFormat.forPattern("MM-dd-yyyy").print(now)
    val DUE_DATE = DateTimeFormat.forPattern("MM-dd-yyyy").print(now + 7.days)


    def getName(nameCell: Cell) = {
        var name = nameCell.getStringCellValue.trim()
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


    def parse(vendor: String)  {
        csvOut.writeNext(createHeader().toArray)
        for (i <- 0 until sourceBook.getNumberOfSheets) {
            val sheet = sourceBook.getSheetAt(i)
            val order = parseSheet(sheet, vendor)

            for (item <- order.items) {
                val row = writeItemToRow(order, item)
                csvOut.writeNext(row.toArray)
            }
            if (order.fees > 0) {
                //Write an extra line for fees
                csvOut.writeNext(writeItemToRow(order, createFeeItem(order)).toArray )
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
         * Lisa Clarke
         Code	Qty	Split units	Category	Subcategory	2<sup>nd</sup> Subcategory	Manufacturer	Description	Size	Comment	Order price	Actual price	Actual unit price	Item total
         13352I	0.5	6 of 12	Grocery	Juices and Nectars		Amy and Brian's	Natural Coconut Juice Pulp Free	12 (17.5 oz) per case		20.87	20.87	1.74/(17.5 oz) per case	10.44
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
            if (state == ParseState.BEGIN && "Code".equals(value)) {
                state = ParseState.IN_ITEMS //We're now parsing the items in the invoice
            } else {
                val m = ExcelParseContext.totQtyReceivedRegex.findFirstMatchIn(value)
                if (state == ParseState.IN_ITEMS && m.isDefined) {
                    state = ParseState.IN_TOTAL


                    val invoiceRow = rowIter.next()
                    o.invoiceTotal = invoiceRow.getCell(1).getNumericCellValue

                    //The next row contains either the fee, if the fee has been assigned, or the invoice total
                    val feeRow = rowIter.next() //
                    val feeDescription = feeRow.getCell(0).getStringCellValue
                    if (feeDescription.trim().startsWith("Processing fee")) {
                        val totalRow = rowIter.next()
                        val total = totalRow.getCell(1).getNumericCellValue

                        o.total = total
                        o.fees = feeRow.getCell(1).getNumericCellValue
                        val feeMatcher = ExcelParseContext.procFeeRegex.findFirstMatchIn(feeRow.getCell(0).getStringCellValue)

                        if (feeMatcher.isDefined) {
                            o.feeRate = feeMatcher.get.group(1).toDouble / 100.0
                        }


                    } else {
                        o.feeRate = 0.035
                        o.fees = o.invoiceTotal * o.feeRate
                        o.total = o.invoiceTotal + o.fees

                    }


                } else if (row.getCell(1) != null && state == ParseState.IN_ITEMS) {
                    items += createInvoiceItem(row)

                }
            }

        }
        o
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
        item.product = formatter.formatCellValue(row.getCell(0)) //Code
        //Add manufacturer to description
        item.description = formatter.formatCellValue(row.getCell(6)) + " " + formatter.formatCellValue(row.getCell(7))
        item.qty = row.getCell(1).getNumericCellValue
        val actualPriceString = formatter.formatCellValue(row.getCell(11))
        var actualPrice = 0.0
        //Sometime n/a shows up here
        if (!"Out- of- stock".equals(actualPriceString) && actualPriceString!=null && !actualPriceString.equals("")) {
            actualPrice = actualPriceString.toDouble
        }
        item.rate = actualPrice
        item.total = row.getCell(13).getNumericCellValue //Item total
        //Food club has frequent rounding issues.  We do the best we can to stay in sync with
        //their totals
        if (Math.abs((item.total * 100) - (item.rate * item.qty * 100)) >= 1) {
            item.rate = item.total / item.qty
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