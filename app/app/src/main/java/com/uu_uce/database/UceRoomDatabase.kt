package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uu_uce.allpins.PinData
import com.uu_uce.fieldbook.FieldbookEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PinData::class, FieldbookEntry::class], version = 4, exportSchema = false)
abstract class UceRoomDatabase : RoomDatabase() {

    abstract fun pinDao(): PinDao

    abstract fun fieldbookDao(): FieldbookDao

    private class UceDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populatePinTable(database.pinDao()) // TODO: remove when database is fully implemented
                }
            }
        }

        suspend fun populatePinTable(pinDao: PinDao) {
            val pinList: MutableList<PinData> = mutableListOf()
            pinList.add(
                PinData(
                    "691bee74-565d-4e2c-8615-c407b8e869c6",
                    "31N46777336N3149680E",
                    1,
                    "TEXT",
                    "Test text",
                    "[{\"tag\":\"TEXT\", \"text\":\"test\"}]",
                    1,
                    1,
                    "",
                    ""
                )
            )
            pinList.add(
                PinData(
                    "d8abb292-c253-49be-8d55-f92d80275654",
                    "31N46758336N3133680E",
                    2,
                    "IMAGE",
                    "Test image",
                    "[{\"tag\":\"IMAGE\", \"file_path\":\"Images/test.png\"}]",
                    1,
                    1,
                    "",
                    ""
                )
            )
            pinList.add(
                PinData(
                    "f0e7638e-9eaa-4c9e-be45-cdafabae3ad5",
                    "31N46670000N3130000E",
                    3,
                    "VIDEO",
                    "Test video",
                    "[{\"tag\":\"VIDEO\", \"file_path\":\"Videos/zoo.mp4\", \"thumbnail\":\"Videos/Thumbnails/zoothumbnail.png\", \"title\":\"zoo video\"}]",
                    0,
                    0,
                    "3",
                    ""
                )
            )
            pinList.add(
                PinData(
                    "539272be-a3c3-4102-ae2f-9c740c1aa1b4",
                    "31N46655335N3134680E",
                    3,
                    "MCQUIZ",
                    "Test quiz",
                    "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}, {\"tag\":\"TEXT\", \"text\":\"Press right again\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\", \"reward\" : 25}]",
                    1,
                    1,
                    "",
                    "2"
                )
            )
            pinDao.updateData(pinList)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UceRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): UceRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UceRoomDatabase::class.java,
                    "uce_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(UceDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE pins_new (" +
                            "pinId INTEGER NOT NULL, " +
                            "location TEXT NOT NULL, " +
                            "difficulty INTEGER NOT NULL," +
                            "type TEXT NOT NULL," +
                            "title TEXT NOT NULL," +
                            "content TEXT NOT NULL," +
                            "status INTEGER NOT NULL," +
                            "predecessorIds TEXT NOT NULL," +
                            "followIds TEXT NOT NULL," +
                            "PRIMARY KEY(pinId))")
                // Copy the data
                database.execSQL(
                    "INSERT INTO pins_new (" +
                            "pinId, " +
                            "location, " +
                            "difficulty, " +
                            "type, " +
                            "title, " +
                            "content, " +
                            "status, " +
                            "predecessorIds, " +
                            "followIds) " +
                            "SELECT " +
                            "pinId, " +
                            "location, " +
                            "difficulty, " +
                            "type, " +
                            "title, " +
                            "content, " +
                            "status, " +
                            "predecessorIds, " +
                            "followIds " +
                            " FROM pins")
                            // Remove the old table
                            database.execSQL("DROP TABLE pins")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE pins_new RENAME TO pins")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE pins_new (" +
                            "pinId TEXT NOT NULL, " +
                            "location TEXT NOT NULL, " +
                            "difficulty INTEGER NOT NULL," +
                            "type TEXT NOT NULL," +
                            "title TEXT NOT NULL," +
                            "content TEXT NOT NULL," +
                            "status INTEGER NOT NULL," +
                            "predecessorIds TEXT NOT NULL," +
                            "followIds TEXT NOT NULL," +
                            "PRIMARY KEY(pinId))")
                // Copy the data
                database.execSQL(
                    "INSERT INTO pins_new (" +
                            "pinId, " +
                            "location, " +
                            "difficulty, " +
                            "type, " +
                            "title, " +
                            "content, " +
                            "status, " +
                            "predecessorIds, " +
                            "followIds) " +
                            "SELECT " +
                            "pinId, " +
                            "location, " +
                            "difficulty, " +
                            "type, " +
                            "title, " +
                            "content, " +
                            "status, " +
                            "predecessorIds, " +
                            "followIds " +
                            " FROM pins")
                // Remove the old table
                database.execSQL("DROP TABLE pins")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE pins_new RENAME TO pins")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL( "ALTER TABLE pins ADD COLUMN startStatus INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}