package org.eclipse.core.tests.resources.saveparticipant;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
/**
 * @see SaveManager1Test
 * @see SaveManager3Test
 */
public class SaveManager2Test extends SaveManagerTest {
/**
 * Need a zero argument constructor to satisfy the test harness.
 * This constructor should not do any real work nor should it be
 * called by user code.
 */
public SaveManager2Test() {
}
public SaveManager2Test(String name) {
	super(name);
}
public static Test suite() {
	// we do not add the whole class because the order is important
	TestSuite suite = new TestSuite();
	suite.addTest(new SaveManager2Test("testVerifyRestoredWorkspace"));
	suite.addTest(new SaveManager2Test("testBuilder"));
	suite.addTest(new SaveManager2Test("testSaveParticipant"));
	suite.addTest(new SaveManager2Test("testVerifyProject2"));
	suite.addTest(new SaveManager1Test("testSaveWorkspace"));
	return suite;
}
public void testBuilder() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	assertTrue("0.0", project.isAccessible());

	IFile added = project.getFile("added file");
	DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
	verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
	try {
		added.create(getRandomContents(), true, null);
	} catch (CoreException e) {
		fail("3.1", e);
	}
	assertTrue("3.2", verifier.wasAutoBuild());
	assertTrue("3.3", verifier.isDeltaValid());
	// remove the file because we don't want it to affect any other delta in the test
	try {
		added.delete(true, false, null);
	} catch (CoreException e) {
		fail("3.4", e);
	}
}
public void testSaveParticipant() {
	// get plugin
	IPluginDescriptor descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_1);
	SaveParticipant1Plugin plugin1 = null;
	try {
		plugin1 = (SaveParticipant1Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("0.0", e);
	}
	assertTrue("0.1", plugin1 != null);

	// prepare plugin to the save operation
	plugin1.resetDeltaVerifier();
	IResource added1 = getWorkspace().getRoot().getFile(new Path(PROJECT_1).append("addedFile"));
	plugin1.addExpectedChange(added1, IResourceDelta.ADDED, 0);
	IStatus status;
	try {
		status = plugin1.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			assertTrue("1.0", false);
		}
	} catch (CoreException e) {
		fail("1.1", e);
	}

	// SaveParticipant3Plugin
	descriptor = Platform.getPluginRegistry().getPluginDescriptor(PI_SAVE_PARTICIPANT_3);
	SaveParticipant3Plugin plugin3 = null;
	try {
		plugin3 = (SaveParticipant3Plugin) descriptor.getPlugin();
	} catch (CoreException e) {
		fail("7.0", e);
	}
	assertTrue("7.1", plugin3 != null);
	try {
		status = plugin3.registerAsSaveParticipant();
		if (!status.isOK()) {
			System.out.println(status.getMessage());
			fail("7.2");
		}
	} catch (CoreException e) {
		fail("7.3", e);
	}
}
public void testVerifyProject2() {
	// project2 should be closed
	IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
	assertTrue("0.0", project.exists());
	assertTrue("0.1", !project.isOpen());

	// verify its children
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
	assertExistsInFileSystem("1.0", resources);
	assertDoesNotExistInWorkspace("1.1", resources);

	try {
		project.open(null);
	} catch (CoreException e) {
		fail("2.0", e);
	}
	assertTrue("2.1", project.exists());
	assertTrue("2.2", project.isOpen());
	assertExistsInFileSystem("2.3", resources);
	assertExistsInWorkspace("2.4", resources);

	// verify builder -- cause an incremental build
	try {
		touch(project);
	} catch (CoreException e) {
		fail("2.5", e);
	}
	SimpleBuilder builder = SimpleBuilder.getInstance();
	assertTrue("2.6", builder.wasAutoBuild());

	// add a file to test save participant delta
	IFile file = project.getFile("addedFile");
	try {
		file.create(getRandomContents(), true, null);
	} catch (CoreException e) {
		fail("3.0", e);
	}
}
public void testVerifyRestoredWorkspace() {
	IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
	assertTrue("0.0", project.exists());
	assertTrue("0.1", project.isOpen());

	// verify children still exist
	IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
	assertExistsInFileSystem("1.0", resources);
	assertExistsInWorkspace("1.1", resources);

	// add a file to test save participant delta
	IFile file = project.getFile("addedFile");
	try {
		file.create(getRandomContents(), true, null);
	} catch (CoreException e) {
		fail("2.0", e);
	}
}
}
