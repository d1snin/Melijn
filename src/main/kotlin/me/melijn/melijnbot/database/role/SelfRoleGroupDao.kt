package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.utils.splitIETEL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoleGroups"
    override val tableStructure: String = "guildId bigint, groupName varchar(64), ids varchar(1024), isEnabled boolean, isSelfRoleable boolean"
    override val primaryKey: String = "guildId, groupName"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): List<SelfRoleGroup> = suspendCoroutine {
        val list = mutableListOf<SelfRoleGroup>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                list.add(
                    SelfRoleGroup(
                        rs.getString("groupName"),
                        rs.getString("messageIds").splitIETEL("%SPLIT%").map { it.toLong() },
                        rs.getBoolean("isEnabled"),
                        rs.getBoolean("isSelfRoleable")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }


    suspend fun set(guildId: Long, groupName: String, messageIds: String, isEnabled: Boolean, isSelfRoleable: Boolean) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, messageIds, isEnabled, isSelfRoleable) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET messageIds = ? AND isEnabled = ? AND isSelfRoleable = ?",
            guildId, groupName, messageIds, isEnabled, isSelfRoleable, messageIds, isEnabled, isSelfRoleable)
    }

    suspend fun remove(guildId: Long, groupName: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupName = ?",
            guildId, groupName)
    }
}

data class SelfRoleGroup(
    val groupName: String,
    val messageIds: List<Long>,
    var isEnabled: Boolean,
    var isSelfRoleable: Boolean
)