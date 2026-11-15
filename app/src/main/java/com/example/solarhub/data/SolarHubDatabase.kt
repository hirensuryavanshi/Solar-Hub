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
      DepartmentEntity("dept_production", "Production", "factory", 1, 1000L, 1000L),
      DepartmentEntity("dept_quality", "Quality", "verified", 2, 1001L, 1001L),
      DepartmentEntity("dept_packing", "Packing", "inventory", 3, 1002L, 1002L),
      DepartmentEntity("dept_sap", "SAP", "hub", 4, 1003L, 1003L),
      DepartmentEntity("dept_ppc", "PPC", "timeline", 5, 1004L, 1004L),
      DepartmentEntity("dept_solar_cell", "Solar Cell", "solar_cell", 6, 1005L, 1005L),
      DepartmentEntity("dept_solar_module", "Solar Module", "solar_module", 7, 1006L, 1006L),
      DepartmentEntity("dept_maintenance", "Maintenance", "build", 8, 1007L, 1007L),
      DepartmentEntity("dept_safety", "Safety", "security", 9, 1008L, 1008L),
      DepartmentEntity("dept_store", "Store", "warehouse", 10, 1009L, 1009L),
      DepartmentEntity("dept_dispatch", "Dispatch", "local_shipping", 11, 1010L, 1010L),
      DepartmentEntity("dept_rnd", "R&D", "science", 12, 1011L, 1011L),
      DepartmentEntity("dept_engineering", "Engineering", "engineering", 13, 1012L, 1012L),
      DepartmentEntity("dept_ehs", "EHS", "eco", 14, 1013L, 1013L),
      DepartmentEntity("dept_hr_training", "HR & Training", "groups", 15, 1014L, 1014L),
      DepartmentEntity("dept_purchase", "Purchase", "shopping_cart", 16, 1015L, 1015L),
      DepartmentEntity("dept_planning_inventory", "Planning & Inventory", "calendar_month", 17, 1016L, 1016L)
    )

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `departments` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `content_items` ADD COLUMN `attachmentFileId` TEXT")
        db.execSQL("ALTER TABLE `content_items` ADD COLUMN `isAnnouncement` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_items_departmentId` ON `content_items` (`departmentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_items_isPublished` ON `content_items` (`isPublished`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_items_publishedAt` ON `content_items` (`publishedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_items_isAnnouncement` ON `content_items` (`isAnnouncement`)")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `user_requests` (
            `id` TEXT NOT NULL,
            `clientRequestId` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `userName` TEXT NOT NULL,
            `userMobile` TEXT NOT NULL,
            `departmentId` TEXT NOT NULL,
            `departmentName` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `details` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `adminReply` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `repliedAt` INTEGER,
            `isPendingSync` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_requests_userId` ON `user_requests` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_requests_createdAt` ON `user_requests` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_requests_isPendingSync` ON `user_requests` (`isPendingSync`)")
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
