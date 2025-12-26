/**
 * Fire.js - JavaScript 扩展属性示例
 *
 * 可用的全局变量 (由 Java 注入):
 * - keys: 属性匹配关键字列表
 * - valueType: 值类型 ("SINGLE" 或 "RANGE")
 * - combatPower: 战斗力系数
 * - priority: 同步优先级
 */

function sync(entitySyncProfile, valueMap) {
    // 同步属性到实体 (如生命值、移速等)
}

function handleAttacker(damageProcessor, valueMap) {
    // 处理攻击者属性
}

function handleDefender(damageProcessor, valueMap) {
    // 处理防御者属性
}

function handleHealer(regainProcessor, valueMap) {
    // 处理治疗者属性
}

function handlePassive(regainProcessor, valueMap) {
    // 处理被动回复属性
}

function combatPower(valueMap) {
    // 计算战斗力
    return 0;
}