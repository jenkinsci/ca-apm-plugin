package com.ca.apm.swat.jenkins.caapm;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.servlet.ServletException;


import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.ca.apm.swat.jenkins.caapm.utils.GenericDataCollector;
import com.ca.apm.swat.jenkins.caapm.utils.HttpDataCollector;
import com.ca.apm.swat.jenkins.caapm.utils.MetricDataCollectionHelper;
import com.ca.apm.swat.jenkins.caapm.utils.APMCustomAttributeManager;
import com.ca.apm.swat.jenkins.caapm.utils.CAAPMPerformanceReport;
import com.ca.apm.swat.jenkins.caapm.utils.EmailManager;
import com.ca.apm.swat.jenkins.caapm.utils.MetricDataCollectionHelper.DataPoint;
import com.ca.apm.swat.jenkins.caapm.utils.MetricDataCollectionHelper.MetricData;

/***
 * 
 * @author Srikant N @ CA Technology
 * Dt: Sep 2015
 *
 */


public class CAAPMJenkinsPublisher extends Recorder {

    private static final Logger LOGGER = Logger.getLogger(CAAPMJenkinsPublisher.class.getName());
    private static final int MINIMUM_TIME_RANGE_MINUTES = 10;

    private String momHost;
    private String momPort;
    private String username;
    private String password;
    private int lastNDataPoints;
    private int frequencyInSec;
    private boolean allDataPoints;
    private String agentPath;
    private String metricPath;
    private String singleMetricName;
    private String singleMetricValue;
    private String singleMetricOperator;
    private String multiMetricName1;
    private String multiMetricName2;
    private String multiMetricRatio;
    private String multiMetricOperator;
    private String emailAddress;
    private String emailPassword;
    private boolean justEmail;
    private String apm10RestToken;
    private String apm10RestPort;
    private String webviewHost;
    private String webviewPort;
    private String apm10AppName;

    String name;

    public String getName() {
        return name;
    }

    @DataBoundConstructor
    public CAAPMJenkinsPublisher(final String momHost, final String momPort, final String username, final String password,
                                 final int lastNDataPoints, final int frequencyInSec, final boolean allDataPoints, final String agentPath, final String metricPath,
                                 final String singleMetricName, final String singleMetricValue, String singleMetricOperator,
                                 final String multiMetricName1, final String multiMetricName2,final String multiMetricOperator, final String multiMetricRatio,
                                 final boolean justEmail, final String emailAddress, final String emailPassword, final String apm10RestPort, final String apm10RestToken, final String webviewHost, final String webviewPort, final String apm10AppName) {
        this.momHost = momHost;
        this.momPort = momPort;
        this.username = username;
        this.password = password;
        this.lastNDataPoints = lastNDataPoints;
        this.frequencyInSec = frequencyInSec;
        this.allDataPoints = allDataPoints;
        this.agentPath = agentPath;
        this.metricPath = metricPath;
        this.singleMetricName = singleMetricName;
        this.singleMetricValue = singleMetricValue;
        this.singleMetricOperator = singleMetricOperator;
        this.multiMetricRatio = multiMetricRatio;
        this.multiMetricName1 = multiMetricName1;
        this.multiMetricName2 = multiMetricName2;
        this.multiMetricOperator = multiMetricOperator;
        this.emailAddress = emailAddress;
        this.emailPassword = emailPassword;
        this.justEmail = justEmail;
        this.apm10RestToken = apm10RestToken;
        this.apm10RestPort = apm10RestPort;
        this.webviewHost = webviewHost;
        this.webviewPort = webviewPort;
        this.apm10AppName = apm10AppName;


        LOGGER.log(Level.FINE, "In CAPM Jenkins publising single metric operator " + singleMetricOperator);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        // No synchronization necessary between builds
        return BuildStepMonitor.NONE;
    }

    public String getMomHost() {
        return momHost;
    }

