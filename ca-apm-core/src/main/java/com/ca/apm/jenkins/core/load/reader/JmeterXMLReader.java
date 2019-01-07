package com.ca.apm.jenkins.core.load.reader;

import com.ca.apm.jenkins.api.LoadGenOPFileReader;
import com.ca.apm.jenkins.api.exception.BuildValidationException;
import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JmeterXMLReader implements LoadGenOPFileReader {

  private static final String XML = ".xml";

  /** This method is invoked to read the provided CSV file and return the start, end time. */
  public long[] getBuildTSFromOutputFile(String jMeterOutputFile) throws BuildValidationException {
    long[] currValues = new long[2];
    if (jMeterOutputFile.endsWith(XML)) {
      String xmlFilePath = jMeterOutputFile;
      File fXmlFile = new File(xmlFilePath);
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder;
      try {
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        doc.getDocumentElement().normalize();
        NodeList nList = doc.getElementsByTagName("testResults");
        long minTs = 0;
        long maxTs = 0;
        for (int temp = 0; temp < nList.getLength(); temp++) {
          Node nNode = nList.item(temp);
          List<Long> tsList = new LinkedList<Long>();
          if (nNode.getNodeType() == Node.ELEMENT_NODE) {

            Element eElement = (Element) nNode;
            NodeList list = eElement.getElementsByTagName("httpSample");
            for (int i = 0; i < list.getLength(); i++) {
              Node httpSampleNode = list.item(i);
              Element hElement = (Element) httpSampleNode;
              long ts = Long.parseLong(hElement.getAttribute("ts"));
              tsList.add(ts);
              if (ts > maxTs) maxTs = ts;
              if (minTs == 0) {
                minTs = ts;
              }
              if (ts < minTs) minTs = ts;
            }
          }
        }
        currValues[0] = minTs;
        currValues[1] = maxTs;
      } catch (FileNotFoundException e) {
        throw new BuildValidationException(
            "The XML file - "
                + jMeterOutputFile
                + " is not found under jenkins workspace's jobname directory");
      } catch (ParserConfigurationException e) {
        JenkinsPlugInLogger.severe("Parsing error in reading the XML file", e);
      } catch (SAXException e) {
        JenkinsPlugInLogger.severe("SAX Error error in reading the XML file", e);
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("I/O error in reading the XML file", e);
      }

      return currValues;
    } else {
      throw new BuildValidationException(jMeterOutputFile + " is not a valid xml file ");
    }
  }
}
