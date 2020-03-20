package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = arrayOf(PinData::class), version = 1, exportSchema = false)
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
            pinDao.deleteAllPins()
            var pin = PinData(0,"31N3149680N46777336E",1, "TEXT", "testPin1", "test", 60)
            var pin2 = PinData(0, "31N3133680N46718336E", 2, "IMAGE", "testPin2", "test", 60)
            var pin3 = PinData(0, "31N3130000N46710000E", 3, "NONE", "testPin3", "test", 60)

            pinDao.insert(pin)
            pinDao.insert(pin2)
            pinDao.insert(pin3)
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