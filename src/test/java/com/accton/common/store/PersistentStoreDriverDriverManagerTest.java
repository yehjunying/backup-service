package com.accton.common.store;

import com.accton.common.store.impl.FilePersistentStoreDriver;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Constructor;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Unit test for simple App.
 */
public class PersistentStoreDriverDriverManagerTest
    extends TestCase
{
    final static String cwd = System.getProperty("user.dir");

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PersistentStoreDriverDriverManagerTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( PersistentStoreDriverDriverManagerTest.class );
    }

    static Object createObject(String className, Object... args) {
        Object object = null;
        try {
            Class classDefinition = Class.forName(className);

            Class[] cArg = new Class[args.length];
            for (Integer i = 0; i < args.length; ++i) {
                cArg[i] = args[i].getClass();
            }

            Constructor constructor = classDefinition.getDeclaredConstructor(cArg);
            object = constructor.newInstance(args);
        } catch (InstantiationException e) {
            System.out.println(e);
        } catch (IllegalAccessException e) {
            System.out.println(e);
        } catch (ReflectiveOperationException e) {
            System.out.println(e);
        //} catch (ClassNotFoundException e) {
        //    System.out.println(e);
        //} catch (NoSuchMethodException e) {
        //    System.out.println(e);
        }
        return object;
    }

    /**
     * Rigourous Test :-)
     */
    public void save(byte[] content, PersistentStoreDriver persistentStoreDriver, String key, String inputDate)
            throws ParseException {
        //int ret;

        Map<String, String> meta = new HashMap<>();

        //Date now = Calendar.getInstance().getTime();
        //
        //String inputString = "2012-05-01 01:01:01.000";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = dateFormat.parse(inputDate);
        //try {
        //    Date inputDate = dateFormat.parse(inputString);
        //    System.out.println(inputDate);
        //} catch (ParseException e) {
        //    //throw e;
        //}

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now);
        String id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now);

        meta.put("id", id); // TOD: id is timeStamp, it will change each saving operation. NOT id.
        meta.put("key", key);
        meta.put("modified", timeStamp);
        meta.put("fileExtension", ".json");

        {
            persistentStoreDriver.save(/*"test" + ".current"*/ key, content, meta);
            //if (ret == 0) {
            //    this.currentDoc = DocumentMeta.create(meta);
            //}
            //
            // TODO: check return value, if error case
        }

        //this.backupService.dataChanged(this.packageName + ".current");
        //
        //return ret;
    }

    public void testAutoSaveEvery10Min() {
        PersistentStoreDriver persistentStoreDriver = new FilePersistentStoreDriver("abc");
        BackupService backupService = new BackupService("abc", persistentStoreDriver);

        String key = "test.current";
        DocumentMeta[] allVersions;

        try {
            save("version 1".getBytes(), persistentStoreDriver, key, "2017-05-01 01:00:00.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 1);
            assertTrue(allVersions[0].getModified().equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));

            save("version 2".getBytes(), persistentStoreDriver, key, "2017-05-01 01:10:00.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 2);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));

            save("internal version 1".getBytes(), persistentStoreDriver, key, "2017-05-01 01:20:00.000");
            backupService.dataChanged("test.current");

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:20:00.000"));
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            save("internal version 2".getBytes(), persistentStoreDriver, key, "2017-05-01 01:21:00.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:21:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            save("internal version 3".getBytes(), persistentStoreDriver, key, "2017-05-01 01:29:59.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 3);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:29:59.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));

            //

            save("new version".getBytes(), persistentStoreDriver, key, "2017-05-01 01:30:00.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 4);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:30:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:29:59.000"));
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[2].getKey().equals(key));
            assertTrue(allVersions[3].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[3].getKey().equals(key));

            //

            save("new version".getBytes(), persistentStoreDriver, key, "2017-05-01 01:40:00.000");
            backupService.dataChanged(key);

            allVersions = backupService.getAllVersions(key);
            assertTrue(allVersions.length == 5);
            assertTrue(allVersions[0].value("modified").equals("2017-05-01 01:40:00.000")); // Last version
            assertTrue(allVersions[0].getKey().equals(key));
            assertTrue(allVersions[1].value("modified").equals("2017-05-01 01:30:00.000")); // Last version
            assertTrue(allVersions[1].getKey().equals(key));
            assertTrue(allVersions[2].value("modified").equals("2017-05-01 01:29:59.000"));
            assertTrue(allVersions[2].getKey().equals(key));
            assertTrue(allVersions[3].value("modified").equals("2017-05-01 01:10:00.000"));
            assertTrue(allVersions[3].getKey().equals(key));
            assertTrue(allVersions[4].value("modified").equals("2017-05-01 01:00:00.000"));
            assertTrue(allVersions[4].getKey().equals(key));
        } catch (ParseException e) {
            fail();
        }
    }

    public void testSave() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", /*new FilePersistentStoreDriver(this.cwd)*/ filePersistentStoreDriver);

        byte[] value = "{\"hello\": \"world\"}".getBytes();
        int ret = mgr.save(value);

        assertTrue( ret == 0);

        ArrayList<DocumentMeta> versionHistory = mgr.getAllVersions();

        assertTrue(versionHistory.size() == 1);
        assertTrue(versionHistory.get(0).getSize() == value.length);

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory.get(0).getId()));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, value));
    }

    public void testRestore() {
        PersistentStoreDriver filePersistentStoreDriver = (FilePersistentStoreDriver)createObject("com.accton.common.store.impl.FilePersistentStoreDriver", this.cwd);
        if (filePersistentStoreDriver == null) {
            fail();
        }

        PersistentStoreManager mgr = new PersistentStoreManager("foo", filePersistentStoreDriver);

        ArrayList<byte[]> files = new ArrayList<byte[]>() {{
            add("{\"version\": \"v1\"}".getBytes());
            add("{\"version\": \"last\"}".getBytes());
        }};

        for (byte[] file : files) {
            Integer ret = mgr.save(file);
            assertTrue( ret == 0);
        }

        ArrayList<DocumentMeta> versionHistory = mgr.getAllVersions();

        assert(versionHistory.size() == 2);
        assert(versionHistory.get(0).getSize() == files.get(1).length);

        try {
            byte[] output = mgr.restore(versionHistory.get(1).getId());
            assertTrue(Arrays.equals(output, files.get(0)));
        } catch (NoSuchVersionException | NoSuchFileException e) {
            System.out.println(e);
            fail();
        }

        String currentVerId = mgr.getCurrentVersionId();
        assertTrue(currentVerId.equals(versionHistory.get(1).getId()));

        byte[] currentOuput = mgr.getCurrentVersionContent();
        assertTrue(Arrays.equals(currentOuput, files.get(0)));
    }
}