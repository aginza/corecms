package com.dotcms.keyvalue.business;

import static com.dotcms.integrationtestutil.content.ContentUtils.createTestKeyValueContent;
import static com.dotcms.integrationtestutil.content.ContentUtils.deleteContentlets;
import static com.dotcms.integrationtestutil.content.ContentUtils.updateTestKeyValueContent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.dotmarketing.util.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cache.KeyValueCache;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This Integration Test will verify the correct and expected behavior of the {@link KeyValueAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 21, 2017
 *
 */
public class KeyValueAPITest extends IntegrationTestBase {

    private static ContentType keyValueContentType;
    private static User systemUser;

    private static long englishLanguageId;
    private static long spanishLanguageId;

    private static final String KEY_1 = "com.dotcms.test.key1";
    private static final String VALUE_1 = "Test Key #1";




    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        // Creating a test Key/Value Content Type
        final long time = System.currentTimeMillis();
        final String contentTypeName = "Key/Value Test " + time;
        final String contentTypeVelocityVarName = "Keyvaluetest" + time;
        systemUser = APILocator.systemUser();
        final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
        ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
        keyValueContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
                        .description("Testing the Key/Value API.").name(contentTypeName).variable(contentTypeVelocityVarName)
                        .fixed(Boolean.FALSE).owner(systemUser.getUserId()).build();
        keyValueContentType = contentTypeApi.save(keyValueContentType);
        englishLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        spanishLanguageId = APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
        setDebugMode(Boolean.FALSE);
    }

    /*
     * Saving KeyValues in english and spanish.
     */
    @Test
    public void saveKeyValueContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final Contentlet contentletEnglish =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);
        final Contentlet contentletSpanish =
                        createTestKeyValueContent(KEY_1, VALUE_1, spanishLanguageId, keyValueContentType, systemUser);

        Assert.assertTrue("Failed creating a new english Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentletEnglish.getIdentifier()));
        Assert.assertTrue("Failed creating a new spanish Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentletSpanish.getIdentifier()));

        deleteContentlets(systemUser, contentletEnglish, contentletSpanish);
    }

    /*
     * Updating an existing KeyValue and verifying the cache group.
     */
    @Test
    public void updateKeyValueContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        Contentlet contentlet = createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);
        KeyValue cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        Assert.assertNotNull("Key/Value cache MUST NOT be null.", cachedKeyValue);

        final String newValue = keyValue.getValue() + ".updatedvalue";
        contentlet = updateTestKeyValueContent(contentlet, keyValue.getKey(), newValue, englishLanguageId, keyValueContentType,
                        systemUser);
        cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        System.out.print("cachedKeyValue: " + cachedKeyValue + "\n");
        Assert.assertNull("Key/Value cache MUST BE null.", cachedKeyValue);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Returning a list of KeyValues which have the same key.
     */
    @Test
    public void getKeyValueList() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final Contentlet contentlet =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);
        final Contentlet contentlet2 =
                        createTestKeyValueContent(KEY_1, VALUE_1 + "spanish", spanishLanguageId, keyValueContentType, systemUser);
        final List<KeyValue> keyValueList = keyValueAPI.get(KEY_1, systemUser, keyValueContentType, Boolean.FALSE);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));
        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet2.getIdentifier()));
        Assert.assertTrue("Key/Value list CANNOT BE empty.", !keyValueList.isEmpty());

        deleteContentlets(systemUser, contentlet, contentlet2);
    }

    /*
     * Testing the cache primary group verifying that the cache is empty at the beginning and filled
     * correctly at the end
     */
    @Test
    public void keyValueCache() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);
        List<KeyValue> keyValues = cache.get(KEY_1);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != keyValues ? keyValues.size() : ""), keyValues);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), systemUser, keyValueContentType, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                        cache.get(testKeyValue.getKey()).size()), cache.get(testKeyValue.getKey()).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byLanguageGroup verifying that the cache is empty at the beginning and
     * filled correctly at the end
     */
    @Test
    public void keyValueLanguageCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);
        List<KeyValue> keyValues = cache.getByLanguage(KEY_1, englishLanguageId);

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != cache.get(KEY_1) ? cache.get(KEY_1) : ""), cache.get(KEY_1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);

        System.out.print("testKeyValue: " + testKeyValue + ", keyValueAPI = " + keyValueAPI + "\n");
        System.out.print("testKeyValue.getKey: " + testKeyValue.getKey()
                + ", testKeyValue.getLanguageId = " + testKeyValue.getLanguageId() + "\n");
        keyValues = keyValueAPI.get(testKeyValue.getKey(), keyValueContentType, testKeyValue.getLanguageId(), systemUser, Boolean.FALSE);

        System.out.print("keyValues: " + keyValues  + "\n");
        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size()),
                        cache.getByLanguage(testKeyValue.getKey(), englishLanguageId).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byContentTypeGroup verifying that the cache is empty at the beginning and
     * filled correctly at the end
     */
    @Test
    public void keyValueContentTypeCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);

        List<KeyValue> keyValues = cache.getByContentType(KEY_1, keyValueContentType.id());

        Assert.assertNull(String.format("Key/Value cache MUST BE EMPTY at this point. It had %s elements.",
                        null != cache.get(KEY_1) ? cache.get(KEY_1) : ""), cache.get(KEY_1));

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        keyValues = keyValueAPI.get(testKeyValue.getKey(), keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertTrue(String.format("Key/Value list MUST CONTAIN 1 element. It had %s elements.", keyValues.size()),
                        keyValues.size() == 1);
        Assert.assertTrue(
                        String.format("Key/Value cache MUST CONTAIN 1 element. It had %s elements.",
                                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size()),
                        cache.getByContentType(testKeyValue.getKey(), keyValueContentType.id()).size() == 1);

        deleteContentlets(systemUser, contentlet);
    }

    /*
     * Testing the cache byLanguageContentTypeGroup verifying that the cache is empty at the
     * beginning and filled correctly at the end
     */
    @Test
    public void keyValueLanguageContentTypeCache() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        final KeyValueCache cache = CacheLocator.getKeyValueCache();
        cache.clearCache();
        final Contentlet contentlet =
                        createTestKeyValueContent(KEY_1, VALUE_1, englishLanguageId, keyValueContentType, systemUser);
        KeyValue cachedKeyValue = cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id());

        Assert.assertNull("Key/Value cache MUST BE NULL at this point.", cachedKeyValue);

        KeyValue testKeyValue = keyValueAPI.fromContentlet(contentlet);
        KeyValue keyValue =
                        keyValueAPI.get(testKeyValue.getKey(), englishLanguageId, keyValueContentType, systemUser, Boolean.FALSE);

        Assert.assertNotNull("Key/Value object MUST NOT be null.", keyValue);
        Assert.assertNotNull("Key/Value cache MUST NOT be null.",
                        cache.getByLanguageAndContentType(KEY_1, englishLanguageId, keyValueContentType.id()));

        deleteContentlets(systemUser, contentlet);
    }

    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {
        if (null != keyValueContentType && UtilMethods.isSet(keyValueContentType.id())) {
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            contentTypeApi.delete(keyValueContentType);
        }
        cleanupDebug(KeyValueAPITest.class);
    }

}
