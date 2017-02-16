package digital.buildit.jenkins.credentials.vault;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.datapipe.jenkins.vault.VaultBuildWrapper;
import hudson.ExtensionList;
import hudson.model.Label;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static digital.buildit.jenkins.credentials.vault.utilities.ReadFromResources.readFromResources;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static spark.Spark.get;

public class HashicorpVaultCredentialsTest {

    private static final String VAULT_ADDRESS = "http://localhost:4567";
    private static final String VAULT_TOKEN = "12345678910";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setup() throws Exception {
        jenkinsRule.createOnlineSlave(Label.get("slaves"));

        ExtensionList<VaultBuildWrapper.DescriptorImpl> extensionList = Jenkins.getInstance().getExtensionList(VaultBuildWrapper.DescriptorImpl.class);
        VaultBuildWrapper.DescriptorImpl descriptor = extensionList.get(0);
        descriptor.setVaultUrl(VAULT_ADDRESS);
        descriptor.setAuthToken(VAULT_TOKEN);

        get("/v1/secret/cloudfoundry", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                res.type("application/json");
                return readFromResources("vault-secret-cloudfoundry.json");
            }
        });

        get("/v1/secret/custom", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                res.type("application/json");
                return readFromResources("vault-secret-custom.json");
            }
        });
    }

    @Test
    public void shouldRetrieveCorrectCredentialsFromVault() throws ExecutionException, InterruptedException, IOException {

        CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new HashicorpVaultCredentialsImpl(null, "cloudfoundry", "secret/cloudfoundry", null, null, "Test Credentials"));

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(readFromResources("retrieve-cloudfoundry-credentials-pipeline.groovy"), true));

        WorkflowRun workflowRun = p.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(workflowRun.getLogFile());
        assertThat(log, containsString("spiderman:peterparker"));
    }

    @Test
    public void shouldRetrieveCorrectCredentialsFromVaultWithCustomKeys() throws ExecutionException, InterruptedException, IOException {

        CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new HashicorpVaultCredentialsImpl(null, "custom", "secret/custom", "name", "alias", "Test Credentials"));

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(readFromResources("retrieve-custom-credentials-pipeline.groovy"), true));

        WorkflowRun workflowRun = p.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(workflowRun.getLogFile());
        assertThat(log, containsString("spiderman:peterparker"));
    }

    @Test
    public void shouldFailWhenMissingRequestingMissingCredentialsFromVault() throws ExecutionException, InterruptedException, IOException {

        CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), new HashicorpVaultCredentialsImpl(null, "cloudfoundry", "does/not/exist", null, null, "Test Credentials"));

        WorkflowJob p = jenkinsRule.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(readFromResources("retrieve-cloudfoundry-credentials-pipeline.groovy"), true));

        WorkflowRun workflowRun = p.scheduleBuild2(0).get();
        String log = FileUtils.readFileToString(workflowRun.getLogFile());
        assertThat(log, containsString("Vault responded with HTTP status code: 404"));
        assertThat(log, containsString("Finished: FAILURE"));
    }
}
