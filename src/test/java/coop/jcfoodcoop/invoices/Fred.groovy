package coop.jcfoodcoop.invoices
import org.apache.poi.hssf.usermodel.HSSFWorkbook
/**
 * @author akrieg
 */
HSSFWorkbook book = new HSSFWorkbook(Fred.class.getResourceAsStream("/testInvoice.xls"))
ExcelParseContext ctx = new ExcelParseContext(book)
boolean totalsOnly = false
def excelSheet = ctx.parse(totalsOnly, "Tuesday Delivery")
excelSheet.write(new FileOutputStream("out.xls"))