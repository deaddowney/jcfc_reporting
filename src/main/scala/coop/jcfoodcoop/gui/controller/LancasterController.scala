package coop.jcfoodcoop.gui.controller

import javafx.fxml.FXML
import java.util.{Date, ResourceBundle}
import javafx.scene.control.{Button, TextField}
import javafx.event.ActionEvent
import javafx.stage.FileChooser
import javafx.scene.Node
import java.io.{FileWriter, FileInputStream, File}
import java.text.SimpleDateFormat
import java.lang.Object
import coop.jcfoodcoop.productupdate.LancasterParser
import javax.swing.{SwingWorker, JOptionPane}
import java.awt.Desktop
import javafx.concurrent.Task
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import coop.jcfoodcoop.invoicing.ExcelParseContext
import org.controlsfx.dialog.Dialogs
import java.util.concurrent.Executors

/**
 * @author akrieg
 */
class LancasterController {
      @FXML
      private var resources:ResourceBundle=null

      @FXML
      private var location:java.net.URL=null

      @FXML
      private var fileTextField :TextField = null

      @FXML
      private var openFileButton: Button = null

      @FXML
      private var runButton:Button = null

    private var lastDirectory = System.getProperty("user.dir")


      @FXML
      def onFileOpen(event:ActionEvent) {
          val fileChooser = new FileChooser()
          val xlsx = new FileChooser.ExtensionFilter("Word Files", "*.docx", "*.doc")



            fileChooser.getExtensionFilters.add(xlsx)

          //Show save file dialog
          val window = event.getTarget.asInstanceOf[Node].getScene.getWindow
          val file = fileChooser.showOpenDialog(window)
          if (file != null) {
              fileTextField.setText(file.getPath)
              lastDirectory = file.getParent
              runButton.disableProperty().set(false)
          }
      }

      @FXML
      def onRun(event:ActionEvent) {
          val invoiceFile: File = new File(fileTextField.getText)


          val format: SimpleDateFormat = new SimpleDateFormat("MM-dd-yyy")


          val outFile: File = new File(invoiceFile.getParent, "Lancaster-" + format.format(new Date) + ".csv")

          val task = new Task[Unit]() {
              def call(): Unit = {
                  val parser: LancasterParser = new LancasterParser(invoiceFile, outFile)
                  parser.parse
              }

              override def succeeded():Unit ={
                  super.succeeded()
                  Desktop.getDesktop.edit(outFile)

              }

              override def failed():Unit = {
                  super.failed()
                  Dialogs.create().
                      message("Failed to parse "+invoiceFile).
                      showException(this.getException)
              }
          }

          Executors.newSingleThreadExecutor().execute(task)

      }

      @FXML
      def initialize() {
          require(fileTextField != null )
          require (openFileButton != null )
          require( runButton != null)
          runButton.disableProperty().set(true)


      }
}
