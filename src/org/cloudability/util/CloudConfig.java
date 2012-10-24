/**
 * Copyright (C) 2012  Lipu Fei
 */
package org.cloudability.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * It parses cloud configuration file and creates a corresponding hash map.
 * The configuration file consists of several lines in the form of "p=v". This
 * class performs strict check on syntax, the constraints include: each line is
 * in the form "parameter=value"; a parameter name can only appear once; neither
 * parameter name nor value can be empty.
 *
 * @author Lipu Fei
 * @version 0.1
 *
 */
public class CloudConfig {

	/**
	 * Parse the configuration file.
	 * @param filePath The path to the configuration file.
	 */
	public static HashMap<String, String> parseFile(String filePath)
				throws CloudConfigException {
		HashMap<String, String> configMap = new HashMap<String, String>();
		try {
			BufferedReader reader =
					new BufferedReader(new FileReader(filePath));

			/* parse lines */
			String line = "";
			while ((line = reader.readLine()) != null) {

				line = line.trim();
				/* skip comment lines and empty lines */
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				String[] params = line.split("=");
				/* check */
				if (params.length != 2) {
					reader.close();

					/* log and throw exception */
					String error = "Invalid cloud config line: " + line;
					throw new CloudConfigException(error);
				}

				params[0] = params[0].trim();
				params[1] = params[1].trim();
				/* check if parameter already exists */
				if (configMap.containsKey(params[0])) {
					reader.close();

					/* log and throw exception */
					String error = "Parameter already exists: " + line;
					throw new CloudConfigException(error);
				}
				/* check if parameter is empty */
				if (params[0].isEmpty()) {
					reader.close();

					/* log and throw exception */
					String error = "empty parameter or value: " + line;
					throw new CloudConfigException(error);
				}

				/* save to hash map */
				configMap.put(params[0], params[1]);
			}

			reader.close();
		} catch (FileNotFoundException e) {
			/* log and throw exception */
			String error = "Cloud config file not found: " + filePath;
			throw new CloudConfigException(error);
		} catch (IOException e) {
			/* log and throw exception */
			String error = "Cloud config file IO error.";
			throw new CloudConfigException(error);
		}

		return configMap;
	}

}
