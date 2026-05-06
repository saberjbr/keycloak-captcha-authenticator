package com.openai.keycloak.captcha;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public final class CaptchaAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "custom-captcha-authenticator";
    static final String CFG_LENGTH = "captcha.length";
    static final String CFG_WIDTH = "captcha.width";
    static final String CFG_HEIGHT = "captcha.height";
    static final String CFG_NOISE_LINES = "captcha.noiseLines";
    static final String CFG_TTL_SECONDS = "captcha.ttlSeconds";
    static final String CFG_CASE_SENSITIVE = "captcha.caseSensitive";

    private static final CaptchaAuthenticator SINGLETON = new CaptchaAuthenticator();
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENTS = new AuthenticationExecutionModel.Requirement[] {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    private static final List<ProviderConfigProperty> CONFIG = List.of(
            property(CFG_LENGTH, "Captcha length", "Number of characters to generate.", ProviderConfigProperty.STRING_TYPE, "6"),
            property(CFG_WIDTH, "Image width", "Captcha image width in pixels.", ProviderConfigProperty.STRING_TYPE, "220"),
            property(CFG_HEIGHT, "Image height", "Captcha image height in pixels.", ProviderConfigProperty.STRING_TYPE, "80"),
            property(CFG_NOISE_LINES, "Noise lines", "Number of random noise lines drawn on the image.", ProviderConfigProperty.STRING_TYPE, "6"),
            property(CFG_TTL_SECONDS, "TTL seconds", "How long a captcha remains valid.", ProviderConfigProperty.STRING_TYPE, "300"),
            property(CFG_CASE_SENSITIVE, "Case sensitive", "Whether the submitted value must match letter casing exactly.", ProviderConfigProperty.BOOLEAN_TYPE, "false"));

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Image CAPTCHA";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENTS;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Adds a separate CAPTCHA challenge step to a browser authentication flow.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        // No-op.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No-op.
    }

    @Override
    public void close() {
        // No-op.
    }

    private static ProviderConfigProperty property(String name, String label, String helpText, String type, String defaultValue) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(name);
        property.setLabel(label);
        property.setHelpText(helpText);
        property.setType(type);
        property.setDefaultValue(defaultValue);
        return property;
    }
}
