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
            pinList.add(PinData(0, "31N46777336N3149680E", 1, "TEXT" , "Text pin", "[{\"tag\":\"TEXT\", \"text\":\"test\"}]", 1, "-1", "-1"))
            pinList.add(PinData(1, "31N46718336N3133680E", 2, "IMAGE", "Image pin", "[{\"tag\":\"IMAGE\", \"file_name\":\"test.png\"}]", 1, "-1", "-1"))
            pinList.add(PinData(2, "31N46710000N3130000E", 3, "VIDEO", "Video pin", "[{\"tag\":\"VIDEO\", \"file_name\":\"zoo.mp4\", \"thumbnail\":\"zoothumbnail.png\", \"title\":\"zoo video\"}]", 0, "3", "-1"))
            pinList.add(PinData(3, "31N46715335N3134680E", 3, "MCQUIZ", "Quiz pin", "[{\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}]", 1, "-1", "2"))

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