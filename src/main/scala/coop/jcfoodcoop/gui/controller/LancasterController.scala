package coop.jcfoodcoop.gui.controller

import javafx.fxml.FXML
import java.util.{Date, ResourceBundle}
import javafx.scene.control.{Button, TextField}
import javafx.event.ActionEvent
import javafx.stage.FileChooser
import javafx.scene.Node
import java.io.File
import java.text.SimpleDateFormat
import java.lang.Object
import coop.jcfoodcoop.productupdate.LancasterParser
import javax.swing.{SwingWorker, JOptionPane}
import java.awt.Desktop

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
  /*  todo: this currently does not work
           val xlsx = new FileChooser.ExtensionFilter("XLSX Files", "xlsx", "xsl")



            fileChooser.getExtensionFilters.add(xlsx)
  */

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


          val worker: SwingWorker[_, _] = new SwingWorker[AnyRef, AnyRef] {
              protected def doInBackground: AnyRef = {
                  try {
                      val parser: LancasterParser = new LancasterParser(invoiceFile, outFile)
                      parser.parse
                  }
                  catch {
                      case e: Exception => {
                          throw new RuntimeException("Exception occurred parsing " + outFile, e)
                      }
                  }
                  return null
              }

              protected override def done {
                  try {
                      get
                      Desktop.getDesktop.edit(outFile)
                  }
                  catch {
                      case e: Exception => {
                          e.printStackTrace
                      }
                  }
              }
          }



          worker.execute
      }

      @FXML
      def initialize() {
          require(fileTextField != null )
          require (openFileButton != null )
          require( runButton != null)
          runButton.disableProperty().set(true)


      }
}
