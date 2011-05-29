package org.jdownloader.extensions.webinterfaceng;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.jdownloader.settings.AboutConfig;
import org.jdownloader.settings.RangeValidatorMarker;

public interface WebinterfaceNgConfig extends ExtensionConfigInterface {

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isUseRefresh();

    @DefaultIntValue(5)
    @AboutConfig
    @RangeValidatorMarker(range = { 5, 60 })
    int getRefreshInterval();

    @DefaultIntValue(8765)
    @AboutConfig
    @RangeValidatorMarker(range = { 1, 65000 })
    int getPort();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isUseLocalhostOnly();

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isUseLogin();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isUseHttps();

    @DefaultStringValue("JD")
    @AboutConfig
    String getUser();

    @DefaultStringValue("JD")
    @AboutConfig
    String getPassword();

    void setUseRefresh(boolean value);

    void setRefreshInterval(int value);

    void setPort(int value);

    void setUseLocalhostOnly(boolean value);

    void setUseLogin(boolean value);

    void setUseHttps(boolean value);

    void setUser(String value);

    void setPassword(String value);

}
