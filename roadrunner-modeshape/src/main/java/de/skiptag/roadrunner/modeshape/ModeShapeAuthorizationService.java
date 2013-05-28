package de.skiptag.roadrunner.modeshape;

import javax.jcr.Session;

import org.json.JSONObject;

import de.skiptag.roadrunner.core.authorization.AuthorizationService;
import de.skiptag.roadrunner.core.authorization.RoadrunnerOperation;
import de.skiptag.roadrunner.core.authorization.RulesDataSnapshot;

public class ModeShapeAuthorizationService implements AuthorizationService {

	private String repositoryName;
	private Session commonRepo;

	public ModeShapeAuthorizationService(Session commonRepo,
			String repositoryName) {
		this.commonRepo = commonRepo;
		this.repositoryName = repositoryName;
	}

	@Override
	public void shutdown() {
		commonRepo.logout();
	}

	@Override
	public boolean authorize(RoadrunnerOperation operation, JSONObject auth,
			RulesDataSnapshot root, String path, RulesDataSnapshot newData) {

		return true;
	}

}
