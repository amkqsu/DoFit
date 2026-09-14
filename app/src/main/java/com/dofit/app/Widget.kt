package com.dofit.app

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.graphics.Color

class DoFitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(GlanceModifier.fillMaxSize().padding(16.dp)) {
                Text("DoFit", style = TextStyle(color = ColorProvider(Color.WHITE)))
                Text("Open DoFit to update today", style = TextStyle(color = ColorProvider(Color.LTGRAY)))
            }
        }
    }
}

class DoFitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DoFitWidget()
}
