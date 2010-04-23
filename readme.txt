Project space to put together the results of a generic exception framework
example based on jBPM version 3.2.8 (see pom.xml for dependencies).

Contains tests to show simple node and state node failing, then passing exception and 
all process context over to the Exception Framework. This framework defaults currently 
to a Human Task for processing the error. In our test we end this task to continue 
processing and go back to the originating process.


This project is maven enabled, which means you can check this out as a java project in eclipse and just run:

maven eclipse:eclipse
 
My thanks to Maurice de Chateau for his contributions.

Regards,
Eric D. Schabell
eric@schabell.org
erics@redhat.com