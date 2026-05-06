<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('captcha'); section>
    <#if section = "header">
        ${msg("captchaTitle")}
    <#elseif section = "form">
        <form id="kc-captcha-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="${captchaFieldName}" class="${properties.kcLabelClass!}">${msg("captchaInstruction")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div style="margin-bottom: 1rem; text-align: center;">
                        <img src="${captchaImage}" alt="CAPTCHA" style="max-width: 100%; border-radius: 6px; border: 1px solid #d1d7e0;" />
                    </div>
                    <input id="${captchaFieldName}" name="${captchaFieldName}" type="text" autocomplete="off" class="${properties.kcInputClass!}" autofocus />
                </div>
            </div>

            <#if captchaError?? && captchaError?has_content>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcInputWrapperClass!}">
                        <span class="pf-v5-c-helper-text pf-m-error">${captchaError}</span>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" type="submit">
                        ${msg("doContinue")}
                    </button>
                    <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!}" type="submit" name="${captchaRefreshField}" value="true">
                        ${msg("captchaRefresh")}
                    </button>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