    public String getMomPort() {
        return momPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getLastNDataPoints() {
        return lastNDataPoints;
    }

    public int getFrequencyInSec() {
        return frequencyInSec;
    }

    public boolean getAllDataPoints() {
        return allDataPoints;
    }

    public String getAgentPath() {
        return agentPath;
    }

    public String getMetricPath() {
        return metricPath;
    }

    public String getSingleMetricValue() {
        return singleMetricValue;
    }

    public String getSingleMetricName() {
        return singleMetricName;
    }

    public String getSingleMetricOperator() {
        return singleMetricOperator;
    }

    public String getMultiMetricName1() {
        return multiMetricName1;
    }

    public String getMultiMetricName2() {
        return multiMetricName2;
    }

    public String getMultiMetricRatio() {
        return multiMetricRatio;
    }

    public String getMultiMetricOperator() {
        return multiMetricOperator;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public boolean getJustEmail() {
        return justEmail;
    }

    public String getApm10RestToken() {
        return apm10RestToken;
    }

    public String getApm10RestPort() {
        return apm10RestPort;
    }

    public String getWebviewHost() {
        return webviewHost;
    }

    public String getWebviewPort() {
        return webviewPort;
    }
    
    public String getApm10AppName() {
        return apm10AppName;
    }
    

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        try {
            String buildNumberStr = build.number+"";
            EmailManager emailManager = new EmailManager(build, listener);
            
            emailManager.setPassword(emailPassword);
            emailManager.setRecepient(emailAddress);
            
            PrintStream logger = listener.getLogger();



            LOGGER.log(Level.FINEST, "**** Running Perform method in Junkins Publisher " );


            HttpDataCollector httpDataCollector = new HttpDataCollector(momHost, momPort);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(build.getStartTimeInMillis());

            LOGGER.log(Level.FINEST, "**** Running Perform method buildStart Time is " + cal.get(Calendar.DATE) + ":" + cal.get(Calendar.MONTH) + ":" + cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) );

            MetricDataCollectionHelper metricDataCollectionHelper = null;

            if ( !getAllDataPoints())
                metricDataCollectionHelper = httpDataCollector.fetchLastNMetricData(agentPath, metricPath, frequencyInSec, build.getStartTimeInMillis(), Calendar.getInstance().getTimeInMillis(), lastNDataPoints);
            else
                metricDataCollectionHelper = httpDataCollector.fetchAllMetricData(agentPath, metricPath, build.getStartTimeInMillis(), Calendar.getInstance().getTimeInMillis());


            CAAPMPerformanceReport report = new CAAPMPerformanceReport("CA APM Report - Build " + build.number, metricDataCollectionHelper);

            CAAPMBuildAction buildAction = new CAAPMBuildAction(build, report, webviewHost, webviewPort);
            build.addAction(buildAction);

            //APMCustomAttributeManager apmCustomAttributeManager = new APMCustomAttributeManager(momHost, "8444", "2391d6ea-d248-491e-a839-b920fadbb3b5");
            APMCustomAttributeManager apmCustomAttributeManager = new APMCustomAttributeManager(momHost, apm10RestPort,apm10RestToken, apm10AppName);

            //this could happen if the build time was too short and we did not have 10 DP's 
            //OR agent,metric expression did not match EM metrics
            if ( metricDataCollectionHelper == null ) {


                LOGGER.log(Level.FINEST, "**** Perform MetricDataCollection returned null" );

                //build.setResult(Result.FAILURE);

                String failReason = "Sorry overall build time is too short or agent, metric path did not match EM metric. Check specified agent and metric expression";

                report.setFailPassReason(failReason);

                LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

                //if ( !justEmail ) {
                build.setResult(Result.FAILURE);

                apmCustomAttributeManager.insertCustomAttributes(  build.number+"","Fail" );
                // }

                    emailManager.sendMail(buildNumberStr, "FAIL", failReason);
                    
                    listener.getLogger().println("BUILD FAILURE REASON: " + failReason);
                    
                return true;
            }

            //this could happen if we had enough time in build time but for some reason EM did not return enough DP's
            if ( metricDataCollectionHelper.getMetricKeyToMetricDataMap().size() == 0 ) {

                LOGGER.log(Level.FINEST, "**** Perform MetricDataCollection returned null OR metricData Collection 0" );

                String failReason = "Sorry not enough data points. May be EM has just started.";

                report.setFailPassReason(failReason);

                LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

                // if ( !justEmail ) {
                build.setResult(Result.FAILURE);
                
                emailManager.sendMail(buildNumberStr, "FAIL", failReason);

                apmCustomAttributeManager.insertCustomAttributes(  build.number+"","Fail" );
                
                listener.getLogger().println("BUILD FAILURE REASON: " +failReason);
                //  }
                return true;
            } else  {//Now we know DP map has some metric we need at least one metric with more than MIN numb of DP

                boolean minNumbOfDPsAvailable = false;

                for (MetricData metricData : metricDataCollectionHelper.getMetricDataCollection())  {



                    if (metricData.getDataPoints().size() >= GenericDataCollector.MIN_DATA_POINTS ) {
                        minNumbOfDPsAvailable = true;
                        break;
                    }
                }

                if (!minNumbOfDPsAvailable) {
                    LOGGER.log(Level.FINEST, "**** Not enough Data Points available for any metrics" );

                    String failReason = "Sorry not enough Data Points available for any metrics. Pls wait till we have at least " + GenericDataCollector.MIN_DATA_POINTS + " data points.";

                    report.setFailPassReason(failReason);

                    LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

                    // if ( !justEmail ) {
                    build.setResult(Result.FAILURE);

                    emailManager.sendMail(buildNumberStr, "FAIL", failReason);
                    
                    apmCustomAttributeManager.insertCustomAttributes(  build.number+"","Fail" );
                    //}

                    listener.getLogger().println("BUILD FAILURE REASON: " +failReason);
                    
                    return true;
                }

            }

            Result buildResult;

            boolean result = determineMultiMetricBuildResult(report, logger);

            result = result ? determineSingleMetricBuildResult(report, logger) : false;

            if ( result == false ) {
                apmCustomAttributeManager.insertCustomAttributes(  build.number+"","Fail" );

                build.setResult( Result.FAILURE);
                
                emailManager.sendMail(buildNumberStr, "FAIL", report.getFailPassReason());
                
                listener.getLogger().println("BUILD FAILURE REASON: " +report.getFailPassReason());
                
                return true;
            }

            build.setResult(  Result.SUCCESS);
            
            
            emailManager.sendMail(build.number+"", "SUCCESS", "");

        } catch (Exception ex ) {
            ex.printStackTrace();
        }

        //reset the build nubmer + state in case prev set.
        //if not no harm is done
        try {
            APMCustomAttributeManager apmCustomAttributeManager = new APMCustomAttributeManager(momHost, apm10RestPort, apm10RestToken, apm10AppName);
            apmCustomAttributeManager.insertCustomAttributes(  build.number+"","Pass" );
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
        
        
        return true;

    }

    //Determine if multi metric build condition is met or not. 
    //true means fail cond not met, false is met meaning build fail

    public boolean determineMultiMetricBuildResult( CAAPMPerformanceReport report, PrintStream logger  ) {

        //Collection<String> metricKeys = report.getMetricKeySet();
        //final MetricData metricData = report.getMetricByKey(metricKey);

        //String metricKey2 = "SuperDomain|RHat66|TixChangeProcess|TixChange|Application|TixChange|Event Customization|Backends:Total DB Calls";
        //String metricKey1 = "SuperDomain|RHat66|TixChangeProcess|TixChange|Application|TixChange|Event Customization:Total Custom Method Calls";

        String metricKey1 = getMultiMetricName1();
        String metricKey2 = getMultiMetricName2();

        //This condition is not set by the user
        if( metricKey1 == null || metricKey2 == null || metricKey2.trim().isEmpty() || metricKey1.trim().isEmpty()) {
            return true;
        }


        float compareRatio = 0;

        try {
            compareRatio = new Float(getMultiMetricRatio());
        } catch (Exception ex ) {
            return true;
        }

        LOGGER.log(Level.FINE, " DetermineMultiBuildResult metric1 " + metricKey1 + " metric 2 " + metricKey2 + " user ratio "  + multiMetricRatio);

        Map<String, MetricData> metricKeyToMetriDatacMap = report.getMetricDataCollectionHelper().getMetricKeyToMetricDataMap();

        MetricData metricDataMap1 = metricKeyToMetriDatacMap.get(metricKey1);
        MetricData metricDataMap2 = metricKeyToMetriDatacMap.get(metricKey2);

        int dataPointSize1 = (metricDataMap1 != null ) ? metricDataMap1.getDataPoints().size() : 0;
        int dataPointSize2 = (metricDataMap2 != null ) ? metricDataMap2.getDataPoints().size() : 0;

        LOGGER.log(Level.FINE,  " DetermineBuildResult called with dp size1 " + dataPointSize1 + " dp size2 " + dataPointSize2 );

        if ( dataPointSize1 == 0 || dataPointSize2 == 0 ) {

            String failReason = "Either metric1 \"" + metricKey1 + "\" or metric2 \"" + metricKey2 +"\" did not match any metric specified in agent and metric path";
            report.setFailPassReason(failReason);

            LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

            return false;
        }


        Object[] dPointArray1 = metricDataMap1.getDataPoints().toArray();
        Object[] dPointArray2 =  metricDataMap2.getDataPoints().toArray();

        for ( int i = 0; i < dataPointSize1 && i < dataPointSize2; i++ ) {
            double ratio = (double)((DataPoint)dPointArray1[i]).value / (double)((DataPoint)dPointArray2[i]).value;

            String[] metricOne = metricKey1.split(":");
            String[] metricTwo = metricKey2.split(":");

            LOGGER.log(Level.FINE,  " DetermineMultiBuildResult ratio is " + ratio);

            if ( multiMetricOperator.equals("lessThan")) {
                if (ratio < compareRatio ) {

                    String failReason = "\"" + metricOne[1] + "\" is too high when compared with \""+ metricTwo[1] + 
                            "\". Calculated ratio is " + ratio + " worse than expected ratio of "+ compareRatio;

                   // report.setFailPassReason(failReason);

                    LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason + " just email " + justEmail);

                  //only fail if justEmail is not set
                    if ( !justEmail ) {
                        report.setFailPassReason(failReason);
                        return false;
                    }  else {
                        return true;
                    }
                } else if (ratio > compareRatio ) {

                    String failReason = "\"" + metricOne[1] + "\" is too low when compared with \""+ metricTwo[1] + "\". Calculated ratio is " + ratio + " worse than expected ratio of "+ compareRatio;

                    //report.setFailPassReason(failReason);

                    LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason + " just email " + justEmail);
                    
                    //only fail if justEmail is not set
                    if ( !justEmail ) {
                        report.setFailPassReason(failReason);
                        return false;
                    }  else {
                        return true;
                    }
                }
            }
        }

