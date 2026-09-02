package com.customwidgets.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.customwidgets.app.data.local.entity.McpServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface McpServerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: McpServerEntity): Long

    @Update
    suspend fun updateServer(server: McpServerEntity)

    @Delete
    suspend fun deleteServer(server: McpServerEntity)

    @Query("DELETE FROM mcp_servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)

    @Query("SELECT * FROM mcp_servers ORDER BY id ASC")
    fun getAllServers(): Flow<List<McpServerEntity>>

    @Query("SELECT * FROM mcp_servers WHERE isEnabled = 1")
    suspend fun getEnabledServers(): List<McpServerEntity>

    @Query("SELECT * FROM mcp_servers WHERE name = :name LIMIT 1")
    suspend fun getServerByName(name: String): McpServerEntity?
}
