package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class BoostMessageCommand : AbstractCommand("command.boostmessage") {

    init {
        id = 179
        name = "boostMessage"
        aliases = arrayOf("boostM")
        children = arrayOf(
            LeaveMessageCommand.SetContentArg(root, MessageType.BOOST),
            LeaveMessageCommand.EmbedArg(root, MessageType.BOOST),
            LeaveMessageCommand.AttachmentsArg(root, MessageType.BOOST),
            LeaveMessageCommand.ViewArg(root, MessageType.BOOST),
            LeaveMessageCommand.SetPingableArg(root, MessageType.BOOST)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}