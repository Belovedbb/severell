package com.severell.core.view;

import com.severell.core.config.Config;
import com.severell.core.container.Container;
import com.severell.core.providers.ServiceProvider;

/**
 * The ViewProvider is used to setup the templating functionality and put it into
 * the Container.
 */
public class ViewProvider extends ServiceProvider {

    public ViewProvider(Container c) {
        super(c);
    }

    @Override
    public void register() {
        this.c.bind("ViewMustacheDriver", (container) -> new ViewMustacheDriver(container));
        this.c.singleton(ViewManager.class, new ViewManager(this.c));
    }

    @Override
    public void boot() throws Exception {
        this.c.singleton(View.class, c.make(ViewManager.class).create_driver(Config.get("VIEW_DRIVER", "Mustache")));
    }
}
