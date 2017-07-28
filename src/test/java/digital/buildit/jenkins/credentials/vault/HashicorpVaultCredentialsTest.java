package digital.buildit.jenkins.credentials.vault;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.datapipe.jenkins.vault.configuration.GlobalVaultConfiguration;
import com.datapipe.jenkins.vault.configuration.VaultConfiguration;
import com.datapipe.jenkins.vault.credentials.VaultAppRoleCredential;
import com.datapipe.jenkins.vault.credentials.VaultTokenCredential;
import hudson.model.Label;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static utilities.HttpJsonServer.startServer;
import static utilities.ReadFromResources.readFromResources;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static utilities.ResourcePath.resourcePath;


public class HashicorpVaultCredentialsTest {

    private static final String VAULT_ADDRESS = "http://localhost:4567";
    private static final String GLOBAL_CREDENTIALS_ID_1 = "global-1";

    private Credentials GLOBAL_CREDENTIAL_1;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    public Server server;

    @Before
    public void setup() throws Exception {
        jenkinsRule.createOnlineSlave(Label.get("slaves"));

        GLOBAL_CREDENTIAL_1 = createTokenCredential(GLOBAL_CREDENTIALS_ID_1);

        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Arrays
                .asList(GLOBAL_CREDENTIAL_1)));

        GlobalVaultConfiguration globalConfig = GlobalConfiguration.all().get(GlobalVaultConfiguration.class);
        globalConfig.setConfiguration(new VaultConfiguration(VAULT_ADDRESS, GLOBAL_CREDENTIALS_ID_1));

        globalConfig.save();

        server = startServer(resourcePath("."), 4567);

    }

    @After
    public void teardown() throws Exception {
        server.stop();
    }


    @Test
    public void shouldRetrieveCorrectCredentialsFromVault() throws Exception {

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

    public static Credentials createTokenCredential(final String credentialId) {
        return new VaultTokenCredential(CredentialsScope.GLOBAL, credentialId, "description", Secret.fromString("secret-id-"+credentialId));
    }
}
