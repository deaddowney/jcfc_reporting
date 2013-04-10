package coop.jcfoodcoop.invoicing

import au.com.bytecode.opencsv.CSVWriter
import org.apache.poi.ss.usermodel.*

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
    private static final String DUE_DATE = new Date().plus(7).format("MM-dd-yyyy")


    private final def formatter = new DataFormatter()

    private Workbook sourceBook
    private CSVWriter csvOut

    ExcelParseContext(Workbook sourceBook, Writer out) {
        this.sourceBook = sourceBook
        this.csvOut = new CSVWriter(out)


    }

    public void parse(String vendor) {
        csvOut.writeNext(createHeader().toArray(new String[0]))
        for (int i = 0; i < sourceBook.numberOfSheets; i++) {
            def sheet = sourceBook.getSheetAt(i)
            def order = parseSheet(sheet, vendor)

            for (InvoiceItem item : order.items) {
                List<String> row = writeItemToRow(order, item)
                csvOut.writeNext(row.toArray(new String[row.size()]))
            }
            //Write an extra line for fees
            csvOut.writeNext(writeItemToRow(order, createFeeItem(order)).toArray(new String[0]))

        }
        csvOut.close()
    }

    InvoiceItem createFeeItem(Order o) {
        InvoiceItem item = new InvoiceItem()
        item.description = "PayPal Fee"
        item.qty = o.invoiceTotal
        item.rate = o.feeRate
        item.accountCode = 777 //PayPal Expense
        return item
    }

    public List<String> createHeader() {
        List<String> header = new LinkedList<String>();
        header.add("ContactName")
        header.add("EmailAddress")
        header.add("POAddressLine1")
        header.add("POAddressLine2")
        header.add("POAddressLine3")
        header.add("POAddressLine4")
        header.add("POCity")
        header.add("PORegion")
        header.add("POPostalCode")
        header.add("POCountry")
        header.add("InvoiceNumber")
        header.add("Reference")

        header.add("InvoiceDate")
        header.add("DueDate")
        header.add("Total")

        header.add("Description")
        header.add("Quantity")
        header.add("UnitAmount")
        header.add("Discount")
        header.add("AccountCode")
        header.add("TaxType") //TaxType
        header.add("TaxAmount")  //TaxAmount
        header.add("TrackingName1")//TrackingName1
        header.add("TrackingOption1")//TrackingOption1
        header.add("TrackingName2")//TrackingName2
        header.add("TrackingOption2")//TrackingOption2
        return header

    }

    private Order parseSheet(Sheet sheet, String vendor) {
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
        o.vendor = vendor
        o.id = prefix(vendor)+"-"+ID_PREFIX + "-" + idCounter++
        o.customer = name

        List<InvoiceItem> items = new LinkedList<InvoiceItem>()
        o.items = items
        while (rowIter.hasNext()) {
            try {
                Row row = rowIter.next()
                String val = formatter.formatCellValue(row.getCell(0))
                if (state == ParseState.BEGIN && "Code".equals(val)) {
                    state = ParseState.IN_ITEMS //We're now parsing the items in the invoice
                    continue
                }
                Matcher m = totQtyReceivedRegex.matcher(val)
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
                        o.feeRate = Double.valueOf(feeMatcher.group(1)) / 100.0
                    }


                } else if (row.getCell(0) != null) {
                    items.add(createInvoiceItem(row))

                }
            } catch (RuntimeException e) {
                throw new RuntimeException("Exception occurred processing " + name, e)

            }


        }
        return o
    }

    /**
     * @return first 3 characters of the vendor in uppercase, e.g. Lancaster -> LAN
     */
    def prefix(String vendor) {
        vendor.toUpperCase().substring(0, 3)
    }

    public InvoiceItem createInvoiceItem(Row row) {
        InvoiceItem.from(row, formatter)

    }

    public List<String> writeItemToRow(Order order, InvoiceItem item) {
        List<String> row = new LinkedList<String>()
        row.add(order.customer)
        row.add("") //Email
        row.add("") //POAddress1
        row.add("") //POAddress2
        row.add("") //POAddress3
        row.add("") //POAddress4
        row.add("") //POCity
        row.add("") //PORegion
        row.add("") //POPostalCode
        row.add("") //POCountry

        row.add(order.id) //InvoiceNumber
        row.add(order.vendor) // Reference
        row.add(DATE)  //InvoiceDate
        row.add(DUE_DATE) // Due Date
        row.add("") // Total
        row.add(item.description)  // Description
        row.add(String.valueOf(item.qty)) //Quantity
        row.add(String.valueOf(item.rate)) //UnitAmount
        row.add("") //Discount
        row.add(String.valueOf(item.accountCode)) //AccountCode
        row.add("Tax Exempt") //TaxType
        row.add("0")//TaxAmount
        row.add("")//TrackingName1
        row.add("")//TrackingOption1
        row.add("")//TrackingName2
        row.add("")//TrackingOption2
        return row

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
        int accountCode = 400 //Sales Account

        InvoiceItem() {}

        static InvoiceItem from(Row row, DataFormatter formatter) {
            InvoiceItem item = new InvoiceItem()
            item.product = formatter.formatCellValue(row.getCell(0)) //Code
            //Add manufacturer to description
            item.description = formatter.formatCellValue(row.getCell(6)) + " " + formatter.formatCellValue(row.getCell(7))
            item.qty = row.getCell(1).getNumericCellValue()
            String actualPriceString = formatter.formatCellValue(row.getCell(11))
            double actualPrice = 0.0
            //Sometime n/a shows up here
            if (!"Out- of- stock".equals(actualPriceString)) {
                actualPrice = Double.valueOf(actualPriceString)
            }
            item.rate = actualPrice
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
