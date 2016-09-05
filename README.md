CA-APM Jenkins Plugin
====================

ca-apm jenkins plugin
---------------------
Developed by Srikant Noorani @ CA Inc Dec 2015
<br>This is NOT an officially supported CA product but is provided on an as is basis. 
Please collaborate and improve the project.

Plugin Setup
---------------
Follows the standard Jenkins plugin process.
<br>You will need easy XL tool kit - an EM side plugin - for this plugin to work (please talk to your sales team)

Description
-----------------
This plugin allows Jenkins users to collect [CA-APM](http://www.ca.com/us/products/ca-application-performance-management.html) performance data from EM.

Here is the list of features ( see the configuration section below for details)

* ability to fetch multiple metrics using regex as KPI for tracking.
* Ability to configure multiple fail conditions
    * Metric A less than or greater than a constant value OR
    * Metric A in comparison to Metric B
* Ability to send notification via Email (if smtp server is configured)
* Ability to view KPI's per build or cross build dashboard in Jenkins
* Ability to decorate ATC 10.1 nodes using custom attributes like build status, number and date 
* Ability to just notify via email: If "justEmail" is checked the build will NOT be marked as fail even if the conditions are met but only a notification will be sent
* Ability Jump to Webview in context (for both metric and time range) from build or x-build dashboard


To Run
-------
Clone the project and run mvn compile. Read up the wiki for more info
Restart tomcat.
