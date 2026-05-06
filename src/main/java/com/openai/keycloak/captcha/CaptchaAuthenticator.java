package com.openai.keycloak.captcha;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public final class CaptchaAuthenticator implements Authenticator {
    static final String FORM_FIELD = "captcha";
    static final String REFRESH_FIELD = "refreshCaptcha";
    static final String NOTE_EXPECTED = "customCaptcha.expected";
    static final String NOTE_ISSUED_AT = "customCaptcha.issuedAt";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.challenge(buildChallenge(context, null, false));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData != null && formData.getFirst(REFRESH_FIELD) != null) {
            context.challenge(buildChallenge(context, null, true));
            return;
        }

        String providedValue = firstOrEmpty(formData, FORM_FIELD).trim();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String expected = authSession.getAuthNote(NOTE_EXPECTED);
        String issuedAtRaw = authSession.getAuthNote(NOTE_ISSUED_AT);

        if (expected == null || issuedAtRaw == null) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    buildChallenge(context, "The CAPTCHA expired. Try again.", true));
            return;
        }

        long issuedAtEpochSeconds;
        try {
            issuedAtEpochSeconds = Long.parseLong(issuedAtRaw);
        } catch (NumberFormatException ex) {
            context.failureChallenge(
                    AuthenticationFlowError.INTERNAL_ERROR,
                    buildChallenge(context, "The CAPTCHA state was invalid. A new challenge was created.", true));
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        if ((now - issuedAtEpochSeconds) > getTtlSeconds(context)) {
            clearCaptcha(authSession);
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    buildChallenge(context, "The CAPTCHA expired. Try again.", true));
            return;
        }

        if (providedValue.isBlank()) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    buildChallenge(context, "Enter the characters from the image.", false));
            return;
        }

        String normalizedProvided = normalize(providedValue, isCaseSensitive(context));
        String normalizedExpected = normalize(expected, isCaseSensitive(context));
        if (!normalizedExpected.equals(normalizedProvided)) {
            context.failureChallenge(
                    AuthenticationFlowError.INVALID_CREDENTIALS,
                    buildChallenge(context, "The CAPTCHA did not match. Try again.", true));
            return;
        }

        clearCaptcha(authSession);
        context.success();
    }

    private Response buildChallenge(AuthenticationFlowContext context, String errorText, boolean forceRegenerate) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        Map<String, String> values = config == null ? Map.of() : config.getConfig();

        int length = getInt(values, CaptchaAuthenticatorFactory.CFG_LENGTH, 6, 4, 10);
        int width = getInt(values, CaptchaAuthenticatorFactory.CFG_WIDTH, 220, 160, 400);
        int height = getInt(values, CaptchaAuthenticatorFactory.CFG_HEIGHT, 80, 60, 160);
        int noiseLines = getInt(values, CaptchaAuthenticatorFactory.CFG_NOISE_LINES, 6, 0, 30);

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String text = authSession.getAuthNote(NOTE_EXPECTED);
        if (forceRegenerate || text == null || text.isBlank()) {
            text = CaptchaGenerator.randomText(length);
            authSession.setAuthNote(NOTE_EXPECTED, text);
            authSession.setAuthNote(NOTE_ISSUED_AT, Long.toString(System.currentTimeMillis() / 1000L));
        }

        String imageDataUrl = CaptchaGenerator.renderAsDataUrl(text, width, height, noiseLines);

        LoginFormsProvider form = context.form();
        form.setAttribute("captchaImage", imageDataUrl);
        form.setAttribute("captchaFieldName", FORM_FIELD);
        form.setAttribute("captchaRefreshField", REFRESH_FIELD);
        form.setAttribute("captchaError", errorText);
        return form.createForm("captcha-login.ftl");
    }

    private static void clearCaptcha(AuthenticationSessionModel authSession) {
        authSession.removeAuthNote(NOTE_EXPECTED);
        authSession.removeAuthNote(NOTE_ISSUED_AT);
    }

    private static String firstOrEmpty(MultivaluedMap<String, String> formData, String key) {
        if (formData == null) {
            return "";
        }
        String value = formData.getFirst(key);
        return value == null ? "" : value;
    }

    private static String normalize(String value, boolean caseSensitive) {
        String trimmed = value == null ? "" : value.trim();
        return caseSensitive ? trimmed : trimmed.toUpperCase(Locale.ROOT);
    }

    private static int getTtlSeconds(AuthenticationFlowContext context) {
        Map<String, String> values = getConfigMap(context);
        return getInt(values, CaptchaAuthenticatorFactory.CFG_TTL_SECONDS, 300, 30, 3600);
    }

    private static boolean isCaseSensitive(AuthenticationFlowContext context) {
        Map<String, String> values = getConfigMap(context);
        return Boolean.parseBoolean(values.getOrDefault(CaptchaAuthenticatorFactory.CFG_CASE_SENSITIVE, "false"));
    }

    private static Map<String, String> getConfigMap(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        return config == null || config.getConfig() == null ? Map.of() : config.getConfig();
    }

    private static int getInt(Map<String, String> values, String key, int defaultValue, int min, int max) {
        try {
            int parsed = Integer.parseInt(values.getOrDefault(key, Integer.toString(defaultValue)).trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No-op.
    }

    @Override
    public void close() {
        // No-op.
    }
}
