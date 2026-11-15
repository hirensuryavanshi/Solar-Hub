package com.example.solarhub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SolarHubDao {

  @Query("SELECT * FROM departments ORDER BY orderIndex ASC, name ASC")
  fun getAllDepartments(): Flow<List<DepartmentEntity>>

  @Query("SELECT * FROM departments WHERE id = :id LIMIT 1")
  fun getDepartmentById(id: String): Flow<DepartmentEntity?>

  @Query("SELECT COUNT(*) FROM departments")
  suspend fun getDepartmentCount(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDepartment(department: DepartmentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDepartments(departments: List<DepartmentEntity>)

  @Update
  suspend fun updateDepartment(department: DepartmentEntity)

  @Query("DELETE FROM departments WHERE id = :id")
  suspend fun deleteDepartment(id: String)

  @Query("DELETE FROM content_items WHERE departmentId = :departmentId")
  suspend fun deleteContentByDepartment(departmentId: String)

  @Query("SELECT * FROM content_items WHERE departmentId = :deptId AND isPublished = 1 ORDER BY publishedAt DESC")
  fun getPublishedContentByDepartment(deptId: String): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE departmentId = :deptId ORDER BY updatedAt DESC")
  fun getAllContentByDepartment(deptId: String): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE isPublished = 1 ORDER BY publishedAt DESC LIMIT :limit")
  fun getLatestPublishedContent(limit: Int = 20): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE isPublished = 1 AND isAnnouncement = 1 ORDER BY publishedAt DESC LIMIT :limit")
  fun getPublishedAnnouncements(limit: Int = 10): Flow<List<ContentItemEntity>>

  @Query("""
    SELECT * FROM content_items
    WHERE isPublished = 1 AND (
      title LIKE '%' || :query || '%' OR
      description LIKE '%' || :query || '%' OR
      contentBody LIKE '%' || :query || '%' OR
      category LIKE '%' || :query || '%' OR
      subcategory LIKE '%' || :query || '%' OR
      attachmentName LIKE '%' || :query || '%'
    )
    ORDER BY publishedAt DESC
  """)
  fun searchPublishedContent(query: String): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE isPublished = 0 ORDER BY updatedAt DESC")
  fun getAllDrafts(): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE isPublished = 1 ORDER BY publishedAt DESC")
  fun getAllPublished(): Flow<List<ContentItemEntity>>

  @Query("SELECT * FROM content_items WHERE id = :id LIMIT 1")
  fun getContentById(id: String): Flow<ContentItemEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContent(item: ContentItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContentItems(items: List<ContentItemEntity>)

  @Update
  suspend fun updateContent(item: ContentItemEntity)

  @Query("DELETE FROM content_items WHERE id = :id")
  suspend fun deleteContent(id: String)

  @Query("DELETE FROM content_items WHERE isPublished = 0")
  suspend fun clearDrafts()

  // --- USER REQUESTS (PRIVATE TO USER OR ADMIN) ---
  @Query("SELECT * FROM user_requests ORDER BY createdAt DESC")
  fun getAllRequests(): Flow<List<UserRequestEntity>>

  @Query("SELECT * FROM user_requests WHERE userId = :userId ORDER BY createdAt DESC")
  fun getRequestsByUser(userId: String): Flow<List<UserRequestEntity>>

  @Query("SELECT * FROM user_requests WHERE isPendingSync = 1 ORDER BY createdAt ASC")
  suspend fun getPendingSyncRequests(): List<UserRequestEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequest(request: UserRequestEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequests(requests: List<UserRequestEntity>)

  @Query("DELETE FROM user_requests WHERE id = :id")
  suspend fun deleteRequest(id: String)

  @Query("DELETE FROM user_requests WHERE userId != :userId AND isPendingSync = 0")
  suspend fun clearRequestsNotOwnedBy(userId: String)

  @Query("DELETE FROM user_requests WHERE isPendingSync = 0")
  suspend fun clearSyncedRequests()

  @Query("UPDATE user_requests SET userId = :newUserId WHERE userId = :oldUserId")
  suspend fun reassignUserIdForRequests(oldUserId: String, newUserId: String)
}
