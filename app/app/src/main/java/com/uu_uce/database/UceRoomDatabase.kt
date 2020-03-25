package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PinData::class], version = 1, exportSchema = false)
abstract class UceRoomDatabase : RoomDatabase() {

    abstract fun pinDao() : PinDao

    private class UceDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.pinDao())
                }
            }
        }

        suspend fun populateDatabase(pinDao: PinDao) {
            val pinList : MutableList<PinData> = mutableListOf()
            pinList.add(PinData(0, 0, "31N3149680N46777336E", 1, "TEXT" , "testPin1", "[{\"tag\":\"TEXT\", \"text\":\"test\"}]", 1, "-1", "-1"))
            pinList.add(PinData(0, 1, "31N3133680N46718336E", 2, "IMAGE", "testPin2", "[{\"tag\":\"IMAGE\", \"file_name\":\"test.png\"}]", 1, "-1", "2"))
            pinList.add(PinData(0, 2, "31N3130000N46710000E", 3, "VIDEO", "testPin3", "[{\"tag\":\"VIDEO\", \"file_name\":\"zoo.mp4\", \"thumbnail\":\"zoothumbnail.png\", \"title\":\"zoo video\"}]", 0, "1", "-1"))

            pinDao.updateData(pinList)
        }
    }

    companion object{
        @Volatile
        private var INSTANCE: UceRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): UceRoomDatabase {
            return INSTANCE ?:
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UceRoomDatabase::class.java,
                    "uce_database"
                )
                    .addCallback(UceDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}