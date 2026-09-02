package com.customwidgets.app.mcp

import com.customwidgets.app.data.local.dao.McpServerDao
import com.customwidgets.app.data.local.entity.McpServerEntity
import com.customwidgets.app.mcp.model.McpServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpServerRepository @Inject constructor(
    private val mcpServerDao: McpServerDao
) {
    fun getAllServers(): Flow<List<McpServerConfig>> {
        return mcpServerDao.getAllServers().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveServer(config: McpServerConfig): Long {
        return mcpServerDao.insertServer(McpServerEntity.fromDomain(config))
    }

    suspend fun deleteServer(id: Long) {
        mcpServerDao.deleteServerById(id)
    }

    suspend fun toggleServer(id: Long, enabled: Boolean) {
        val all = mcpServerDao.getEnabledServers()
        all.firstOrNull { it.id == id }?.let {
            mcpServerDao.updateServer(it.copy(isEnabled = enabled))
        }
    }

    suspend fun getAllEnabledServers(): List<McpServerConfig> {
        return mcpServerDao.getEnabledServers().map { it.toDomain() }
    }

    suspend fun getServerByName(name: String): McpServerConfig? {
        return mcpServerDao.getServerByName(name)?.toDomain()
    }
}
