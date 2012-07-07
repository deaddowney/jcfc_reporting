REPORTING
Script to process the CSV files that BigTent reports produce.
This will clean up the format so that we can run historical reports.

This project requires Apache Maven to compile.

To create a stand-alone jar, type:
mvn assembly:assembly -DdescriptorId=jar-with-dependencies

This should create a jar in the target directory
(Reporting-1.0-jar-with-dependencies.jar) which you can execute
by:
java -jar Reporting-1.0-jar-with-dependencies.jar

which will print out:
usage: CsvStartDateProcessor fileName


So you can process a csv file by typing:
java -jar Reporting-1.0-jar-with-dependencies.jar /path/to/csv/file

which will read the csv file and print out a new file under
/path/to/csv/file.new.csv

