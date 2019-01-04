package com.ca.apm.jenkins.core.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.ca.apm.jenkins.core.logging.JenkinsPlugInLogger;

/**
 * Utility for class loading
 * 
 * @author Avinash Chandwani
 *
 */

public class IOUtility {

	private Map<String, String> ioProperties;
	private URLClassLoader classLoader;

	public IOUtility() {
		super();
	}

	public void addToIOProperties(String key, String value) {
		if (ioProperties == null) {
			ioProperties = new HashMap<String, String>();
		}
		ioProperties.put(key, value);
	}

	public String getIOPropertyValue(String key) {
		return ioProperties.get(key);
	}

	public void closeClassLoader() {
		if (classLoader != null)
			try {
				classLoader.close();
			} catch (IOException e) {
				JenkinsPlugInLogger.severe("Error in closing class-loader ", e);
			}
	}

	public Class<?> findClass(String className) throws ClassNotFoundException {
		Class<?> pluginClass = null;
		String extensionsDirectory = ioProperties.get("extensions.directory");
		try {
			pluginClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			try {
				if (extensionsDirectory != null)
					pluginClass = Class.forName(className, true, classLoader);
			} catch (ClassNotFoundException e1) {
				JenkinsPlugInLogger.severe(className + " Class Not found", e1);
				throw new ClassNotFoundException("Class " + className + " not found");
			}
		}
		return pluginClass;
	}

	public void extractZipFromClassPath(String zipFileName, File targetFolder) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		ZipInputStream zipInputStream = new ZipInputStream(classLoader.getResourceAsStream(zipFileName));
		ZipEntry entry = null;
		try {
			entry = zipInputStream.getNextEntry();
			while (entry != null) {
				String filePath = targetFolder + File.separator + entry.getName();
				if (!entry.isDirectory()) {
					extractFile(zipInputStream, filePath);
				} else {
					File dir = new File(filePath);
					dir.mkdir();
				}
				zipInputStream.closeEntry();
				entry = zipInputStream.getNextEntry();
				
			}
		} catch (IOException e1) {
			JenkinsPlugInLogger.severe("Error while copying the zip files");
		}finally{
			try {
				zipInputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static final int BUFFER_SIZE = 4096;

	private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}

	public void loadExtensionsLibraries() {
		String extensionsDirectory = ioProperties.get("extensions.directory");
		if (extensionsDirectory == null) {
			JenkinsPlugInLogger.info("No jar file(s) found in the extensions directory specified in configuration");
			return;
		}
		File extensionsFolder = new File(extensionsDirectory);
		if (!extensionsFolder.exists()) {
			return;
		}
		File[] jarFiles = extensionsFolder.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("jar");
			}
		});
		if (jarFiles.length == 0) {
			return;
		}
		URL[] jarUrls = new URL[jarFiles.length];

		int i = 0;
		for (File jar : jarFiles) {
			try {
				jarUrls[i++] = jar.toURI().toURL();
			} catch (MalformedURLException e) {
			}
		}
		classLoader = new URLClassLoader(jarUrls, this.getClass().getClassLoader());
	}
}