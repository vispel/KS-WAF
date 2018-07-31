package com.ks.loaders;

import com.ks.config.ConfigurationManager;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.exceptions.RuleLoadingException;
import com.ks.pojo.RuleFile;
import com.ks.utils.ConfigurationUtils;

import javax.servlet.FilterConfig;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FileSystemRuleFileLoader extends AbstractFilebasedRuleFileLoader{

	public static final String PARAM_RULE_FILES_BASE_PATH = "RuleFilesBasePath";
	private String base;

	public void setFilterConfig(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		super.setFilterConfig(filterConfig);
		ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
		this.base = ConfigurationUtils.extractMandatoryConfigValue(configManager, "RuleFilesBasePath");
	}

	public RuleFile[] loadRuleFiles()
			throws RuleLoadingException
	{
		if (this.base == null) {
			throw new IllegalStateException("FilterConfig must be set before loading rules files");
		}
		if (this.path == null) {
			throw new IllegalStateException("Path must be set before loading rules files");
		}
		try
		{
			File directory = new File(this.base, this.path);
			if (!directory.exists()) {
				throw new IllegalArgumentException("Directory does not exist: " + directory.getAbsolutePath());
			}
			if (!directory.isDirectory()) {
				throw new IllegalArgumentException("Directory exists but is not a directory (maybe just a file?): " + directory.getAbsolutePath());
			}
			List rules = new ArrayList();
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length;)
			{
				File file = files[i];
				if ((file.isFile()) && (isMatchingSuffix(file.getName())))
				{
					if (!file.canRead()) {
						throw new FileNotFoundException("Unable to read rule definition file: " + file.getAbsolutePath());
					}
					Properties properties = new Properties();
                    try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
                        properties.load(input);
                        rules.add(new RuleFile(file.getAbsolutePath(), properties));
                        if (input != null) {
                            try {
                                input.close();
                            } catch (IOException ignored) {
                            }
                        }
                        i++;
                    }
				}
			}
			return (RuleFile[])rules.toArray(new RuleFile[0]);
		}
		catch (Exception e)
		{
			throw new RuleLoadingException(e);
		}
	}

}
