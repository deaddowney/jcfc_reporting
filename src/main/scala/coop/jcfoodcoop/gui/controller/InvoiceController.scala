package coop.jcfoodcoop.gui.controller

import javafx.fxml.FXML
import java.util.{Date, ResourceBundle}
import java.net.URL
import javafx.scene.control.{ChoiceBox, Button, TextField}
import javafx.event.ActionEvent
import javafx.collections.FXCollections
import javafx.stage.FileChooser
import javafx.scene.Node
import java.text.SimpleDateFormat
import java.io.{FileWriter, FileInputStream, File}
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import coop.jcfoodcoop.invoicing.ExcelParseContext
import java.awt.Desktop
import javafx.concurrent.Task
import org.controlsfx.dialog.Dialogs
import java.util.concurrent.Executors
import javafx.scene.image.{Image, ImageView}
import javafx.beans.value.{ObservableValue, ChangeListener}
import scala.collection.{immutable, mutable}

/**
 * @author akrieg
 */
class InvoiceController {

    @FXML
    private var resources: ResourceBundle = null

    @FXML
    private var location: URL = null

    @FXML
    private var fileField: TextField = null

    @FXML
    private var launchFileExpButton: Button = null

    @FXML
    private var runButton: Button = null

    private var lastDirectory: String = System.getProperty("user.dir")
    @FXML
    private var supplierDropDown: ChoiceBox[String] = null

    @FXML
    var imageBox2: ImageView = null

    @FXML
    def onFileOpen(event: ActionEvent)= {
        val fileChooser = new FileChooser()
        val xlsx = new FileChooser.ExtensionFilter("XLS Files", "*.xls")

        fileChooser.getExtensionFilters.add(xlsx)


        //Show save file dialog
        val window = event.getTarget.asInstanceOf[Node].getScene.getWindow
        val file = fileChooser.showOpenDialog(window)
        if (file != null) {
            fileField.setText(file.getPath)
            lastDirectory = file.getParent
            runButton.disableProperty().set(false)
        }

    }

    @FXML
    def onRunPushed(event: ActionEvent)= {
        val invoiceFile: String = fileField.getText


        val supplier = supplierDropDown.getValue


        val format = new SimpleDateFormat("MM-dd-yyy")


        val outFile: File = new File(lastDirectory, supplier + "-" + format.format(new Date) + ".csv")

        val task = new Task[Unit]() {
            def call(): Unit = {
                val book: HSSFWorkbook = new HSSFWorkbook(new FileInputStream(invoiceFile))
                val pc: ExcelParseContext = new ExcelParseContext(book, new FileWriter(outFile))
                pc.parse(supplier)
            }

            override def succeeded(): Unit = {
                super.succeeded()
                Desktop.getDesktop.edit(outFile)

            }

            override def failed(): Unit = {
                super.failed()
                Dialogs.create().
                    message("Failed to parse " + invoiceFile).
                    showException(this.getException)
            }
        }

        Executors.newSingleThreadExecutor().execute(task)


    }

    @FXML
    def initialize() {
        if(imageBox2==null) { throw new IllegalArgumentException("imageBox was null")}

        val supplierMap = Map(
            "Tuesday Suppliers" -> new Image("/images/Regional.jpeg"),
            "Lancaster Farm Fresh" ->new Image("/images/lancaster.jpg"),
            "Zone 7" -> new Image("/images/Zone7.jpeg"))

        supplierDropDown.setItems(FXCollections.
            observableArrayList(
            "Tuesday Suppliers",
            "Lancaster Farm Fresh",
            "Zone 7")
        )
        supplierDropDown.getSelectionModel.select(0)
        supplierDropDown.valueProperty().addListener(new ChangeListener[String] {
            def changed(p1: ObservableValue[_ <: String], p2: String, p3: String) {
                if (p3!=null && supplierMap(p3) !=null) {
                    imageBox2.setImage(supplierMap(p3))
                }
            }
        })

        runButton.disableProperty().set(true)

        launchFileExpButton.setOnAction(new javafx.event.EventHandler[ActionEvent]{
            def handle(p1: ActionEvent) {
                onFileOpen(p1)
            }
        })


    }


}
