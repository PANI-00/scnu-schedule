package com.scnu.schedule.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text

/** 下一节课 2×1。Task 1 冒烟：固定文本；Task 3 接入真实数据。 */
class NextClassWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(GlanceModifier.fillMaxSize()) {
                Text("下一节课")
            }
        }
    }
}
