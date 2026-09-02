package com.customwidgets.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.customwidgets.app.mcp.model.McpServerConfig

@Entity(tableName = "mcp_servers")
data class McpServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val apiKey: String? = null
) {
    fun toDomain(): McpServerConfig {
        return McpServerConfig(
            id = id,
            name = name,
            url = url,
            isEnabled = isEnabled,
            apiKey = apiKey
        )
    }

    companion object {
        fun fromDomain(domain: McpServerConfig): McpServerEntity {
            return McpServerEntity(
                id = domain.id,
                name = domain.name,
                url = domain.url,
                isEnabled = domain.isEnabled,
                apiKey = domain.apiKey
            )
        }
    }
}
