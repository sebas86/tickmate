package de.smasi.tickmate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.smasi.tickmate.database.DataSource;
import de.smasi.tickmate.database.DatabaseOpenHelper;
import de.smasi.tickmate.database.FileUtils;
import de.smasi.tickmate.models.Track;
import de.smasi.tickmate.models.Group;

@Config(sdk = 17, constants = BuildConfig.class)
@RunWith(TickmateTestRunner.class)
public class DatabaseTest {
    Tickmate tickmate;
    DataSource dataSource;

    Method openMethod, closeMethod;

    @Before
    public void setUp() throws NoSuchMethodException {
        tickmate = new Tickmate();
        dataSource = DataSource.getInstance();

        // set TracksDataSource open method accessible
        openMethod = dataSource.getClass().getDeclaredMethod("open");
        openMethod.setAccessible(true);

        // set TracksDataSource close method accessible
        closeMethod = dataSource.getClass().getDeclaredMethod("close");
        closeMethod.setAccessible(true);
    }

    @After
    public void tearDown() {
        Field databseOpenHelperInstance, tracksDataSourceInstance;

        try {
            databseOpenHelperInstance = DatabaseOpenHelper.class.getDeclaredField("sharedInstance");
            databseOpenHelperInstance.setAccessible(true);
            databseOpenHelperInstance.set(null, null);

            tracksDataSourceInstance = DataSource.class.getDeclaredField("mInstance");
            tracksDataSourceInstance.setAccessible(true);
            tracksDataSourceInstance.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void databaseCreateExportImport() throws Exception {
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(0));
        closeMethod.invoke(dataSource);

        Track t = new Track("Testing", "Run my tests");
        t.setEnabled(true);
        assertThat(t.isEnabled(), is(true));
        assertThat(t.isSectionHeader(), is(false));

        openMethod.invoke(dataSource);
        dataSource.storeTrack(t);
        closeMethod.invoke(dataSource);

        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(1));
        closeMethod.invoke(dataSource);

        DatabaseOpenHelper db = DatabaseOpenHelper.getInstance(tickmate);
        db.exportDatabase("test.db");
        db.exportDatabase("test2.db");
        assertThat(Arrays.asList(db.getExternalDatabaseNames()),
                hasItem("test.db"));
        assertThat(Arrays.asList(db.getExternalDatabaseNames()),
                hasItem("test2.db"));

        Track t2 = new Track("Testing 2", "Run my tests again");
        openMethod.invoke(dataSource);
        dataSource.storeTrack(t2);
        closeMethod.invoke(dataSource);

        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(2));
        closeMethod.invoke(dataSource);

        db.importDatabase("test.db");

