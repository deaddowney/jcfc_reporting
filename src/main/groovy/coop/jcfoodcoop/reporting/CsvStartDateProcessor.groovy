package coop.jcfoodcoop.reporting

/**
 * CsvStartDateProcessor.groovy
 *
 * A simple script to parse the csv output we get from
 * BigTent and transform it into a more usable format for
 * reporting.
 * For more information about Groovy Scripts and CLI see:
 * http://groovy.codehaus.org/Groovy+CLI
 *
 * Usage: CsvStartDateProcessor fileName
 * @author akrieg
 */

/**
 * Check args
 */
if (args.length < 1) {
    println "usage: CsvStartDateProcessor fileName"
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

//We create a group () around the date portion, which we'll
// use later for replacing the whole string with just the date

/**
 * We want to print out to a file with a similar name, but with a .new.csv
 * extension
 */
String outFileName = f.getPath().replace("csv", "new.csv")

println "Writing out to ${outFileName}"

Writer out = new File(outFileName).newWriter()
f.eachLine {
    line ->
    //do first regex replacement
    String replacedJcfcMember = datePattern.matcher(line).replaceFirst('$1')
    out.println generalMembershipPattern.matcher(replacedJcfcMember).replaceFirst('$1')
}
out.close()
println "Finished"

