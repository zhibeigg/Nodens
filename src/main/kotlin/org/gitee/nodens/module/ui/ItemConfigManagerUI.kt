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

    private fun pageItem(name: String, active: Boolean) = ItemBuilder(
        if (active) XMaterial.REDSTONE_TORCH else XMaterial.LEVER
    ).apply {
        this.name = if (active) name else "无"
    }.build()

    fun open() {
        open(node)
    }

    fun open(node: Node) {
        if (node is ParentNode) {
            viewer.openMenu<PageableChestImpl<Node>> {
                handLocked(false)
                rows(6)
                slots((0..44).toList())
                elements { node.subNode }
                setPreviousPage(45) { _, has -> pageItem("上一页", has) }
                setNextPage(53) { _, has -> pageItem("下一页", has) }
                if (node != Companion.node) {
                    set(46, ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                        name = "返回"
                    }.build()) {
                        open(findParent(node) ?: Companion.node)
                    }
                }
                onGenerate(false) { _, element, _, _ ->
                    when (element) {
                        is ParentNode -> ItemBuilder(XMaterial.CHEST).apply {
                            name = element.file.nameWithoutExtension
                            lore.add("左键打开此文件夹")
                        }.build()
                        is SubNode -> ItemBuilder(XMaterial.PAPER).apply {
                            name = element.file.nameWithoutExtension
                            lore.add("左键打开此配置")
                        }.build()
                        else -> error("Unknown element type ${element.file.name}")
                    }
                }
                onClick { _, element ->
                    open(element)
                }
            }
        } else if (node is SubNode) {
            viewer.openMenu<PageableChestImpl<ItemConfig>> {
                handLocked(false)
                rows(6)
                slots((0..44).toList())
                elements { node.configs.filter { !it.ignoreGenerate } }
                setPreviousPage(45) { _, has -> pageItem("上一页", has) }
                set(46, ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                    name = "返回"
                }.build()) {
                    open(findParent(node) ?: Companion.node)
                }
                setNextPage(53) { _, has -> pageItem("下一页", has) }
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