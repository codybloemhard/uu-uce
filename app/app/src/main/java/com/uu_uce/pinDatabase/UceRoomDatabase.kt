package com.uu_uce.pinDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uu_uce.fieldbook.FieldbookDao
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
                    populatePinTable(database.pinDao())
                    //populateFieldbook(database.fieldbookDao())
                }
            }
        }

        /*suspend fun populateFieldbook(fieldbookDao: FieldbookDao) {
            val fieldbookEntries: MutableList<FieldbookEntry> = mutableListOf(
                FieldbookEntry(
                    "31N3149680N46777336E",
                    "04-05-1945 07:54",
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"IMAGE\",\"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/images/test.png\"}]"
                ),
                FieldbookEntry(
                    "31N3133680N46718336E",
                    "01-01-1970 00:00",
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"VIDEO\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/videos/zoo.mp4\", \"thumbnail\":\"file:///data/data/com.uu_uce/files/pin_content/videos/thumbnails/zoothumbnail.png\", \"title\":\"zoo video\"}]"
                )
            )

            fieldbookDao.insertAll(fieldbookEntries)
        }*/


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