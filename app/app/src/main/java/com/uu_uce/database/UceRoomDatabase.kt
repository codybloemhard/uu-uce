package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uu_uce.allpins.PinData
import com.uu_uce.fieldbook.FieldbookEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PinData::class, FieldbookEntry::class], version = 1, exportSchema = false)
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
                    0,
                    "31N46777336N3149680E",
                    1,
                    "TEXT",
                    "Test text",
                    "[{\"tag\":\"TEXT\", \"text\":\"test\"}]",
                    1,
                    "-1",
                    "-1"
                )
            )
            pinList.add(
                PinData(
                    1,
                    "31N46718336N3133680E",
                    2,
                    "IMAGE",
                    "Test image",
                    "[{\"tag\":\"IMAGE\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/images/test.png\"}]",
                    1,
                    "-1",
                    "-1"
                )
            )
            pinList.add(
                PinData(
                    2,
                    "31N46710000N3130000E",
                    3,
                    "VIDEO",
                    "Test video",
                    "[{\"tag\":\"VIDEO\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/videos/zoo.mp4\", \"thumbnail\":\"file:///data/data/com.uu_uce/files/pin_content/videos/thumbnails/zoothumbnail.png\", \"title\":\"zoo video\"}]",
                    0,
                    "3",
                    "-1"
                )
            )
            pinList.add(
                PinData(
                    3,
                    "31N46715335N3134680E",
                    3,
                    "MCQUIZ",
                    "Test quiz",
                    "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}, {\"tag\":\"TEXT\", \"text\":\"Press right again\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\", \"reward\" : 25}]",
                    1,
                    "-1",
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
                    .addCallback(UceDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}