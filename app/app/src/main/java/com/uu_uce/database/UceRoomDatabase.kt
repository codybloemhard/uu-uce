package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uu_uce.allpins.PinData
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.views.pinsUpdated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PinData::class, FieldbookEntry::class], version = 3, exportSchema = false)
abstract class UceRoomDatabase : RoomDatabase() {

    abstract fun pinDao(): PinDao

    abstract fun fieldbookDao(): FieldbookDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    }
}