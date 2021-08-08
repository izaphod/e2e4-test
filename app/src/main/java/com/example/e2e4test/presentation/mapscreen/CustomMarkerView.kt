package com.example.e2e4test.presentation.mapscreen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import com.example.e2e4test.R

class CustomMarkerView {

    @SuppressLint("InflateParams")
    fun create(context: Context, onCLick: (view: View) -> Unit): View {
        val customMarkerView =
            LayoutInflater.from(context).inflate(R.layout.marker_view, null)
        customMarkerView.layoutParams =
            ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        val icon = customMarkerView.findViewById<ImageView>(R.id.marker_icon)
        val onMarkerClickListener = View.OnClickListener {
            onCLick(it)
        }
        icon.setOnClickListener(onMarkerClickListener)
        return customMarkerView
    }
}