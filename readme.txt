Project space to put together the results of a generic exception framework
example based on jBPM version 3.2.8 (see pom.xml for dependencies).

Currently only running a test to show simple node failing, then passing exception and 
all process context over to the Exception Framework. This framework defaults currently 
to a Human Task for processing the error. In our test we end this task to continue 
processing and go back to the originating process.

This project is maven enabled.

TODO: State node failure example via unit test class.
 
With my thanks to Maurice de Chateau for his contributions.

Regards,
Eric D. Schabell
eric@schabell.org
erics@redhat.com