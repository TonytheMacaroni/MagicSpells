package com.nisovin.magicspells.volatilecode

import org.bukkit.Bukkit

import com.nisovin.magicspells.MagicSpells
import com.nisovin.magicspells.debug.MagicDebug

object ManagerVolatile {

    private val helper = object: VolatileCodeHelper {
        override fun error(message: String) {
            MagicSpells.error(message)
        }

        override fun scheduleDelayedTask(task: Runnable?, delay: Long): Int {
            return MagicSpells.scheduleDelayedTask(task, delay)
        }
    }

    fun constructVolatileCodeHandler(): VolatileCodeHandle {
        val mcVersion = Bukkit.getMinecraftVersion()
        return try {
            val version = "v" + mcVersion.replace(".", "_")
            val volatileCode = Class.forName("com.nisovin.magicspells.volatilecode.$version.VolatileCode_$version")

            MagicDebug.info("Found volatile code handler for $mcVersion.")
            volatileCode.getConstructor(VolatileCodeHelper::class.java).newInstance(helper) as VolatileCodeHandle
        } catch (ex: Exception) {
            MagicDebug.error("Volatile code handler for $mcVersion not found, using fallback.")
            VolatileCodeDisabled()
        }
    }
}
