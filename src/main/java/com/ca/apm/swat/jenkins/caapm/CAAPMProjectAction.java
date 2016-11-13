package com.ca.apm.swat.jenkins.caapm;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.taskdefs.BuildNumber;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.ca.apm.swat.jenkins.caapm.utils.CAAPMPerformanceReport;
import com.ca.apm.swat.jenkins.caapm.utils.MetricDataCollectionHelper.MetricData;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;

/***
 * 
 * @author Srikant N @ CA Technology
 * Dt: Sep 2015
 *
 */


public class CAAPMProjectAction implements Action
{

    private final AbstractProject<?, ?> project;

    private final String webviewHost;
    private final String webviewPort;
    
    private String firstBuildTime = null;
    private String lastBuildTime = null;

    private static final Logger LOGGER = Logger.getLogger(CAAPMProjectAction.class.getName());
    private static final long serialVersionUID = 1L;

    private static final String DASHBOARD = "caapm-dashboard";

    public CAAPMProjectAction (final AbstractProject project, final String webviewHost,
                               final String webviewPort ) {

        this.project = project;
        this.webviewHost = webviewHost;
        this.webviewPort = webviewPort;
    }

    @Override
    public String getDisplayName()
    {
        // TODO Auto-generated method stub
        return "Cross-Build Performance Dashboards";
    }

    @Override
    public String getIconFileName()
    {
        // TODO Auto-generated method stub
        return "graph.gif";
    }

    @Override
    public String getUrlName()
    {
        // TODO Auto-generated method stub
        return DASHBOARD;
    }

    public boolean isTrendAvailable() {
        LOGGER.log(Level.FINEST, "**** CAAPMProjectAction isTrendAvailable");
        return true;
    }

    public Collection<String> getMetricKeysFromLastGoodBuild() {

        final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

        for (AbstractBuild<?, ?> build : builds) {
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricKeysFromLastGoodBuild Called 1 " + build.number);

            CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricKeysFromLastGoodBuildCalled 2 " + buildAction);

            if ( buildAction == null )
                continue;
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricKeysFromLastGoodBuildCalled 3 -  " + buildAction.getDisplayName());

            CAAPMPerformanceReport report = buildAction.getReport();

            if ( report != null ) {
                Collection<String> keySet = report.getMetricKeySet();

                if(keySet == null) {
                    LOGGER.log(Level.FINEST, "**** CAAPMProjectActiongetMetricKeysFromLastGoodBuild Called 4 -  key set is null" );
                    continue;
                }
                return keySet;
            }

        }

        return null;

    }

    public Collection<MetricData> getMetricDataFromLastGoodBuild() {

        LOGGER.log(Level.INFO, "in getMetrciDataFromLastGoodBuild ");

        final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

        for (AbstractBuild<?, ?> build : builds) {
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricDataFromLastGoodBuild Called 1 " + build.number);

            CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricDataFromLastGoodBuild Called 2 " + buildAction);

            if ( buildAction == null )
                continue;
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricDataFromLastGoodBuild Called 3 -  " + buildAction.getDisplayName());

            CAAPMPerformanceReport report = buildAction.getReport();

            if ( report != null ) {
                Collection<MetricData> metricDataCollection = report.getMetricDataCollection();

                if(metricDataCollection == null) {
                    LOGGER.log(Level.FINEST, "**** CAAPMProjectAction getMetricDataFromLastGoodBuild Called 4 -  key set is null" );
                    continue;
                }
                return metricDataCollection;
            }

        }

        return null;

    }

    public CAAPMPerformanceReport getLastBuildReport() {

        final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

        for (AbstractBuild<?, ?> build : builds) {
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 1 " + build.number);

            CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 2 " + buildAction);

            if ( buildAction == null )
                continue;
            LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 3 -  " + buildAction.getDisplayName());

        }
        //LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 1 -  " + project.getLastSuccessfulBuild().number);
        // LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 2 -  " + project.getLastCompletedBuild().getAction(CAAPMBuildAction.class));
        // LOGGER.log(Level.FINEST, "**** CAAPMProjectAction get Last Build Called 3 -  " + project.getLastSuccessfulBuild().getAction(CAAPMBuildAction.class).getDisplayName());

        return project.getLastSuccessfulBuild().getAction(CAAPMBuildAction.class).getReport();
    }

