package org.gitee.nodens.module.ui

import org.bukkit.entity.Player
import org.gitee.nodens.module.item.ItemConfig
import org.gitee.nodens.module.item.ItemManager
import taboolib.common.platform.function.getDataFolder
import taboolib.library.xseries.XMaterial
import taboolib.module.configuration.Configuration
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
                        Configuration.loadFromFile(it).getKeys(false).mapNotNull { key ->
                            ItemManager.getItemConfig(key)
                        },
                        it
                    )
                }
            }
        }
    }

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
                setPreviousPage(45) { page, hasPreviousPage ->
                    if (hasPreviousPage) {
                        ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                            name = "上一页"
                        }.build()
                    } else {
                        ItemBuilder(XMaterial.LEVER).apply {
                            name = "无"
                        }.build()
                    }
                }
                setNextPage(53) { page, hasNextPage ->
                    if (hasNextPage) {
                        ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                            name = "下一页"
                        }.build()
                    } else {
                        ItemBuilder(XMaterial.LEVER).apply {
                            name = "无"
                        }.build()
                    }
                }
                onGenerate(false) { player, element, index, slot ->
                    return@onGenerate when (element) {
                        is ParentNode -> {
                            ItemBuilder(XMaterial.CHEST).apply {
                                name = element.file.nameWithoutExtension
                                lore.add("左键打开此文件夹")
                            }.build()
                        }

                        is SubNode -> {
                            ItemBuilder(XMaterial.PAPER).apply {
                                name = element.file.nameWithoutExtension
                                lore.add("左键打开此配置")
                            }.build()
                        }

                        else -> error("Unknown element type ${element.file.name}")
                    }
                }
                onClick { event, element ->
                    open(element)
                }
            }
        } else if (node is SubNode) {
            viewer.openMenu<PageableChestImpl<ItemConfig>> {
                handLocked(false)
                rows(6)
                slots((0..44).toList())
                elements { node.configs }
                setPreviousPage(45) { page, hasPreviousPage ->
                    if (hasPreviousPage) {
                        ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                            name = "上一页"
                        }.build()
                    } else {
                        ItemBuilder(XMaterial.LEVER).apply {
                            name = "无"
                        }.build()
                    }
                }
                setNextPage(53) { page, hasNextPage ->
                    if (hasNextPage) {
                        ItemBuilder(XMaterial.REDSTONE_TORCH).apply {
                            name = "下一页"
                        }.build()
                    } else {
                        ItemBuilder(XMaterial.LEVER).apply {
                            name = "无"
                        }.build()
                    }
                }
                onGenerate(false) { player, element, index, slot ->
                    element.generate(1, player)
                }
                onClick { event, element ->
                    viewer.giveItem(element.generate(1, viewer))
                }
            }
        }
    }
}