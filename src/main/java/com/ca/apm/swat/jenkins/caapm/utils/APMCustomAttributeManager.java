package com.ca.apm.swat.jenkins.caapm.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.ca.apm.swat.jenkins.caapm.CAAPMProjectAction;

/***
 * 
 * @author Srikant N @ CA Technology
 * Dt: Sep 2015
 *
 */

public class APMCustomAttributeManager
{

    String momHost = null;
    String port = null;
    String authToken = null;
    String apm10AppName = null;
    int timeout = 5000;
    boolean isPublishAttrib = true;
    
    private static final Logger LOGGER = Logger.getLogger(APMCustomAttributeManager.class.getName());

    public APMCustomAttributeManager( String momHost, String port, String authToken, String apm10AppName ) {
        this.momHost = momHost;
        this.port = port;
        this.authToken = authToken;
        this.apm10AppName = apm10AppName;
        
        if(apm10AppName == null || authToken == null || apm10AppName.isEmpty() || authToken.isEmpty() ) {
        	isPublishAttrib = false;
        }
    }


    public boolean insertCustomAttributes( String buildNumber, String buildStatus ) {
    	
    	if (!isPublishAttrib)
    		return false;
    	

        CloseableHttpResponse response = null;

        try {
            //String relativeTime = endTime + startTime;
            //String connectionURL = "https://" + momHost + ":" + port + "/apm/appmap/vertex/1";
            String appName=apm10AppName;
            String connectionURL = "https://" + momHost + ":" + port + "/apm/appmap/" + "/vertex?q=attributes." + appName ;


            System.out.println( "APMCustomAttributeManager connection url is " + connectionURL);
            
            
            LOGGER.log(Level.FINEST, "APMCustomAttributeManager connection url is " + connectionURL);
            
            List<VertexInfo> vis =  getVertexInfoByApp(connectionURL);


            Calendar currentDate = Calendar.getInstance(); //Get the current date
            SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MMM-dd HH-mm a"); //format it as per your requirement
            String buildDate = formatter.format(currentDate.getTime());

            LOGGER.log(Level.FINEST, "APMCustomAttributeManager build date is " + buildDate);

            String payload = null;


            payload = "{"+
                    "\"attributes\": {" +
                    "\"Build Number\":\"" + buildNumber + "\","+
                    "\"Build Date\":\"" + buildDate + "\","+
                    // "\"Build Date\":" + buildDate + ","+
                    "\"Build Status\": \"" + buildStatus + "\""+
                    "}"+
                    "}";

            //          }
            //update all nodes
            
            int count = 0;
            for ( VertexInfo vi : vis ) {
            	
            	
                connectionURL = "https://" + momHost + ":" + port + "/apm/appmap/vertex/" + vi.getID();
                

                HttpClientBuilder client =  HttpClientBuilder.create();
                HttpPatch request = new HttpPatch(connectionURL);

                client = IgnoreSSLClient(client);

                request.addHeader("Authorization", "Bearer " + authToken);
                request.addHeader("Content-type", "application/json");
                request.addHeader("Accept", "application/json");

                StringEntity entity = new StringEntity(payload);
                request.setEntity(entity);

                StringBuffer stringResponse = new StringBuffer();

                RequestConfig.Builder requestBuilder = RequestConfig.custom();
                requestBuilder = requestBuilder.setConnectTimeout(timeout);
                requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);

                client.setDefaultRequestConfig(requestBuilder.build());

                LOGGER.log(Level.FINEST,  "Connection String is " + buildDate );

                
                LOGGER.log(Level.FINEST,  "Connection String is " + connectionURL );
                response = client.build().execute(request);

                LOGGER.log(Level.FINEST, "Connection String is return code " + response.getStatusLine().getStatusCode());

                if (response.getStatusLine().getStatusCode() != 200  && response.getEntity() != null ) {
                    BufferedReader rd = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));

                    String line = "";
                    while ((line = rd.readLine()) != null) {
                        stringResponse.append(line);
                    }

                    System.out.println(" Custom Attrib failed " + stringResponse );

                    //return false;
                }
                
                if(count++ == 2) {
                	Thread.sleep(200);
                	count = 0;
                }
            }

            return true;
        } catch ( Exception ex ) {
            ex.printStackTrace();
        } finally {
            try {
                if ( response != null )
                    response.close();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private List<VertexInfo> getVertexInfoByApp(String url) 
    {
        CloseableHttpResponse response = null;
        List<VertexInfo> info = new ArrayList<VertexInfo>();

        try {
            HttpClientBuilder client =  HttpClientBuilder.create();
            HttpGet request = new HttpGet(url);

            client = IgnoreSSLClient(client);

            LOGGER.log(Level.INFO,  "Connection String is " + url );


            request.addHeader("Authorization", "Bearer " + authToken);
            request.addHeader("Content-type", "application/json");
            request.addHeader("Accept", "application/json");

            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder = requestBuilder.setConnectTimeout(timeout);
            requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);

            client.setDefaultRequestConfig(requestBuilder.build());
            JSONObject JSONResponse = executeRequest(request, client);

            JSONArray verticesArr = JSONResponse.getJSONObject("_embedded").getJSONArray("vertex");
            for (int i = 0; i < verticesArr.length(); i++)
            {
                JSONObject vertex = verticesArr.getJSONObject(i);

                System.out.println( "got vertex for url " + vertex.getInt("id") + " app name is " + vertex.getJSONObject(
                        "attributes").getString("applicationName"));

                VertexInfo vi = new VertexInfo( vertex.getInt("id"), vertex.getJSONObject(
                        "attributes").getString("applicationName"));

                info.add(vi);
            }



        } catch ( Exception ex) {
            ex.printStackTrace();
        }
        return info;
    }

    private JSONObject executeRequest(HttpRequestBase request, HttpClientBuilder httpClient) throws IOException, JSONException
    {
        JSONObject jsonObj;


        CloseableHttpResponse resp = httpClient.build().execute(request);
        int statusCode = resp.getStatusLine().getStatusCode();
        if (resp.getEntity() != null)
        {
            jsonObj = new JSONObject(new JSONTokener(resp.getEntity().getContent()));
        }
        else
        {
            jsonObj = new JSONObject();
        }
        if (statusCode > 204)
        {
            LOGGER.log(Level.FINEST, "ApmAttributeHelper: Error executing request: "
                    + resp.getStatusLine());
            LOGGER.log(Level.FINEST, "Response: " + jsonObj);
        }
        HttpClientUtils.closeQuietly(resp);
        return jsonObj;
    }


    public  HttpClientBuilder IgnoreSSLClient( HttpClientBuilder client) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public java.security.cert.X509Certificate[] getAcceptedIssuers(){return null;}

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                           String authType)
                                                   throws java.security.cert.CertificateException {}
            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                           String authType)
                                                   throws java.security.cert.CertificateException{}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            //@SuppressWarnings("deprecation")
            javax.net.ssl.HostnameVerifier hnv = new javax.net.ssl.HostnameVerifier() {

                @Override
                public boolean verify(String arg0, SSLSession session)
                {
                    return true;
                }
            };
            SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory( sc, hnv) ;
            //HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            client.setSSLSocketFactory(sslcsf);

            return client;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class VertexInfo 
    {
        private int id;
        private String appname;

        public VertexInfo (int id, String appname ) {
            this.id = id;
            this.appname = appname;
        }

        public int getID() {
            return id;
        }

        public String getAppname() {
            return appname;
        }


    }

}
