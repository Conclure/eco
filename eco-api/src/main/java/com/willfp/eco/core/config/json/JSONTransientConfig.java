package com.willfp.eco.core.config.json;

import com.willfp.eco.core.Eco;
import com.willfp.eco.core.config.json.wrapper.JSONConfigWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Raw JSON config with a map of values at its core.
 */
public class JSONTransientConfig extends JSONConfigWrapper {
    /**
     * Config implementation for passing maps.
     * <p>
     * Does not automatically update.
     *
     * @param values The map of values.
     */
    public JSONTransientConfig(@NotNull final Map<String, Object> values) {
        super(Eco.getHandler().getConfigFactory().createJSONConfig(values));
    }
}
