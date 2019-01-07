package com.ca.apm.jenkins.core.helper;

import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility to help you to create a file, folder or exporting an output to file to a specific
 * location
 *
 * @author Avinash Chandwani
 */
public class FileHelper {

  private FileHelper() {
    super();
  }

  /**
   * Utility to check whether the given file exist or not
   *
   * @param fileName Full name of the file along with absolute path
   * @return Returns true or false based upon whether file exists or not
   */
  public static boolean fileExists(String fileName) {
    File file = new File(fileName);
    return file.exists();
  }

  /**
   * Create a directory, but no guarantee, it may fail if it is not allowed to create
   *
   * @param dir The directory name which you want to create
   * @return Returns true if it is successful in creating the desired directory or if directory
   *     already exists
   */
  public static boolean createDirectory(String dir) {
    File directory = new File(dir);
    if (directory.exists()) return true;
    return directory.mkdir();
  }

  public static void initializeLog(
      String loggingLevel, String buildDirectory, int currentBuildNumber) {
    boolean fileExist = fileExists(buildDirectory);
    if (!fileExist) {
      fileExist = createDirectory(buildDirectory);
    }
    if (fileExist) {
      fileExist = createDirectory(buildDirectory + File.separator + currentBuildNumber);
    }
    if (fileExist) {
      JenkinsPlugInLogger.configureLog(
          loggingLevel,
          buildDirectory
              + File.separator
              + currentBuildNumber
              + File.separator
              + "comparison-runner.log");
    }
    JenkinsPlugInLogger.printLogOnConsole(3, "File exist" + fileExist);
  }

  public static void closeLogging() {
    JenkinsPlugInLogger.closeLogger();
  }

  /**
   * Flush the output into a file
   *
   * @param outputDirectory Folder name where you want to flush the output to
   * @param fileName File Name
   * @param content Content of the file
   */
  public static void exportOutputToFile(String outputDirectory, String fileName, String content) {
    boolean created = true;
    File directory = new File(outputDirectory);
    if (!directory.exists()) {
      created = directory.mkdirs();
    }
    if (created) {
      File outputFile = new File(outputDirectory + File.separator + fileName);
      BufferedWriter bw = null;
      try {
        bw = new BufferedWriter(new FileWriter(outputFile));
        bw.write(content);
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error in writing content to file " + fileName, e);
      } finally {
        try {
          if (bw != null) bw.close();
        } catch (IOException e) {
          JenkinsPlugInLogger.severe("Error in writing content to file " + fileName, e);
        }
      }
    } else {
      JenkinsPlugInLogger.severe(
          "Cannot create the " + fileName + "  in folder " + outputDirectory);
    }
  }

  /**
   * This utility is used to unzip any file and extract it to a given directory
   *
   * @param zipFilePath Complete File path of the Zip File
   * @param destDir The destination directory where the zip has to be extracted
   */
  public static void unzip(String zipFilePath, String destDir) {
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(zipFilePath);
      FileSystem fileSystem = FileSystems.getDefault();
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          Files.createDirectories(fileSystem.getPath(destDir + entry.getName()));
        } else {
          InputStream is = zipFile.getInputStream(entry);
          BufferedInputStream bis = new BufferedInputStream(is);
          String uncompressedFileName = destDir + entry.getName();
          Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
          Files.createFile(uncompressedFilePath);
          FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
          while (bis.available() > 0) {
            fileOutput.write(bis.read());
          }
          fileOutput.close();
        }
      }
    } catch (IOException e) {
      JenkinsPlugInLogger.severe("Error while performing unzip operation ", e);
    } finally {
      try {
        if (zipFile != null) zipFile.close();
      } catch (IOException e) {
        JenkinsPlugInLogger.severe("Error while performing unzip operation ", e);
      }
    }
  }
}
