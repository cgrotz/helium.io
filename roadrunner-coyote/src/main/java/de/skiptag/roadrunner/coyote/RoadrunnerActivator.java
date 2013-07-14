package de.skiptag.roadrunner.coyote;

import org.json.JSONException;
import org.mozilla.javascript.NativeObject;

import com.google.common.base.Preconditions;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.modules.Module;
import de.skiptag.coyote.api.modules.ModuleActivator;

public class RoadrunnerActivator implements ModuleActivator {

    @Override
    public Module start(Coyote coyote, NativeObject config) {
	try {

	    String moduleWebPath = (String) Preconditions.checkNotNull(config.get("moduleWebPath"), "moduleWebPath must be defined");
	    String journalDirectory = (String) Preconditions.checkNotNull(config.get("journalDirectory"), "journalDirectory must be defined");
	    NativeObject rule = (NativeObject) Preconditions.checkNotNull(config.get("rule"), "rule must be defined");
	    return new RoadrunnerModule(coyote, moduleWebPath,
		    journalDirectory, rule);
	} catch (JSONException e) {
	    throw new RuntimeException(e);
	}
    }

}
