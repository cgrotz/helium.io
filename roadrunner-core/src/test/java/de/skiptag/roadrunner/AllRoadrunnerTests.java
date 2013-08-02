package de.skiptag.roadrunner;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ PersistenceProcessorTest.class, PersistenceTest.class,
	RoadrunnerSenderTest.class })
public class AllRoadrunnerTests {

}
