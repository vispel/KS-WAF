package com.ks.loaders;

import com.ks.config.ConfigurationManager;
import com.ks.exceptions.FilterConfigurationException;
import com.ks.exceptions.RuleLoadingException;
import com.ks.pojo.RuleFile;
import com.ks.utils.ConfigurationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.FilterConfig;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ClasspathZipRuleFileLoader extends AbstractFilebasedRuleFileLoader implements RuleFileLoader {
	private String classpathReference = "rules.zip";

	public void setFilterConfig(FilterConfig filterConfig)
			throws FilterConfigurationException
	{
		super.setFilterConfig(filterConfig);
		ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
		this.classpathReference = ConfigurationUtils.extractOptionalConfigValue(configManager, "RuleFilesClasspathReference", "com/ks/rules.zip");
	}

	public RuleFile[] loadRuleFiles() throws RuleLoadingException {
        if (this.classpathReference == null) throw new IllegalStateException("FilterConfig must be set before loading rules files");
        if (this.path == null) throw new IllegalStateException("Path must be set before loading rules files");
        ZipInputStream zipper = null;
        try {
            final List rules = new ArrayList();
         /*   final InputStream input = getClass().getClassLoader().getResourceAsStream(this.classpathReference);
            if (input == null) throw new FileNotFoundException("Unable to locate zipped rule file on classpath: "+this.classpathReference);
*/
            String path = getClass().getClassLoader().getResource(this.classpathReference).toURI().getPath();
            ZipFile zipFile = new ZipFile(path);
            /*zipper = new ZipInputStream(new BufferedInputStream(input));
            FileUtils.copyInputStreamToFile(input,zip);

            for (File dir : zip.listFiles()){
                if(dir.canRead()){
                    for(String ruleName: dir.list()) {
                        createRuleFile(zipper, rules, ruleName);
                    }
                }
            }*/










            return (RuleFile[])rules.toArray(new RuleFile[0]);
        } catch (Exception e) {
            throw new RuleLoadingException(e);
        } finally {
            if (zipper != null) try { zipper.close(); } catch(IOException ignored) {}
        }
	}

    private void createRuleFile(ZipInputStream zipper, List rules, String name) throws IOException {
        if (name != null && isMatchingSuffix(name)) {
            // remove leading slash if there is one
            if (name.startsWith("/") && name.length()>1) name = name.substring(1);
            if (name.startsWith(this.path)) { //= OK, we've got a relevant file here
                final Properties properties = new Properties();
                properties.load(zipper);
                rules.add( new RuleFile(name,properties) );
            }
        }
    }
}
