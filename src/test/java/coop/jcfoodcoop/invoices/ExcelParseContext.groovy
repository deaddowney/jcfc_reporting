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

    private static final String ID_PREFIX = new Date().format("yyyyMMdd")
    private int idCounter = 0;
    private static final String DATE = new Date().format("MM-dd-yyyy")


    private final def formatter = new DataFormatter()

    private Workbook sourceBook
    private Workbook outBook

    ExcelParseContext(Workbook sourceBook) {
        this.sourceBook = sourceBook
        this.outBook = new HSSFWorkbook()

    }

    public Workbook parse(boolean totalsOnly, String vendor) {
        Sheet outsheet = outBook.createSheet("invoices")
        int rowCounter = 0 //number of rows that have been written
        Row outRow = outsheet.createRow(rowCounter++)
        createHeader(outRow, totalsOnly)

        for (int i = 0; i < sourceBook.numberOfSheets; i++) {
            def sheet = sourceBook.getSheetAt(i)
            def order = parseSheet(sheet)
            order.vendor = vendor
            if (totalsOnly) {
                outRow =  outsheet.createRow(rowCounter++)
                writeOrderToRow(order, "Lancaster", outRow)
            } else {

                for (InvoiceItem item : order.items) {
                    outRow = outsheet.createRow(rowCounter++)
                    writeItemToRow(outRow, order, item)
                }
                //Write an extra line for fees
                writeFeeRow(order, outsheet.createRow(rowCounter++) )

            }


        }
        return outBook
    }



    public void createHeader(Row outRow, boolean totalsOnly) {
        int counter = 0
        outRow.createCell(counter++).setCellValue("ID")
        outRow.createCell(counter++).setCellValue("Date")
        outRow.createCell(counter++).setCellValue("Customer")
        outRow.createCell(counter++).setCellValue("Source")
        outRow.createCell(counter++).setCellValue("Product")
        outRow.createCell(counter++).setCellValue("Description")
        outRow.createCell(counter++).setCellValue("Qty")
        if (totalsOnly) {
            outRow.createCell(counter++).setCellValue("Invoice Total")
            outRow.createCell(counter++).setCellValue("Fee Pct")
            outRow.createCell(counter++).setCellValue("Fee Total")

        } else {
            outRow.createCell(counter++).setCellValue("Rate")
        }
        outRow.createCell(counter).setCellValue("Total")
    }

    private Order parseSheet(Sheet sheet) {
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
        Order o = new Order()
        o.id =  ID_PREFIX + "-" + idCounter++
        o.customer = name

        List<InvoiceItem> items = new LinkedList<InvoiceItem>()
        o.items = items
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
                def totalRow = rowIter.next()
                def total = totalRow.getCell(1).getNumericCellValue()
                System.out.println("Name:${name}: invoice = ${invoice}, fee ${fee}, total = ${total}")

                o.total = total
                o.fees = feeRow.getCell(1).getNumericCellValue()
                o.invoiceTotal = invoiceRow.getCell(1).getNumericCellValue()
                Matcher feeMatcher = procFeeRegex.matcher(feeRow.getCell(0).getStringCellValue())

                if (feeMatcher.find()) {
                    o.feeRate = Double.valueOf(feeMatcher.group(1))/100.0
                }


            } else if (row.getCell(0) != null) {
                items.add( createInvoiceItem(row))

            }


            }
        return o
        }

    public InvoiceItem createInvoiceItem( Row row) {
        InvoiceItem.from(row, formatter)

    }

    public void writeItemToRow(Row outRow, Order order, InvoiceItem item) {
        int colCounter = 0
        outRow.createCell(colCounter++).setCellValue(order.id)
        outRow.createCell(colCounter++).setCellValue(DATE)
        outRow.createCell(colCounter++).setCellValue(order.customer)
        outRow.createCell(colCounter++).setCellValue(order.vendor)
        outRow.createCell(colCounter++).setCellValue(item.product)
        outRow.createCell(colCounter++).setCellValue(item.description)
        outRow.createCell(colCounter++).setCellValue(item.qty)
        outRow.createCell(colCounter++).setCellValue(item.rate)
        outRow.createCell(colCounter).setCellValue(item.total) //total

    }

    public void writeFeeRow(Order o, Row outRow) {
        int colCounter = 0
        outRow.createCell(colCounter++).setCellValue(o.id)
        outRow.createCell(colCounter++).setCellValue(DATE)
        outRow.createCell(colCounter++).setCellValue(o.customer)
        outRow.createCell(colCounter++).setCellValue(o.vendor)

        outRow.createCell(colCounter++).setCellValue("Paypal")
        outRow.createCell(colCounter++).setCellValue("Paypal Fee")
        outRow.createCell(colCounter++).setCellValue(o.invoiceTotal)
        outRow.createCell(colCounter++).setCellValue(o.feeRate)
        outRow.createCell(colCounter++).setCellValue(o.fees)
    }

    public void writeOrderToRow(Order o, String product, Row outRow) {
        int colCounter = 0
        outRow.createCell(colCounter++).setCellValue(o.id)
        outRow.createCell(colCounter++).setCellValue(DATE)
        outRow.createCell(colCounter++).setCellValue(o.customer)
        outRow.createCell(colCounter++).setCellValue(o.vendor)

        outRow.createCell(colCounter++).setCellValue(product)
        outRow.createCell(colCounter++).setCellValue("sale")
        outRow.createCell(colCounter++).setCellValue(1)
        outRow.createCell(colCounter++).setCellValue(o.invoiceTotal)
        outRow.createCell(colCounter++).setCellValue(o.feeRate)
        outRow.createCell(colCounter++).setCellValue(o.fees)

        outRow.createCell(colCounter).setCellValue(o.total)
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

    class Order {
        String customer
        List<InvoiceItem> items
        double fees
        double feeRate
        double invoiceTotal
        double total

        String id
        String vendor
    }
}
