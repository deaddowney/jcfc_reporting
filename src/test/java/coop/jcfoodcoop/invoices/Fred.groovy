package coop.jcfoodcoop.invoices
import org.apache.poi.hssf.usermodel.HSSFWorkbook
/**
 * @author akrieg
 */
HSSFWorkbook book = new HSSFWorkbook(Fred.class.getResourceAsStream("/testInvoice.xls"))
ExcelParseContext ctx = new ExcelParseContext(book)
def excelSheet = ctx.parse()
excelSheet.write(new FileOutputStream("out.xls"))