        return true;
    }

    public boolean determineSingleMetricBuildResult( CAAPMPerformanceReport report, PrintStream logger  ) {

        String metricKey = getSingleMetricName();
        float metricValue = 0;

        //this condn not set
        if( metricKey == null ||  metricKey.trim().isEmpty() ) {
            return true;
        }

        try {
            metricValue = new Float(getSingleMetricValue());
        } catch (Exception ex ) {
            return true;
        }

        String compareOperator = getSingleMetricOperator();

        LOGGER.log(Level.FINE,  " DetermineSingle Metric build " + metricKey + " metricValue " + metricValue + " singleMetricOperator " + singleMetricOperator);

        Map<String, MetricData> metricKeyToMetriDatacMap = report.getMetricDataCollectionHelper().getMetricKeyToMetricDataMap();

        MetricData metricDataMap = metricKeyToMetriDatacMap.get(metricKey);

        if ( metricDataMap == null ) {

            String failReason = "Metric \"" + metricKey + "\" did not match any metric specified in agent and metric path";
            report.setFailPassReason(failReason);

            LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

            return false;
        }

        int average = metricDataMap.getAverageART() ;

        LOGGER.log(Level.FINE,  " DetermineSingle Metric build average is " + average );

        //fail condition meets
        if ( compareOperator.equals("greaterThan")) {
            if ( average > metricValue) {

                String failReason = "Build fail condition met. Calculated value " + average + " for metric  \""  + metricKey + "\" is greater than the specified value of " + metricValue;

                LOGGER.log(Level.FINEST, " BUILD FAILED??: "+ failReason  + " just email is " + justEmail);
              
                //only fail if justEmail is not set
                if ( !justEmail ) {
                    report.setFailPassReason(failReason);
                    return false;
                }  else {
                    return true;
                }
            }
        } else {
            if ( average < metricValue) {

                String failReason = "Build fail condition met. Calculated value " + average + " for metric  \""  + metricKey + "\" is less than the specified value of " + metricValue;

               // report.setFailPassReason(failReason);

                LOGGER.log(Level.FINEST, " BUILD FAILED: "+ failReason );

              //only fail if justEmail is not set
                if ( !justEmail ) {
                    report.setFailPassReason(failReason + " just email is " + justEmail);
                    return false;
                }  else {
                    return true;
                }
            }
        }       

        return true;
    }


    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        //String[] dummyMetrics = new String[10];

        LOGGER.log(Level.FINEST, " in getProject Action in publisher ####");

        return new CAAPMProjectAction(project, webviewHost, webviewPort);
    }


    @Extension    
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private String momHost;
        private String momPort;
        private String username;
        private String password;
        private int lastNDataPoints;
        private int frequencyInSec;
        private boolean allDataPoints;
        private String agentPath;
        private String metricPath;
        private String singleMetricName;
        private String singleMetricOperator;
        private String singleMetricValue;
        private String multiMetricName1;
        private String multiMetricName2;
        private String multiMetricRatio;
        private String multiMetricOperator;
        private String emailAddress;
        private String emailPassword;
        private boolean justEmail;
        private String apm10RestToken;
        private String apm10RestPort;
        private String webviewHost;
        private String webviewPort;
        private String apm10AppName;

        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        @Override
        public String getDisplayName() {
            return "CA APM Jenkins Configuration";
        }

        public DescriptorImpl() {
            try {
                load();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }

        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0)
        {
            // TODO Auto-generated method stub
            return true;
        }

        public int getDefaultMinimumTimeRangeInMinutes() {
            return 10;
        }


        public FormValidation doTestConnection(@QueryParameter("momHost") final String momHost,
                                               @QueryParameter("username") final String username,
                                               @QueryParameter("momPort") final String port) {
            FormValidation validationResult;
            boolean connection = GenericDataCollector.testMomConnection(momHost, port);

            if (connection) {
                validationResult = FormValidation.ok("Connection successful");
            } else {
                validationResult = FormValidation.warning("Connection failed");
            }

            return validationResult;
        }

        public FormValidation doTestRegex(@QueryParameter("momHost") final String momHost,
                                          @QueryParameter("username") final String username,
                                          @QueryParameter("momPort") final String port,
                                          @QueryParameter("momPort") final String agentPath,
                                          @QueryParameter("momPort") final String metricPath){
            FormValidation validationResult;
            String regex = GenericDataCollector.testRegex(momHost, port, username, agentPath, metricPath);

            if (regex != null && !regex.isEmpty()) {
                validationResult = FormValidation.ok("Regex Matches");
            } else {
                validationResult = FormValidation.warning("Regex Matches Nothing");
            }

            return validationResult;
        } 


        public ListBoxModel doFillSingleMetricOperatorItems(){

            boolean defaultGT = true;
            boolean defaultLT = false;

            if( singleMetricOperator != null && singleMetricOperator.equals("lessThan")) {
                defaultGT = false;
                defaultLT = true;
            }

            return new ListBoxModel(
                                    new Option("Greater Than", "greaterThan", defaultGT),
                                    new Option("Less Than", "lessThan", defaultLT));
        }

        public ListBoxModel doFillMultiMetricOperatorItems(){

            boolean defaultGT = true;
            boolean defaultLT = false;

            if( multiMetricOperator != null && multiMetricOperator.equals("lessThan")) {
                defaultGT = false;
                defaultLT = true;
            }

            return new ListBoxModel(
                                    new Option("Greater Than", "greaterThan", defaultGT),
                                    new Option("Less Than", "lessThan", defaultLT));
        }


        public FormValidation doCheckMomHost(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please specify mom host name or ip");
            return FormValidation.ok();
        }

        public FormValidation doCheckMomPort(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a mom port");
            return FormValidation.ok();
        }

        public FormValidation doCheckUsername(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a user name");
            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value) 
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.warning("Looks like you dont want to set a password");
            return FormValidation.ok();
        }

        public FormValidation doCheckAgentPath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set an agent path");
            return FormValidation.ok();
        }

        public FormValidation doCheckMetricPath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a metric Path");
            return FormValidation.ok();
        }

        public FormValidation doCheckNumbDataPoints(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set number of data points");
            return FormValidation.ok();
        }


        @Override
        public CAAPMJenkinsPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {

            try {
                momHost = formData.getString("momHost");
                momPort= formData.getString("momPort");
                username= formData.getString("username");
                password= formData.getString("password");
                try {
                	lastNDataPoints= formData.getInt("lastNDataPoints");
                } catch ( Exception ex ) {
                	lastNDataPoints = 10;
                }
                frequencyInSec= formData.getInt("frequencyInSec");
                allDataPoints = formData.getBoolean("allDataPoints");
                agentPath= formData.getString("agentPath");
                metricPath= formData.getString("metricPath");
                singleMetricName = formData.getString("singleMetricName");
                singleMetricValue = formData.getString("singleMetricValue");
                singleMetricOperator = formData.getString("singleMetricOperator");
                multiMetricName1 = formData.getString("multiMetricName1");
                multiMetricName2 = formData.getString("multiMetricName2");
                multiMetricRatio = formData.getString("multiMetricRatio"); 
                multiMetricOperator = formData.getString("multiMetricOperator");
                justEmail = formData.getBoolean("justEmail");
                emailAddress = formData.getString("emailAddress");
                emailPassword = formData.getString("emailPassword");
                apm10RestPort = formData.getString("apm10RestPort");
                apm10RestToken = formData.getString("apm10RestToken");
                apm10AppName = formData.getString("apm10AppName");
                webviewHost = formData.getString("webviewHost");
                webviewPort = formData.getString("webviewPort");

                LOGGER.log(Level.FINEST, "##### new formData " + formData);

                CAAPMJenkinsPublisher caAPMPublisher = new CAAPMJenkinsPublisher(momHost, momPort, username, password,
                                                                                 lastNDataPoints, frequencyInSec, allDataPoints, agentPath, metricPath, singleMetricName, 
                                                                                 singleMetricValue, singleMetricOperator, multiMetricName1, multiMetricName2, multiMetricOperator, 
                                                                                 multiMetricRatio, justEmail, emailAddress, emailPassword, apm10RestPort, apm10RestToken, webviewHost, webviewPort, apm10AppName);
                //return super.configure(req,formData);

                save();

                return caAPMPublisher;

            } catch ( Exception ex ) {
                ex.printStackTrace();
            }

            return null;
        }


    }



}