        // reimported previous database, so track count should be one again:
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(1));
        closeMethod.invoke(dataSource);
    }

    @Test
    public void tickmateShouldAppearInDescription() throws Exception {
        String hello = new Tickmate().getResources().getString(
                R.string.about_description);
        assertThat(hello, containsString("Tickmate"));

    }

    @Test
    public void trackDefaultIconResolvesToResID() throws Exception {
        Track t = new Track("Test track", "Cats and dogs");
        assertThat(t.getDescription(), is("Cats and dogs"));
        assertThat(t.getIcon(), not(is("")));
        assertThat(t.getIcon(), not(nullValue()));
        assert (t.getIconId(RuntimeEnvironment.application.getApplicationContext()) > 0);
    }

    @Test
    public void legacyDatabaseVersion10ShouldBeImportable() throws Exception {
        // File testDb = new File(getClass().getResource("test.sql").getFile());
        InputStream is = tickmate.getAssets().open("test/smiley-version10.db");
        DatabaseOpenHelper db = DatabaseOpenHelper.getInstance(tickmate);
        File extDb = new File(db.getExternalDatabasePath("legacy.db"));

        FileUtils.saveStreamToFile(is, new FileOutputStream(extDb));
        File intDb = tickmate.getApplicationContext().getDatabasePath("tickmate.db");
        intDb.getParentFile().mkdirs();
        db.importDatabase("legacy.db");

        // the legacy db should have 8 tracks (6 active)
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(8));
        assertThat(dataSource.getActiveTracks().size(), is(6));
        assertThat(dataSource.getTickCount(1), is(28));
        assertThat(dataSource.getTickCount(2), is(2));
        assertThat(dataSource.getTickCount(3), is(13));
        // make sure that no groups have been imported with a version 10 database
        // (groups did not exist back then)
        assertThat(dataSource.getGroups().size(), is(0));
        closeMethod.invoke(dataSource);
        ;
    }

    @Test
    public void legacyDatabaseVersion12ShouldBeImportable() throws Exception {
        // File testDb = new File(getClass().getResource("test.sql").getFile());
        InputStream is = tickmate.getAssets().open("test/tickmate-version12.db");
        DatabaseOpenHelper db = DatabaseOpenHelper.getInstance(tickmate);
        File extDb = new File(db.getExternalDatabasePath("legacy.db"));

        FileUtils.saveStreamToFile(is, new FileOutputStream(extDb));
        File intDb = tickmate.getApplicationContext().getDatabasePath("tickmate.db");
        intDb.getParentFile().mkdirs();
        db.importDatabase("legacy.db");

        // the legacy db should have 8 tracks (6 active)
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(2));
        assertThat(dataSource.getActiveTracks().size(), is(2));
        assertThat(dataSource.getTickCount(1), is(5));
        assertThat(dataSource.getTickCount(2), is(4));
        // make sure that no groups have been imported with a version 12 database
        // (groups did not exist back then)
        assertThat(dataSource.getGroups().size(), is(0));
        closeMethod.invoke(dataSource);
    }

    @Test
    public void legacyDatabaseVersion13ShouldBeImportable() throws Exception {
        // File testDb = new File(getClass().getResource("test.sql").getFile());
        InputStream is = tickmate.getAssets().open("test/tickmate-version13.db");
        DatabaseOpenHelper db = DatabaseOpenHelper.getInstance(tickmate);
        File extDb = new File(db.getExternalDatabasePath("legacy.db"));

        FileUtils.saveStreamToFile(is, new FileOutputStream(extDb));
        File intDb = tickmate.getApplicationContext().getDatabasePath("tickmate.db");
        intDb.getParentFile().mkdirs();
        db.importDatabase("legacy.db");

        // the legacy db should have 8 tracks (6 active)
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(3));
        assertThat(dataSource.getActiveTracks().size(), is(3));
        assertThat(dataSource.getTickCount(1), is(6));
        assertThat(dataSource.getTickCount(2), is(4));
        assertThat(dataSource.getTickCount(3), is(5));
        // make sure that 3 groups have been imported
        assertThat(dataSource.getGroups().size(), is(3));
        assertThat(dataSource.getGroupsForTrack(1).size(), is(1));
        assertThat(dataSource.getGroupsForTrack(2).size(), is(3));
        assertThat(dataSource.getGroupsForTrack(3).size(), is(1));
        assertThat(dataSource.getGroups().get(1).getName(), is("Wochenende"));
        assertThat(dataSource.getTrack(3).multipleEntriesEnabled(), is(true));
        assertThat(dataSource.getTrack(2).multipleEntriesEnabled(), is(false));

        closeMethod.invoke(dataSource);
    }

    @Test
    public void databaseGroupOrderTest() throws Exception {
        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(0));
        closeMethod.invoke(dataSource);

        Track t1 = new Track("Track 1", "Count to one");
        Track t2 = new Track("Track 2", "Count to two");
        Track t3 = new Track("Track 3", "Count to three");
        Track t4 = new Track("Track 4", "Count to four");
        Track t5 = new Track("Track 5", "Count to five");

        t1.setEnabled(true);
        t1.setOrder(1);
        t2.setEnabled(true);
        t2.setOrder(2);
        t3.setEnabled(true);
        t3.setOrder(3);
        t4.setEnabled(true);
        t4.setOrder(4);
        t5.setEnabled(true);
        t5.setOrder(5);

        openMethod.invoke(dataSource);
        /* store in random order */
        dataSource.storeTrack(t5);
        dataSource.storeTrack(t2);
        dataSource.storeTrack(t3);
        dataSource.storeTrack(t1);
        dataSource.storeTrack(t4);
        closeMethod.invoke(dataSource);

        openMethod.invoke(dataSource);
        assertThat(dataSource.getTracks().size(), is(5));

        assertThat(dataSource.getTracks().get(0).getName(), is("Track 1"));
        assertThat(dataSource.getTracks().get(1).getName(), is("Track 2"));
        assertThat(dataSource.getTracks().get(2).getName(), is("Track 3"));
        assertThat(dataSource.getTracks().get(3).getName(), is("Track 4"));
        assertThat(dataSource.getTracks().get(4).getName(), is("Track 5"));

        closeMethod.invoke(dataSource);

        Group g = new Group("My Group");
        g.setOrder(1);

        openMethod.invoke(dataSource);
        dataSource.storeGroup(g);
        dataSource.linkOneTrackOneGroup(t4.getId(), g.getId());
        dataSource.linkOneTrackOneGroup(t1.getId(), g.getId());
        dataSource.linkOneTrackOneGroup(t3.getId(), g.getId());
        dataSource.linkOneTrackOneGroup(t2.getId(), g.getId());
        closeMethod.invoke(dataSource);

        openMethod.invoke(dataSource);
        List<Track> ts = dataSource.getTracksForGroup(g.getId());
        assertThat(ts.get(0).getName(), is("Track 1"));
        assertThat(ts.get(1).getName(), is("Track 2"));
        assertThat(ts.get(2).getName(), is("Track 3"));
        assertThat(ts.get(3).getName(), is("Track 4"));

        closeMethod.invoke(dataSource);

    }
}
