package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipFile;

import org.elasticsearch.ElasticsearchException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;

public class ESIndexAPITest {

	final private ESIndexAPI esIndexAPI;
	final private ContentletIndexAPI contentletIndexAPI;

    public ESIndexAPITest() {
		this.esIndexAPI = APILocator.getESIndexAPI();
		this.contentletIndexAPI = APILocator.getContentletIndexAPI();
	}

	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File tempDir = new File(pathToRepo);
		if(!tempDir.exists()){
			tempDir.mkdirs();
		}
		tempDir.deleteOnExit();
	}

	@After
	public void cleanUp(){
		esIndexAPI.deleteRepository("backup");
	}

	/**
	 * Test creation of a snapshot file with a valid index name
	 * @throws Exception
	 */
	@Test
	public void createSnapshotTest() throws IOException{
		String indexName = getLiveIndex();
		File snapshot = null;
		snapshot = esIndexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
		assertNotNull(snapshot);
		snapshot.delete();
	}

	/**
	 * Creation of a snapshot file using and invalid index, should fail
	 * @throws IOException
	 * @throws ElasticsearchException
	 * @throws IllegalArgumentException
	 * @throws DotStateException
	 */
	@Test(expected = ElasticsearchException.class)
	public void createSnapshotTest_invalidIndex() throws IOException{
		String indexName = "live_xxxxxxxxx";
		esIndexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
	}

	/**
	 * Downloads current live index, deletes it and restores it
	 * The current live index is removed
	 * The new one is uploaded
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws DotDataException
	 */
	@Test
	public void uploadSnapshotTest_fromCurrentLive() throws IOException, InterruptedException, ExecutionException{
		String indexName = getLiveIndex();
		File snapshot = null;
		snapshot = esIndexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
		esIndexAPI.closeIndex(indexName);
		ZipFile file = new ZipFile(snapshot.getAbsolutePath());
		String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File tempDir = new File(pathToRepo);
		boolean response = esIndexAPI.uploadSnapshot(file, tempDir.getAbsolutePath(),true);
		assertTrue(response);
		if(snapshot!=null){
			snapshot.delete();
		}
		esIndexAPI.openIndex(indexName);
	}
	
	/**
	 * Downloads current live index, tries to restores it
	 * The current live index is live so the process should fail and remove all data used
	 */
	@Test
	public void uploadSnapshotTest_onExceptionCleanup(){
		File snapshot = null;
		ZipFile file = null;
		File tempDir = null;
		try{
			String indexName = getLiveIndex();
			
			snapshot = esIndexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
			file = new ZipFile(snapshot.getAbsolutePath());
			String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
			tempDir = new File(pathToRepo);
			esIndexAPI.uploadSnapshot(file, tempDir.getAbsolutePath(),true);
		}catch(Exception e){
			if(file != null && snapshot.exists()){
				fail("Process did not delete zip file");
			}
			if(tempDir != null && snapshot.exists()){
				fail("Process did not delete repository directory");
			}
		}
	}

	/**
	 * Uploading a sample index snapshot file, index live_20161011212551 (on a sample zip file)
	 * The current live index is removed
	 * The new one is uploaded
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void uploadSnapshotTest() throws IOException, InterruptedException, ExecutionException{
		String currentLiveIndex = getLiveIndex();
		esIndexAPI.closeIndex(currentLiveIndex);
		String path = ConfigTestHelper.getPathToTestResource("index.zip");
		ZipFile file = new ZipFile(path);
		String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File tempDir = new File(pathToRepo);
		boolean response = esIndexAPI.uploadSnapshot(file, tempDir.getAbsolutePath(),true);
		assertTrue(response);
		esIndexAPI.closeIndex("live_20161011212551");
		esIndexAPI.openIndex(currentLiveIndex);
		esIndexAPI.delete("live_20161011212551");
	}

	/**
	 * Uploading a sample index snapshot file, should failed as it does not contain
	 * snapshot information
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */

	@Test(expected = ElasticsearchException.class)
	public void uploadSnapshotTest_noSnapshotFound() throws IOException, InterruptedException, ExecutionException{
		String path = ConfigTestHelper.getPathToTestResource("failing-test.zip");
		ZipFile file = new ZipFile(path);
		String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File tempDir = new File(pathToRepo);
		esIndexAPI.uploadSnapshot(file, tempDir.getAbsolutePath(),true);
	}

	/**
	 * Download current live index, and restores it, the index already exists and should fail
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test(expected = ElasticsearchException.class)
	public void uploadSnapshotTest_alreadyExistingIndex() throws IOException, InterruptedException, ExecutionException{
		String indexName = getLiveIndex();
		File snapshot = null;
		snapshot = esIndexAPI.createSnapshot(ESIndexAPI.BACKUP_REPOSITORY, "backup", indexName);
		ZipFile file = new ZipFile(snapshot.getAbsolutePath());
		String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File tempDir = new File(pathToRepo);
		esIndexAPI.uploadSnapshot(file, tempDir.getAbsolutePath(),true);
		if(snapshot!=null){
			snapshot.delete();
		}
	}

	private String getLiveIndex(){
		Set<String> indexesList = esIndexAPI.listIndices();
		for(String index: indexesList){
			if(index.toLowerCase().startsWith(ESContentletIndexAPI.ES_LIVE_INDEX_NAME)){
				return index;
			}
		}

		// In case "live" index is not found, create and activate it
        String timeStamp = String.valueOf( new Date().getTime() );
        String liveIndex = ESContentletIndexAPI.ES_LIVE_INDEX_NAME + "_" + timeStamp;
        try {
        	esIndexAPI.createIndex(liveIndex);
        	contentletIndexAPI.activateIndex(liveIndex);
        	return liveIndex;
        } catch (Exception e) {
        	return null;
        }
	}

	@AfterClass
	public static void cleanUpTests(){
		String pathToRepo = Config.getStringProperty("es.path.repo","test-resources");
		File baseDirectory = new File(pathToRepo);
		File toDelete = baseDirectory.getParentFile();
		com.liferay.util.FileUtil.deltree(toDelete.getAbsolutePath());


	}
}
