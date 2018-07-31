import com.ks.exceptions.RuleLoadingException;
import com.ks.loaders.ClasspathZipRuleFileLoader;
import com.ks.pojo.RuleFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.TestClass;

import static junit.framework.TestCase.assertTrue;

public class ClassPathRuleLoaderTest {


    private ClasspathZipRuleFileLoader classpathZipRuleFileLoader;

    @Before
    public void setUp(){
        classpathZipRuleFileLoader = new ClasspathZipRuleFileLoader();
        classpathZipRuleFileLoader.setPath("");
    }

    @Test
    public void testZipRuleLoad() throws RuleLoadingException {
        System.out.println(System.getProperty("user.dir"));
        RuleFile[] results = classpathZipRuleFileLoader.loadRuleFiles();
        assertTrue(results.length > 0);
    }
}
