package coop.jcfoodcoop.invoices

import coop.jcfoodcoop.invoicing.ExcelParseContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
/**
 * @author akrieg
 */
HSSFWorkbook book = new HSSFWorkbook(Fred.class.getResourceAsStream("/testInvoice.xls"))
Writer out = new FileWriter("out.csv")
ExcelParseContext ctx = new ExcelParseContext(book, out)
ctx.parse("Tuesday Delivery")
out.close()
