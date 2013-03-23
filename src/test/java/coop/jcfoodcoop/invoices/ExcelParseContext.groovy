package coop.jcfoodcoop.invoices

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * From
 *  Code	Qty	Split units	Category	Subcategory	2<sup>nd</sup> Subcategory	Manufacturer	Description	Size	Comment	Order price	Actual price	Actual unit price	Item total
 *  15903I	0.083	1 of 12	Grocery	Vinegar		Bragg Live Foods	Organic Apple Cider Vinegar	12 (16 oz) per case		32.02	32.02	2.67/(16 oz) per case	2.67
 * Need to get Product/Service, Description, Qty, Rate, Amount
 *
 * @author akrieg
 */
class ExcelParseContext {

    /** We search for this to find the summary row **/
    private static final Pattern totQtyReceivedRegex = ~/Total quantity received: (\d+\.\d+)/
    private static final Pattern procFeeRegex = ~/Processing fee \((\d+\.\d+)%\)/

    private static final String ID_PREFIX = new Date().format("yyyyMMdd-HHmmss")
    private int idCounter = 0;
    private static final String DATE = new Date().format("MM-dd-yyyy")


    private final def formatter = new DataFormatter()

    private Workbook sourceBook
    private Workbook outBook
    int rowCounter = 0

    ExcelParseContext(Workbook sourceBook) {
        this.sourceBook = sourceBook
        this.outBook = new HSSFWorkbook()

    }

    public Workbook parse() {
        Sheet outsheet = outBook.createSheet("invoices")
        Row outRow = outsheet.createRow(rowCounter++)
        int counter = 0
        outRow.createCell(counter++).setCellValue("ID")
        outRow.createCell(counter++).setCellValue("Date")
        outRow.createCell(counter++).setCellValue("Customer")
        outRow.createCell(counter++).setCellValue("Product")
        outRow.createCell(counter++).setCellValue("Description")
        outRow.createCell(counter++).setCellValue("Qty")
        outRow.createCell(counter++).setCellValue("Rate")
        outRow.createCell(counter).setCellValue("Total")

        for (int i = 0; i < sourceBook.numberOfSheets; i++) {
            def sheet = sourceBook.getSheetAt(i)
            parseSheet(sheet, outsheet)

        }
        return outBook
    }

    def parseSheet(Sheet sheet, Sheet outsheet) {
        //The spreadsheet format looks like:
        /**
         * Lisa Clarke
         Code	Qty	Split units	Category	Subcategory	2<sup>nd</sup> Subcategory	Manufacturer	Description	Size	Comment	Order price	Actual price	Actual unit price	Item total
         13352I	0.5	6 of 12	Grocery	Juices and Nectars		Amy and Brian's	Natural Coconut Juice Pulp Free	12 (17.5 oz) per case		20.87	20.87	1.74/(17.5 oz) per case	10.44
         */


        def rowIter = sheet.iterator()
        def row1 = rowIter.next()
        def nameCell = row1.getCell(0)
        String name = getName(nameCell)

        def state = ParseState.BEGIN
        while (rowIter.hasNext()) {
            Row row = rowIter.next()
            String val = formatter.formatCellValue(row.getCell(0))
            if (state == ParseState.BEGIN && "Code".equals(val)) {
                state = ParseState.IN_ITEMS //We're now parsing the items in the invoice
                continue
            }
            Matcher m  = totQtyReceivedRegex.matcher(val)
            if (state == ParseState.IN_ITEMS && m.find()) {
                state = ParseState.IN_TOTAL


                def invoiceRow = rowIter.next()
                def invoice = formatter.formatCellValue(invoiceRow.getCell(1))
                def feeRow = rowIter.next()
                def fee = formatter.formatCellValue(feeRow.getCell(1))
                def total = formatter.formatCellValue(rowIter.next().getCell(1))
                System.out.println("Name:${name}: invoice = ${invoice}, fee ${fee}, total = ${total}")

                InvoiceItem item = new InvoiceItem()
                item.product = "Fee"
                item.description = feeRow.getCell(0)
                Matcher feeMatcher = procFeeRegex.matcher(item.description)
                if (feeMatcher.find()) {
                    item.qty = Double.valueOf(feeMatcher.group(1))/100.0
                }

                item.rate = invoiceRow.getCell(1).getNumericCellValue()
                item.total = feeRow.getCell(1).getNumericCellValue()
                Row outRow = outsheet.createRow(rowCounter++)
                writeItemToRow(outRow, name, item)

            } else if (row.getCell(0) != null) {
                rowCounter = writeRow(outsheet, rowCounter, row, name)

            }


            }
        }

    public int writeRow(Sheet outsheet, int rowCounter, Row row, String name) {
        Row outRow = outsheet.createRow(rowCounter++)
        InvoiceItem item = InvoiceItem.from(row, formatter)
        writeItemToRow(outRow, name, item)
        rowCounter
    }

    public void writeItemToRow(Row outRow, String name, InvoiceItem item) {
        int colCounter = 0
        def id = ID_PREFIX +"-"+idCounter++
        outRow.createCell(colCounter++).setCellValue(id)
        outRow.createCell(colCounter++).setCellValue(DATE)
        outRow.createCell(colCounter++).setCellValue(name)
        outRow.createCell(colCounter++).setCellValue(item.product)
        outRow.createCell(colCounter++).setCellValue(item.description)
        outRow.createCell(colCounter++).setCellValue(item.qty)
        outRow.createCell(colCounter++).setCellValue(item.rate)
        outRow.createCell(colCounter).setCellValue(item.total)
    }



    public static String getName(Cell nameCell) {
        def name = nameCell.getStringCellValue().trim()
        //put last name at end.
        def commaIndex = name.indexOf(",")
        if (commaIndex > 0) {
            name = name.substring(commaIndex + 1).trim() + " " + name.substring(0, commaIndex).trim()


        }
        name
    }

    enum ParseState {
        BEGIN,  //Initial State
        IN_ITEMS, //Parsing the various items
        IN_TOTAL //Parsing the total section
    }

    static class InvoiceItem {

        String product
        String description
        double qty
        double rate
        double total

        InvoiceItem() {}
        static InvoiceItem from(Row row, DataFormatter formatter) {
            InvoiceItem item = new InvoiceItem()
            item.product = formatter.formatCellValue(row.getCell(0)) //Code
            //Add manufacturer to description
            item.description = formatter.formatCellValue(row.getCell(6))+" "+formatter.formatCellValue(row.getCell(7))
            item.qty = row.getCell(1).getNumericCellValue()
            item.rate = row.getCell(11).getNumericCellValue() //Actual Price
            item.total = row.getCell(13).getNumericCellValue() //Item total
            item
        }

        public String toString() {
            return "product=${product} description='${description}' qty = '${qty}' rate='${rate} total=${total}"
        }

    }
}
