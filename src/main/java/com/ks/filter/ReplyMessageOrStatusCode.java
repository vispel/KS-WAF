package com.ks.filter;

import com.ks.KsWafFilter;
import com.ks.config.ConfigurationManager;

import javax.servlet.UnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.ks.utils.ParamConsts.PARAM_CONFIG_MISSING_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE;

public class ReplyMessageOrStatusCode {

    private String configurationMissingReplyMessage;
    private String attackReplyMessage;
    private String exceptionReplyMessage;
    private int attackReplyStatusCode = 200;
    private int configurationMissingReplyStatusCode = 503;
    private int exceptionReplyStatusCode = 503;

    public String getConfigurationMissingReplyMessage() {
        return configurationMissingReplyMessage;
    }

    public void setConfigurationMissingReplyMessage(String configurationMissingReplyMessage) {
        this.configurationMissingReplyMessage = configurationMissingReplyMessage;
    }

    public String getAttackReplyMessage() {
        return attackReplyMessage;
    }

    public void setAttackReplyMessage(String attackReplyMessage) {
        this.attackReplyMessage = attackReplyMessage;
    }

    public String getExceptionReplyMessage() {
        return exceptionReplyMessage;
    }

    public void setExceptionReplyMessage(String exceptionReplyMessage) {
        this.exceptionReplyMessage = exceptionReplyMessage;
    }

    public int getAttackReplyStatusCode() {
        return attackReplyStatusCode;
    }

    public void setAttackReplyStatusCode(int attackReplyStatusCode) {
        this.attackReplyStatusCode = attackReplyStatusCode;
    }

    public int getConfigurationMissingReplyStatusCode() {
        return configurationMissingReplyStatusCode;
    }

    public void setConfigurationMissingReplyStatusCode(int configurationMissingReplyStatusCode) {
        this.configurationMissingReplyStatusCode = configurationMissingReplyStatusCode;
    }

    public int getExceptionReplyStatusCode() {
        return exceptionReplyStatusCode;
    }

    public void setExceptionReplyStatusCode(int exceptionReplyStatusCode) {
        this.exceptionReplyStatusCode = exceptionReplyStatusCode;
    }

    public void loadConfigurationMissingReplyConfig(ConfigurationManager configManager) throws UnavailableException {
        String configurationMissingReplyConfigValue = configManager.getConfigurationValue(PARAM_CONFIG_MISSING_REPLY_STATUS_CODE_OR_MESSAGE_RESOURCE);
        if (configurationMissingReplyConfigValue == null)
            configurationMissingReplyConfigValue = "503";
        try {
            this.configurationMissingReplyStatusCode = Integer.parseInt(configurationMissingReplyConfigValue.trim());
            if (this.configurationMissingReplyStatusCode < 0)
                throw new UnavailableException("Configured HTTP status code to send as reply to missing configuration (in production mode) must not be negative: " + configurationMissingReplyConfigValue);
        } catch (NumberFormatException e) {
            // treat as file pointer into classpath instead of treating it as a status code
            final InputStream input = KsWafFilter.class.getClassLoader().getResourceAsStream(configurationMissingReplyConfigValue);
            if (input == null)
                throw new UnavailableException("Unable to number-parse configured HTTP status code to send as reply to missing configuration (in production mode) as well as unable to locate a resource in classpath with name: " + configurationMissingReplyConfigValue);
            BufferedReader buffer = null;
            try {
                buffer = new BufferedReader(new InputStreamReader(input));
                final StringBuilder content = new StringBuilder();
                String line;
                while ((line = buffer.readLine()) != null) {
                    content.append(line).append("\n");
                }
                this.configurationMissingReplyMessage = content.toString().trim();
            } catch (Exception ex) {
                throw new UnavailableException("Unable to load content from the specified resource in classpath with name: " + configurationMissingReplyConfigValue);
            } finally {
                if (buffer != null) try {
                    buffer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
