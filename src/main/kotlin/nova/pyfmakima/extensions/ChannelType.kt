package nova.pyfmakima.extensions

import discord4j.core.`object`.entity.channel.Channel

fun Channel.Type.isThread(): Boolean {
    return this == Channel.Type.GUILD_NEWS_THREAD
        || this == Channel.Type.GUILD_PUBLIC_THREAD
        || this == Channel.Type.GUILD_PRIVATE_THREAD
}
