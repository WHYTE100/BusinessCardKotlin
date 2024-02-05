package io.pridetechnologies.businesscard

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.RemoteViews


/**
 * Implementation of App Widget functionality.
 */
class BusinessCardWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val constants = Constants()
    val individualIntent = Intent(context, ScanIndividualActivity::class.java)
    val individualPendingIntent = PendingIntent.getActivity(context, 0, individualIntent,
        PendingIntent.FLAG_IMMUTABLE)
    val businessIntent = Intent(context, ScanBusinessActivity::class.java)
    val businessPendingIntent = PendingIntent.getActivity(context, 0, businessIntent,
        PendingIntent.FLAG_IMMUTABLE)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.business_card_widget)
    val userCode = constants.readFromSharedPreferences(context, "user_qr_code", "")
    if (userCode != ""){
        val b: ByteArray = Base64.decode(userCode, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
        views.setImageViewBitmap(R.id.qrCodeImageView, bitmap)
    }else{
        views.setImageViewResource(R.id.qrCodeImageView, R.drawable.qr_code_black)
    }
    views.setOnClickPendingIntent(R.id.openScanIndividualButton, individualPendingIntent)
    views.setOnClickPendingIntent(R.id.openScanBusinessButton, businessPendingIntent)
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}