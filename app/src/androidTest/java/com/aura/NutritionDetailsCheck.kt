package com.aura

import android.content.Intent
import android.graphics.Rect
import android.graphics.Bitmap
import java.io.File
import java.time.LocalDate
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.aura.navigation.AuraNavGraph
import com.aura.navigation.Routes
import com.aura.ui.theme.AuraTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test

class NutritionDetailsCheck {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val automation = instrument.uiAutomation

    private fun find(node: AccessibilityNodeInfo?, label: String, field: Boolean = false): AccessibilityNodeInfo? {
        if (node == null) return null
        if ((!field && node.text?.toString() == label) || node.contentDescription?.toString() == label) return node
        for (index in 0 until node.childCount) find(node.getChild(index), label, field)?.let { return it }
        return null
    }

    private fun await(label: String, field: Boolean = false): AccessibilityNodeInfo {
        repeat(50) {
            find(automation.rootInActiveWindow, label, field)?.let { return it }
            Thread.sleep(200)
        }
        error("Missing visible node: $label")
    }

    private fun tap(label: String) {
        val bounds = Rect()
        await(label).getBoundsInScreen(bounds)
        automation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").close()
        Thread.sleep(550)
    }



    private fun enter(label: String, text: String) {
        val bounds = Rect()
        await(label, field = true).getBoundsInScreen(bounds)
        automation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").close()
        Thread.sleep(350)
        automation.executeShellCommand("input text $text").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        Thread.sleep(300)
    }

    private fun shot(name: String) {
        File(instrument.targetContext.cacheDir, "aura-nutrition-$name.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun logAndReuseFood() {
        val context = instrument.targetContext
        val db = (context.applicationContext as AuraApp).db
        val dao = db.nutritionDao()
        val name = "AuraNutritionCheck" + System.currentTimeMillis()
        val date = LocalDate.now().toString()
        val activity = instrument.startActivitySync(
            Intent(context, ComponentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as ComponentActivity
        try {
            instrument.runOnMainSync {
                activity.setContent {
                    AuraTheme { AuraNavGraph(rememberNavController(), Routes.NUTRITION, Modifier.fillMaxSize()) }
                }
            }
            await("Calories remaining")
            tap("Add meal")
            val ai = await("AI estimate")
            val favoriteTab = await("Favorites")
            val manual = await("Manual")
            fun left(node: AccessibilityNodeInfo): Int = Rect().also { node.getBoundsInScreen(it) }.left
            check(left(ai) < left(favoriteTab) && left(favoriteTab) < left(manual))
            await("Open camera")
            val contextHasGemini = com.aura.data.food.FoodAi.apiKey(context) != null
            println("Gemini configured on emulator: $contextHasGemini")
            tap("Manual")
            await("Food details")
            check(find(automation.rootInActiveWindow, "Fiber (g)", true) == null)
            enter("Food name", name)
            enter("Calories (kcal)", "120")
            automation.executeShellCommand("input keyevent KEYCODE_BACK").close()
            Thread.sleep(450)
            tap("More nutrition details")
            enter("Fiber (g)", "4.5")
            automation.executeShellCommand("input keyevent KEYCODE_BACK").close()
            Thread.sleep(450)
            tap("Review meal")
            await("4.5 g")
            tap("Save as favorite")
            tap("Log food")
            await(name)
            val saved = runBlocking { dao.getLogsForDate(date).first().single { it.foodName == name } }
            check(org.json.JSONObject(saved.nutrientsJson!!).getDouble("fiber") == 4.5)
            tap(name)
            await("More nutrition details")
            await("4.5 g")
            automation.executeShellCommand("input keyevent KEYCODE_BACK").close()
            Thread.sleep(450)
            tap("Add meal")
            tap("Favorites")
            tap(name)
            await("4.5 g")
            tap("Log food")
            await("2 entries")
            check(runBlocking { dao.getLogsForDate(date).first().filter { it.foodName == name }.all {
                org.json.JSONObject(it.nutrientsJson!!).getDouble("fiber") == 4.5
            } })
        } finally {
            instrument.runOnMainSync { activity.finish() }
            runBlocking {
                dao.getLogsForDate(date).first().filter { it.foodName == name }.forEach { dao.deleteLog(it) }
                db.openHelper.writableDatabase.execSQL("DELETE FROM food_items WHERE name = ?", arrayOf(name))
            }
        }
    }
}
