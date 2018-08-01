import com.ks.DefaultAttackLogger;
import com.ks.exceptions.RuleLoadingException;
import com.ks.loaders.ClasspathZipRuleFileLoader;
import com.ks.pojo.RuleFile;
import org.junit.Before;
import org.junit.Test;

public class ClassPathRuleLoaderTest {


    private ClasspathZipRuleFileLoader classpathZipRuleFileLoader;
    private DefaultAttackLogger attackLogger;

    @Before
    public void setUp(){
        classpathZipRuleFileLoader = new ClasspathZipRuleFileLoader();
        classpathZipRuleFileLoader.setPath("");
        attackLogger = new DefaultAttackLogger();
    }

    @Test
    public void testZipRuleLoad() throws RuleLoadingException {
        System.out.println(System.getProperty("user.dir"));
        RuleFile[] results = classpathZipRuleFileLoader.loadRuleFiles();
        //assertTrue(results.length > 0);
    }

    @Test
    public void testEnabledSeccurityLogging() throws RuleLoadingException {
        attackLogger.init("test",false,true);
    }
}
