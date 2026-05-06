# Keycloak CAPTCHA Authenticator JAR

This JAR adds a custom **Image CAPTCHA** authenticator to the Keycloak browser authentication flow.

## Target
- Built against the current Keycloak 26.6.1 SPI surface
- Compiled for Java 21

## Docker usage

```dockerfile
FROM quay.io/keycloak/keycloak:26.6.1

COPY keycloak-captcha-authenticator-26.6.1.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build
```

Then start Keycloak normally.

## Enable in Keycloak Admin Console
1. Go to **Authentication** → **Flows**.
2. Copy the built-in **browser** flow to a custom flow.
3. In the copied flow, add an execution of type **Image CAPTCHA**.
4. Move it to the place you want, usually:
   - before **Username Password Form** to block bots early, or
   - after **Username Password Form** if you only want CAPTCHA after credentials.
5. Set the execution requirement to **REQUIRED**.
6. Go to **Authentication** → **Bindings** and bind your custom browser flow.

## Configurable properties
- `captcha.length` default `6`
- `captcha.width` default `220`
- `captcha.height` default `80`
- `captcha.noiseLines` default `6`
- `captcha.ttlSeconds` default `300`
- `captcha.caseSensitive` default `false`

## Notes
- The challenge is a separate login step.
- The CAPTCHA image is generated server-side and embedded directly in the form.
- No external CAPTCHA service is required.
