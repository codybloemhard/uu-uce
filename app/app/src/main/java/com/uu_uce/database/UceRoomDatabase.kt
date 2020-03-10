package com.uu_uce.database

import android.content.Context
import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = arrayOf(PinData::class), version = 1, exportSchema = false)
public abstract class UceRoomDatabase : RoomDatabase() {

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
            var pin = PinData(1, "31N3149680N46777336E",1, "test", "testPin1", "test", 60)
            var pin2 = PinData(2, "31N3133680N46718336E", 1, "test", "testPin2", "test", 60)

            //var pin2 = PinData(2, "123456", 1, "test", "testPin2", "test", 1)

            pinDao.insert(pin)
            pinDao.insert(pin2)
            //pinDao.insert(pin2)
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