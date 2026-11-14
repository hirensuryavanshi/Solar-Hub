package com.example.solarhub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [DepartmentEntity::class, ContentItemEntity::class, UserRequestEntity::class],
  version = 2,
  exportSchema = false
)
abstract class SolarHubDatabase : RoomDatabase() {

  abstract fun solarHubDao(): SolarHubDao

  companion object {
    @Volatile
    private var INSTANCE: SolarHubDatabase? = null

    val INITIAL_DEPARTMENTS = listOf(
      DepartmentEntity("dept_production", "Production", "factory", 1, 1000L),
      DepartmentEntity("dept_quality", "Quality", "verified", 2, 1001L),
      DepartmentEntity("dept_packing", "Packing", "inventory", 3, 1002L),
      DepartmentEntity("dept_sap", "SAP", "hub", 4, 1003L),
      DepartmentEntity("dept_ppc", "PPC", "timeline", 5, 1004L),
      DepartmentEntity("dept_solar_cell", "Solar Cell", "solar_cell", 6, 1005L),
      DepartmentEntity("dept_solar_module", "Solar Module", "solar_module", 7, 1006L),
      DepartmentEntity("dept_maintenance", "Maintenance", "build", 8, 1007L),
      DepartmentEntity("dept_safety", "Safety", "security", 9, 1008L),
      DepartmentEntity("dept_store", "Store", "warehouse", 10, 1009L),
      DepartmentEntity("dept_dispatch", "Dispatch", "local_shipping", 11, 1010L),
      DepartmentEntity("dept_rnd", "R&D", "science", 12, 1011L),
      DepartmentEntity("dept_ehs", "EHS", "eco", 13, 1013L),
      DepartmentEntity("dept_hr_training", "HR & Training", "groups", 14, 1014L)
    )

    // Adds the "user requests" table without touching any existing data
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "CREATE TABLE IF NOT EXISTS `user_requests` (" +
            "`id` TEXT NOT NULL, " +
            "`userName` TEXT NOT NULL, " +
            "`userMobile` TEXT NOT NULL, " +
            "`message` TEXT NOT NULL, " +
            "`status` TEXT NOT NULL, " +
            "`adminReply` TEXT NOT NULL, " +
            "`createdAt` INTEGER NOT NULL, " +
            "`updatedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))"
        )
      }
    }

    fun getDatabase(context: Context): SolarHubDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SolarHubDatabase::class.java,
          "solar_hub_database"
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
              INSTANCE?.solarHubDao()?.insertDepartments(INITIAL_DEPARTMENTS)
            }
          }
        })
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
