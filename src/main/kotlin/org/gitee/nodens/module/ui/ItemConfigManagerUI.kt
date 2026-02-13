package org.gitee.nodens.module.ui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType.*
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.ItemManager
import taboolib.common.platform.function.getDataFolder
import taboolib.library.xseries.XMaterial
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.impl.PageableChestImpl
import taboolib.platform.util.ItemBuilder
import taboolib.platform.util.giveItem
import java.io.File

class ItemConfigManagerUI(val viewer: Player) {

    companion object {

        lateinit var node: ParentNode

        interface Node {
            val file: File
        }

        class ParentNode(val subNode: List<Node>, override val file: File) : Node
        class SubNode(val configs: List<ItemConfig>, override val file: File) : Node

        private val contentSlots = listOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        )

        private val borderSlots = listOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44
        )

        internal fun load() {
            val file = File(getDataFolder(), "items")
            node = ParentNode(deepFind(file), file)
        }

        private fun deepFind(file: File): List<Node> {
            return file.listFiles()!!.map {
                if (it.isDirectory) {
                    ParentNode(deepFind(it), it)
                } else {
                    SubNode(
                        getOrderedKeys(it).mapNotNull { key ->
                            ItemManager.getItemConfig(key)
                        },
                        it
                    )
                }
            }.sortedWith(compareBy<Node> { it !is ParentNode }.thenBy { it.file.name })
        }

        private fun getOrderedKeys(file: File): List<String> {
            return file.readLines().mapNotNull { line ->
                if (line.isNotBlank() && !line[0].isWhitespace() && !line.startsWith("#")) {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex > 0) line.substring(0, colonIndex).trim()
                        .removeSurrounding("'").removeSurrounding("\"") else null
                } else null
            }
        }

        fun findParent(target: Node, current: ParentNode = node): ParentNode? {
            for (i in current.subNode) {
                if (i == target) return current
                if (i is ParentNode) return findParent(target, i) ?: continue
            }
            return null
        }
    }

    private val borderItem = ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).apply {
        name = "§r"
    }.build()

    private val fillItem = ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).apply {
        name = "§r"
    }.build()

    private fun pageItem(name: String, active: Boolean) = ItemBuilder(
        if (active) XMaterial.SPECTRAL_ARROW else XMaterial.GRAY_STAINED_GLASS_PANE
    ).apply {
        this.name = if (active) "§e$name" else "§r"
    }.build()

    private fun backItem() = ItemBuilder(XMaterial.DARK_OAK_DOOR).apply {
        name = "§c返回上一级"
        lore.add("§f点击返回上级目录")
    }.build()

    private fun infoItem(node: Node) = ItemBuilder(XMaterial.OAK_SIGN).apply {
        name = "§e§l当前位置"
        lore.add("§8┃ §f${node.file.name}")
    }.build()

    private fun buildPath(node: Node): String {
        if (node == Companion.node) return "根目录"
        return node.file.relativeTo(Companion.node.file).path
            .replace(File.separatorChar, '/')
    }

    private fun applyDecoration(menu: PageableChestImpl<*>) {
        borderSlots.forEach { menu.set(it, borderItem) }
        listOf(46, 47, 50, 51, 52).forEach { menu.set(it, fillItem) }
    }

    fun open() {
        open(node)
    }

    fun open(node: Node) {
        if (node is ParentNode) {
            viewer.openMenu<PageableChestImpl<Node>>("§8§l物品管理 §8| §f${buildPath(node)}") {
                handLocked(false)
                rows(6)
                slots(contentSlots)
                elements { node.subNode }
                applyDecoration(this)
                setPreviousPage(45) { _, has -> pageItem("上一页", has) }
                setNextPage(53) { _, has -> pageItem("下一页", has) }
                set(49, infoItem(node))
                if (node != Companion.node) {
                    set(48, backItem()) {
                        open(findParent(node) ?: Companion.node)
                    }
                } else {
                    set(48, fillItem)
                }
                onGenerate(false) { _, element, _, _ ->
                    when (element) {
                        is ParentNode -> ItemBuilder(XMaterial.CHEST).apply {
                            name = "§6§l${element.file.nameWithoutExtension}"
                            lore.add("§8┃ §f文件夹")
                            lore.add("§8┃ §f包含 §f${element.subNode.size} §f项")
                            lore.add("")
                            lore.add("§e左键 §8» §f打开")
                        }.build()
                        is SubNode -> ItemBuilder(XMaterial.PAPER).apply {
                            name = "§f§l${element.file.nameWithoutExtension}"
                            lore.add("§8┃ §f配置文件")
                            lore.add("§8┃ §f包含 §f${element.configs.size} §f个物品")
                            lore.add("")
                            lore.add("§e左键 §8» §f打开")
                        }.build()
                        else -> error("Unknown element type ${element.file.name}")
                    }
                }
                onClick { _, element ->
                    open(element)
                }
            }
        } else if (node is SubNode) {
            viewer.openMenu<PageableChestImpl<ItemConfig>>("§8§l物品列表 §8| §f${buildPath(node)}") {
                handLocked(false)
                rows(6)
                slots(contentSlots)
                elements { node.configs.filter { !it.ignoreGenerate } }
                applyDecoration(this)
                setPreviousPage(45) { _, has -> pageItem("上一页", has) }
                setNextPage(53) { _, has -> pageItem("下一页", has) }
                set(49, infoItem(node))
                set(48, backItem()) {
                    open(findParent(node) ?: Companion.node)
                }
                onGenerate(false) { player, element, _, _ ->
                    element.generate(1, player)
                }
                onClick { event, element ->
                    val clickEvent = event.clickEventOrNull()
                    if (clickEvent != null) {
                        when (clickEvent.click) {
                            LEFT -> viewer.giveItem(element.generate(1, viewer))
                            RIGHT -> viewer.giveItem(element.generate(64, viewer))
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}