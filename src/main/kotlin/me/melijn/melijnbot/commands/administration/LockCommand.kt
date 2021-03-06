package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class LockCommand : AbstractCommand("command.lock") {

    init {
        id = 241
        name = "lock"
        aliases = arrayOf("lockChannel")
        discordPermissions =
            (textDenyList + voiceDenyList).toTypedArray() // need the server permission in order to create overrides for it
        cooldown = 10_000
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val textDenyList = mutableListOf(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION)
        val voiceDenyList = mutableListOf(Permission.VOICE_CONNECT)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val voice = mutableListOf<VoiceChannel>()
        val text = mutableListOf<TextChannel>()
        val unknown = mutableListOf<String>()
        for ((index, arg) in context.args.withIndex()) {
            val textChannel = getTextChannelByArgsN(context, index)
            if (textChannel != null) {
                text.addIfNotPresent(textChannel)
                break
            }

            val voiceChannel = getVoiceChannelByArgsN(context, index)
            if (voiceChannel != null) {
                voice.addIfNotPresent(voiceChannel)
                break
            }

            if (arg == "all") {
                text.addAllIfNotPresent(context.guild.textChannels)
                voice.addAllIfNotPresent(context.guild.voiceChannels)
                break
            } else if (arg == "all-text") {
                text.addAllIfNotPresent(context.guild.textChannels)
                break
            } else if (arg == "all-voice") {
                voice.addAllIfNotPresent(context.guild.voiceChannels)
                break
            }

            unknown.addIfNotPresent(arg)
        }

        if (text.size == 0 && voice.size == 1 && unknown.size == 0) {
            lockChannel(context, voice.first())
        } else if (text.size == 1 && voice.size == 0 && unknown.size == 0) {
            lockChannel(context, text.first())
        } else if (text.size == 0 && voice.size == 0) {
            val msg = context.getTranslation("$root.notfound")
                .withSafeVariable("arg", unknown.joinToString())
            sendRsp(context, msg)
        } else {
            val msg = context.getTranslation("$root.questionmany")
                .withVariable("text", text.size)
                .withVariable("voice", voice.size)
                .withSafeVariable("unknown", unknown.joinToString())
            sendRsp(context, msg)

            context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
                it.author.idLong == context.authorId && it.channel.idLong == context.channelId
            }, {
                if (it.message.contentRaw == "yes") {
                    lockMany(context, voice + text)
                } else {
                    val rsp = context.getTranslation("$root.manyquestion.denied")
                    sendRsp(context, rsp)
                }
            }, {
                val rsp = context.getTranslation("$root.manyquestion.expired")
                sendRsp(context, rsp)
            }, 120)
        }
    }


    private suspend fun lockMany(context: ICommandContext, list: List<GuildChannel>) {
        for (channel in list) {
            if (notEnoughPermissionsAndMessage(
                    context,
                    channel,
                    Permission.MANAGE_CHANNEL,
                    Permission.MANAGE_ROLES
                )
            ) return
        }

        val alreadyLocked = mutableListOf<GuildChannel>()
        val locked = mutableListOf<GuildChannel>()
        var textOverrides = 0
        var textPermChanges = 0
        var voiceOverrides = 0
        var voicePermChanges = 0
        for (channel in list) {
            val unlockStatus = internalLock(context, channel)
            when (unlockStatus.third) {
                UnlockCommand.LockStatus.SUCCESS, UnlockCommand.LockStatus.NO_OVERRIDE -> locked.add(channel)
                UnlockCommand.LockStatus.ALREADY_LOCKED -> alreadyLocked.add(channel)
                UnlockCommand.LockStatus.NOT_LOCKED -> throw IllegalStateException("not locked in lockcommand")
            }
            if (channel is TextChannel) {
                textOverrides += unlockStatus.first
                textPermChanges += unlockStatus.second
            } else {
                voiceOverrides += unlockStatus.first
                voicePermChanges += unlockStatus.second
            }
        }

        val msg = context.getTranslation("$root.lockedmany")
            .withVariable("alreadyLocked", alreadyLocked.size)
            .withVariable("locked", locked.size)
            .withVariable("text", locked.filterIsInstance<TextChannel>().size)
            .withVariable("textOverrides", textOverrides)
            .withVariable("textPermChanges", textPermChanges)
            .withVariable("voice", locked.filterIsInstance<VoiceChannel>().size)
            .withVariable("voiceOverrides", voiceOverrides)
            .withVariable("voicePermChanges", voicePermChanges)


        val eb = Embedder(context)
            .setDescription(msg)
        sendEmbedRsp(context, eb.build())

    }

    private suspend fun lockChannel(context: ICommandContext, channel: GuildChannel) {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES)) return

        val status = internalLock(context, channel)
        val msg = when (status.third) {
            UnlockCommand.LockStatus.SUCCESS, UnlockCommand.LockStatus.NO_OVERRIDE -> {
                context.getTranslation("$root.locked")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.name)
                    .withVariable("overrides", status.first)
                    .withVariable("permChanges", status.second)
            }

            UnlockCommand.LockStatus.ALREADY_LOCKED -> {
                context.getTranslation("$root.alreadylocked")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.name)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            }
            UnlockCommand.LockStatus.NOT_LOCKED -> {
                throw IllegalStateException("not locked in lockcommand")
            }
        }

        sendRsp(context, msg)
    }

    // -> overrideCount, permissionsSwitched, status
    private suspend fun internalLock(
        context: ICommandContext,
        channel: GuildChannel
    ): Triple<Int, Int, UnlockCommand.LockStatus> {
        val denyList = when (channel) {
            is TextChannel -> textDenyList
            is VoiceChannel -> voiceDenyList
            else -> throw IllegalStateException("unknown channeltype")
        }

        val pubRole = context.guild.publicRole
        val overrideMap = mutableMapOf<Long, Pair<Long, Long>>()
        overrideMap.putAll(
            channel.rolePermissionOverrides.filterNotNull().map {
                it.idLong to Pair(it.allowedRaw, it.deniedRaw)
            }.toMap()
        )
        if (!overrideMap.containsKey(pubRole.idLong)) {
            overrideMap[pubRole.idLong] = 0L to 0L
        }


        val discordChannelOverridesWrapper = context.daoManager.discordChannelOverridesWrapper
        if (discordChannelOverridesWrapper.getAll(context.guildId, channel.idLong).isNotEmpty()) {
            return Triple(0, 0, UnlockCommand.LockStatus.ALREADY_LOCKED)
        }

        discordChannelOverridesWrapper.bulkPut(context.guildId, channel.idLong, overrideMap)

        var overrideCounter = 0
        var permsChangedCounter = 0
        for ((id, flags) in overrideMap) {
            val role = context.guild.getRoleById(id) ?: continue

            val manager = channel.upsertPermissionOverride(role)

            var permsChangedHere = 0
            for (perm in denyList) {
                when {
                    (flags.second and perm.rawValue) == 0L -> { // if the permission to deny is not yet denied
                        manager.deny(perm)
                        permsChangedHere++
                    }
                }
            }

            permsChangedCounter += permsChangedHere
            if (permsChangedHere > 0) {
                overrideCounter++
                manager.reason("(lock) " + context.author.asTag).queue()
            }
        }

        val status = if (overrideCounter != 0) {
            UnlockCommand.LockStatus.SUCCESS
        } else {
            UnlockCommand.LockStatus.NO_OVERRIDE
        }
        return Triple(overrideCounter, permsChangedCounter, status)
    }
}