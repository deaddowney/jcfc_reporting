package coop.jcfoodcoop.reporting

import groovy.swing.SwingBuilder
import java.awt.Color
import java.awt.GradientPaint
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.CategoryAxis
import org.jfree.chart.axis.CategoryLabelPositions
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.data.category.DefaultCategoryDataset
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import javax.swing.JFrame

/**
 * NewMemberChart.groovy
 *
 * A simple script to parse the csv output we get from
 * BigTent and create bar chart of the output
 *
 * Usage: NewMemberChart fileName
 * @author akrieg
 */

/**
 * Check args
 */
if (args.length < 1) {
    println "usage: NewMemberChart fileName"
    return -1;
}
File f = new File(args[0])

/**
 * Check file existence
 */
if (!f.exists()) {
    println "File ${f.getPath()} does not exist"
    return -1;
}
/**
 * We want to replace entries like:
 * "JCFC Membership, 1/18/2011 - forever,"
 * with
 * "1/18/2011"
 */

//This is a regex pattern
def datePattern = ~/JCFC Membership, (\d+\/\d+\/\d+) - forever/
def generalMembershipPattern = ~/General Member, (\d+\/\d+\/\d+) - forever/

def intsToMonths = [1:"JAN",2:"FEB", 3:"MAR",4:"APR",5:"MAY",6:"JUN",7:"JUL",8:"AUG",9:"SEP",10:"OCT",11:"NOV",12:"DEC"]

//We create a group () around the date portion, which we'll
// use later for replacing the whole string with just the date

/**
 * We want to print out to a file with a similar name, but with a .new.csv
 * extension
 */
String outFileName = f.getPath().replace("csv", "new.csv")

println "Writing out to ${outFileName}"

DateTimeFormatter fmt_in = DateTimeFormat.forPattern("M/d/yyyy")

org.joda.time.DateTime now = new org.joda.time.DateTime();

/**
 * Let us construct a map of total number of occurrences per month, YTD
 */
//Map Month -> Counter of instances for the month
Map<Integer, Integer> monthlyTotals = new HashMap<Integer, Integer>();
Writer out = new File(outFileName).newWriter()
f.eachLine {
    line ->
    def matcher = datePattern.matcher(line)
    if (matcher.find()) {
        //do first regex replacement
        def time = fmt_in.parseDateTime(matcher.group(1))
        if (now.getYear() == time.getYear()) {
            Integer count = monthlyTotals.get(time.getMonthOfYear());
            if (count == null) {
                count = 0;
            }
            count++;
            monthlyTotals.put(time.getMonthOfYear(), count);
        }

    }
}
def dataset = new DefaultCategoryDataset()

monthlyTotals.entrySet().each { entry -> System.out.println(entry); dataset.addValue(entry.getValue(), "Members", intsToMonths.get(entry.getKey()))}


JFreeChart chart = createChart(dataset)

private JFreeChart createChart(DefaultCategoryDataset dataset) {
// create the chart...
    final JFreeChart chart = ChartFactory.createBarChart(
            "New Members Per Month (YTD)",         // chart title
            "Month",               // domain axis label
            "New Members",                  // range axis label
            dataset,                  // data
            PlotOrientation.VERTICAL, // orientation
            true,                     // include legend
            true,                     // tooltips?
            false                     // URLs?
    );

// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

    // set the background color for the chart...
    chart.setBackgroundPaint(Color.white);

// get a reference to the plot for further customisation...
    final CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.lightGray);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);

// set the range axis to display integers only...
    final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

// disable bar outlines...
    final BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(false);

// set up gradient paints for series...
    final GradientPaint gp0 = new GradientPaint(
            0.0f, 0.0f, Color.blue,
            0.0f, 0.0f, Color.lightGray
    );
    final GradientPaint gp1 = new GradientPaint(
            0.0f, 0.0f, Color.green,
            0.0f, 0.0f, Color.lightGray
    );
    final GradientPaint gp2 = new GradientPaint(
            0.0f, 0.0f, Color.red,
            0.0f, 0.0f, Color.lightGray
    );
    renderer.setSeriesPaint(0, gp0);
    renderer.setSeriesPaint(1, gp1);
    renderer.setSeriesPaint(2, gp2);

    final CategoryAxis domainAxis = plot.getDomainAxis();
    domainAxis.setCategoryLabelPositions(
            CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
    )
    return chart
}

// OPTIONAL CUSTOMISATION COMPLETED.

JFrame frame = createFrame(chart)

private JFrame createFrame(JFreeChart chart) {
    def swing = new SwingBuilder()
    def frame = swing.frame(title: 'Groovy LineChart',
            defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
        panel(id: 'canvas') { widget(new ChartPanel(chart)) }
    }
    frame.pack()
    return frame
}

frame.show()
System.out.println("Monthly totals = " + dataset);
//Create a Dataset

//Create a Chart

//Save as PDF
out.close()
println "Finished"

