package org.elasticsearch.plugin.filter;

import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.filter.FilterHandler;

public class FilterHandlerPlugin extends AbstractPlugin {

    public String name() {
        return "Authentication Filter";
    }

    public String description() {
        return "Authorizes user for requested index";
    }

    @Override public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(FilterHandler.class);
        }
    }
}