    public AbstractProject<?, ?> getProject() {
        return this.project;
    }

    public void doRenderFailureMsgTable  (final StaplerRequest request,
                                      final StaplerResponse response) throws IOException {
        
    }
    
    
    
    
    public HashMap<Integer,String> getBuildNumberToMessageFailureMap() {
        
        LinkedHashMap<Integer,String> buildNumberToMessageFailureMap = new LinkedHashMap<Integer, String>();
        
        LOGGER.log(Level.INFO, "**** getBuildNumberToMessageFailureMap  Called " );
        
        try {

            final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

            int i = 0;

            for (AbstractBuild<?, ?> build : builds) {

                if (build == null ) {
                    continue;
                }

                int buildNumber = build.number;
                CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);

                if ( buildAction == null ) {
                    continue;
                }

                CAAPMPerformanceReport report = buildAction.getReport();

                if ( report == null )
                    continue;


                LOGGER.log(Level.INFO, "**** getBuildNumberToMessageFailureMap Project Build number " + buildNumber + " Message " + report.getFailPassReason());

                if ( report.getFailPassReason() !=null )
                     buildNumberToMessageFailureMap.put(buildNumber, report.getFailPassReason());
                

                //Last 10 builds ONLY
                if (i++ > 11 ) {
                    break;
                }

                
            }
            
            return buildNumberToMessageFailureMap;
            
        } catch ( Exception ex ) {
            ex.printStackTrace();
            
            return null;
        }
    }
    
    public String constructWebviewURL( String metricKey) {
        
        if ( metricKey == null ) {
            LOGGER.log(Level.INFO, "**** CAAPMPROJECTACTION::constructViewURL called with empty metric Key ");
            return "";
        }
        
        //Get the build start time for first and last build
        final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

        int i = 0;
        
        for (AbstractBuild<?, ?> build : builds) {

            if (build == null ) {
                continue;
            }

            int buildNumber = build.number;
            CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);

            if ( buildAction == null ) {
                continue;
            }

            CAAPMPerformanceReport report = buildAction.getReport();

            if ( report == null )
                continue;
            
            //this 30 sec is padding so that when x-build dashboard is clicked and goes to WV its 30 sec in future
            // and if any TT is generated then we include that
            if (lastBuildTime == null ) {
                lastBuildTime = (build.getStartTimeInMillis() + 30000 )+"";
                LOGGER.log(Level.INFO, "**** CAAPMPROJECTACTION::constructViewURL last build numb " + buildNumber );
            }

            //Last 10 builds ONLY
            if (i++ > 11 ) {
                
                if ( firstBuildTime == null ) {
                    firstBuildTime = build.getStartTimeInMillis() + "";
                    LOGGER.log(Level.INFO, "**** CAAPMPROJECTACTION::constructViewURL first build numb " + buildNumber);
                }
                
                break;
            }
        }

        metricKey = metricKey.replace("|", "%257C").replace(":", "%253A");
        
        LOGGER.log(Level.INFO, "**** CAAPMPROJECTACTION::constructViewURL start time " + firstBuildTime + " end time " + lastBuildTime );
        
        String webviewURL = null;
        
        if ( firstBuildTime == null || lastBuildTime == null ) {
        
            webviewURL = "http://" + webviewHost + ":" + webviewPort + "/#investigator;smm=false;tab-n=mb;tab-tv=pd;tr=0;uid="+ metricKey;
        
        } else {
            webviewURL = "http://" + webviewHost + ":" + webviewPort + "/#investigator;et=" + lastBuildTime + ";re=15000;smm=false;st="+ firstBuildTime + 
                    ";tab-in=mb;tab-tv=pd;tr=-1;uid="+ metricKey;
        }
        
        firstBuildTime = lastBuildTime = null;
        
        LOGGER.log(Level.INFO, "**** CAAPMPROJECTACTION::constructViewURL wv url is " + webviewURL);
        
        return webviewURL;
        
    }
    
    public Collection<Integer> getBuildNumber() {
        HashMap<Integer,String> buildNumberToMessageFailureMap = getBuildNumberToMessageFailureMap();
        
        if ( buildNumberToMessageFailureMap != null ) {
            return buildNumberToMessageFailureMap.keySet();
        }
            
        return null;
    }
    
     
    public void doRenderMetricGraph  (final StaplerRequest request,
                                      final StaplerResponse response) throws IOException {

        String metricKey = request.getParameter("metricKey");
        HashMap<Integer, Long> buildNumberToARTMap = new HashMap<Integer, Long>();

        LOGGER.log(Level.FINEST, "**** Project Build render Metric Graph " + metricKey );

        try {

            final List<? extends AbstractBuild<?, ?>> builds = project.getBuilds();

            int i = 0;

            for (AbstractBuild<?, ?> build : builds) {

                if (build == null ) {
                    continue;
                }

                int buildNumber = build.number;
                CAAPMBuildAction buildAction = build.getAction(CAAPMBuildAction.class);

                if ( buildAction == null ) {
                    continue;
                }

                CAAPMPerformanceReport report = buildAction.getReport();

                if ( report == null )
                    continue;


                LOGGER.log(Level.INFO, "**** Project Build number " + buildNumber + " metric name "  + metricKey );
                LOGGER.log(Level.FINEST," Build ART " + report.getARTForBuild(metricKey));
                
                buildNumberToARTMap.put(buildNumber, report.getARTForBuild(metricKey));
                

                //Last 10 builds ONLY
                if (i++ > 11 ) {
                    

                    break;
                }

            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        final Graph graph = new GraphImplementation(metricKey, buildNumberToARTMap );

        LOGGER.log(Level.FINEST, "**** Project rendering metric graph ");
        //System.out.println(" Project rendering metric graph system out");

        graph.doPng(request, response);

    }

    
    

    
    private class GraphImplementation extends Graph {
        private final String graphTitle;
        private HashMap<Integer, Long> buildNumberToARTMap;

        protected GraphImplementation(final String metricKey, HashMap<Integer, Long> buildNumberToARTMap) {
            super(-1, 500, 400); 
            this.graphTitle = metricKey;
            this.buildNumberToARTMap = buildNumberToARTMap;
        }


        protected DataSetBuilder<String, Integer> createDataSet() {
            DataSetBuilder<String, Integer> dataSetBuilder =
                    new DataSetBuilder<String, Integer>();

            Set<Integer> buildNumbers = buildNumberToARTMap.keySet();
            int size = buildNumberToARTMap.size();


            for ( Iterator<Integer> iter = buildNumbers.iterator(); iter.hasNext(); ) {
                Integer buildNumber = iter.next();

                dataSetBuilder.add(buildNumberToARTMap.get(buildNumber), "Test Graph Srikant", buildNumber);

                LOGGER.log(Level.FINEST, "****#### Render Graph CA " + buildNumberToARTMap.get(buildNumber) );
            }

            return dataSetBuilder;
        }

        protected JFreeChart createGraph() {
            final CategoryDataset dataset = createDataSet().build();

            String[] stringArray = null;

            if (graphTitle !=null) {
                stringArray = graphTitle.split(":");
            }

            String metricName = "";
            if ( stringArray != null  && stringArray.length == 2 ) {
                metricName = stringArray[1];
            }

            final JFreeChart barChart = ChartFactory.createBarChart3D(" ", 
                                                                  "Build Number", 
                                                                  null, 
                                                                  dataset,
                                                                  PlotOrientation.VERTICAL, 
                                                                  false, 
                                                                  true, 
                                                                  false 
                    );

            barChart.setBackgroundPaint(Color.white);
            barChart.setBackgroundPaint(Color.yellow);
            barChart.getCategoryPlot().getRenderer().setSeriesPaint(0,  Color.darkGray);
//            StandardCategoryURLGenerator urlGen = new StandardCategoryURLGenerator();
  //          chart.getCategoryPlot().getRenderer().setBaseItemURLGenerator(arg0);

            return barChart;
        }
    }
}
