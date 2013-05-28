package de.skiptag.roadrunner.coyote;

import org.mozilla.javascript.NativeObject;

import de.skiptag.coyote.api.Coyote;
import de.skiptag.coyote.api.modules.Module;
import de.skiptag.coyote.api.modules.ModuleActivator;

public class RoadrunnerActivator implements ModuleActivator {

	@Override
	public Module start(Coyote coyote, NativeObject nativeObject) {
		return new RoadrunnerModule(coyote, (String) nativeObject.get("path"),
				(String) nativeObject.get("repoName"));
	}

}
