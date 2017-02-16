package digital.buildit.jenkins.credentials.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.datapipe.jenkins.vault.VaultBuildWrapper;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HashicorpVaultCredentialsImpl extends BaseStandardCredentials implements HashicorpVaultCredentials, StandardUsernamePasswordCredentials {

    public static final String DEFAULT_USERNAME_KEY = "username";
    public static final String DEFAULT_PASSWORD_KEY = "password";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(HashicorpVaultCredentialsImpl.class.getName());
    private String key;
    private String usernameKey;
    private String passwordKey;

    @DataBoundConstructor
    public HashicorpVaultCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id,
                                         @CheckForNull String key, @CheckForNull String passwordKey, @CheckForNull String usernameKey, @CheckForNull String description) {
        super(scope, id, description);
        this.key = key;
        this.usernameKey = StringUtils.isEmpty(usernameKey) ? DEFAULT_USERNAME_KEY : usernameKey;
        this.passwordKey = StringUtils.isEmpty(passwordKey) ? DEFAULT_PASSWORD_KEY : passwordKey;
    }

    @Override
    public String getDisplayName() {
        return this.key;
    }

    @NonNull
    @Override
    public Secret getPassword() {
        return Secret.fromString(getValue(this.passwordKey));
    }

    private String getValue(String valueKey) {
        ExtensionList<VaultBuildWrapper.DescriptorImpl> extensionList = Jenkins.getInstance().getExtensionList(VaultBuildWrapper.DescriptorImpl.class);
        VaultBuildWrapper.DescriptorImpl descriptor = extensionList.get(0);

        if (descriptor == null) {
            throw new IllegalStateException("Vault plugin has not been configured.");
        }

        try {

            String url = descriptor.getVaultUrl();
            String token = Secret.toString(descriptor.getAuthToken());

            VaultConfig vaultConfig = new VaultConfig(url, token).build();

            Vault vault = new Vault(vaultConfig);

            LOGGER.log(Level.FINE, "Fetching value " + key + " from vault: " + url);

            Map<String, String> values = vault.logical().read(key).getData();

            return values.get(valueKey);

        } catch (VaultException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public String getUsername() {
        return getValue(this.usernameKey);
    }

    @NonNull
    public String getPasswordKey() {
        return passwordKey;
    }

    public void setPasswordKey(String passwordKey) {
        this.passwordKey = passwordKey;
    }

    @NonNull
    public String getUsernameKey() {
        return usernameKey;
    }

    public void setUsernameKey(String usernameKey) {
        this.usernameKey = usernameKey;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Hashicorp Vault Credentials";
        }
    }
}
