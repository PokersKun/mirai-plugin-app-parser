package cc.pkks.mirai.plugin.appparser

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.LightApp
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `org.example.mirai.plugin.PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "cc.pkks.mirai.plugin.appparser",
        name = "小程序链接提取",
        version = "0.1.0"
    ) {
        author("PokerS")
        info(
            """
            自动提取小程序卡片中的真实链接。
        """.trimIndent()
        )
        // author 和 info 可以删除.
    }
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
        //配置文件目录 "${dataFolder.absolutePath}/"

        var repeatGroupId = 0L
        var repeatMessage = ""
        var repeatCount = 0

        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
//            if (group.id == 12345) {
                //分类示例
                message.forEach {
                    //循环每个元素在消息里
                    if (it is PlainText) {
                        //如果消息这一部分是纯文本
                        val newMessage = it.content
                        val newGroupId = group.id
                        //复读
                        if (newMessage == repeatMessage && newGroupId == repeatGroupId) {
                            repeatCount++
                            if (repeatCount >= 2) {
                                this.group.sendMessage(newMessage)
                                repeatCount = 0
                            }
                        } else {
                            repeatMessage = newMessage
                            repeatGroupId = newGroupId
                            repeatCount = 1
                        }
                    } else if (it is LightApp) {
                        //提取链接
                        val newMessage = it.content
                        val titleRegex = "\"title\":\\s*\"(.*?)\"".toRegex() // 匹配标题
                        val qqDocUrlRegex = "\"qqdocurl\":\\s*\"(.*?)\"".toRegex() // 匹配 qqdocurl
                        val jumpUrlRegex = "\"jumpUrl\":\\s*\"(.*?)\"".toRegex() // 匹配 jumpUrl
                        val titleMatch = titleRegex.find(newMessage)
                        val qqDocUrlMatch = qqDocUrlRegex.find(newMessage)
                        val jumpUrlMatch = jumpUrlRegex.find(newMessage)

                        if (titleMatch != null) {
                            val title = titleMatch.groupValues[1]
                            val url = if (qqDocUrlMatch != null) {
                                qqDocUrlMatch.groupValues[1]
                            } else if (jumpUrlMatch != null) {
                                jumpUrlMatch.groupValues[1]
                            } else {
                                null
                            }
                            if (url != null) {
                                this.group.sendMessage("标题：$title\n链接：${url.replace("\\", "")}")
                            }
                        } else {
                            logger.info { "No title or URL found in: $newMessage" }
                        }
                    }
                }
//            }
        }
    }
    // endregion
}
