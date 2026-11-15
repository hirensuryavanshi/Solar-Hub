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
  fun getLatestPublishedContent(limit: Int = 10): Flow<List<ContentItemEntity>>

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

  @Update
  suspend fun updateContent(item: ContentItemEntity)

  @Query("DELETE FROM content_items WHERE id = :id")
  suspend fun deleteContent(id: String)

  @Query("SELECT * FROM user_requests ORDER BY createdAt DESC")
  fun getAllRequests(): Flow<List<UserRequestEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequest(request: UserRequestEntity)

  @Query("UPDATE user_requests SET status = :status, adminReply = :reply, updatedAt = :now WHERE id = :id")
  suspend fun updateRequest(id: String, status: String, reply: String, now: Long)

  @Query("DELETE FROM user_requests WHERE id = :id")
  suspend fun deleteRequest(id: String)

  @Query("SELECT * FROM content_items WHERE id = :id LIMIT 1")
  suspend fun getContentOnce(id: String): ContentItemEntity?

  @Query("SELECT * FROM content_items WHERE isPublished = 1")
  suspend fun getPublishedOnce(): List<ContentItemEntity>

  @Query("SELECT * FROM user_requests WHERE id = :id LIMIT 1")
  suspend fun getRequestOnce(id: String): UserRequestEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRequests(requests: List<UserRequestEntity>)

  @Query("DELETE FROM user_requests WHERE id NOT IN (:ids)")
  suspend fun deleteRequestsNotIn(ids: List<String>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(users: List<AppUserEntity>)

  @Query("DELETE FROM app_users WHERE mobile NOT IN (:mobiles)")
  suspend fun deleteUsersNotIn(mobiles: List<String>)

  @Query("SELECT * FROM app_users ORDER BY lastLoginAt DESC")
  fun getAllUsers(): Flow<List<AppUserEntity>>

  @Query("SELECT * FROM app_users WHERE mobile = :mobile LIMIT 1")
  suspend fun getUser(mobile: String): AppUserEntity?

  @Query("SELECT * FROM app_users WHERE mobile = :mobile LIMIT 1")
  fun observeUser(mobile: String): Flow<AppUserEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUser(user: AppUserEntity)

  @Query("UPDATE app_users SET isBlocked = :blocked WHERE mobile = :mobile")
  suspend fun setUserBlocked(mobile: String, blocked: Boolean)

  @Query("DELETE FROM app_users WHERE mobile = :mobile")
  suspend fun deleteUser(mobile: String)
}
