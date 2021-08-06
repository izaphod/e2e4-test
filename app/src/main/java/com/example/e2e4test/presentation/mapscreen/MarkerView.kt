package com.example.e2e4test.presentation.mapscreen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.e2e4test.R
import com.example.e2e4test.domain.model.PlaceModel

class CustomMarkerView {

    @SuppressLint("InflateParams")
    fun create(context: Context, place: PlaceModel): View {
        val customMarkerView =
            LayoutInflater.from(context).inflate(R.layout.marker_view, null)
        customMarkerView.layoutParams =
            ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        val icon = customMarkerView.findViewById<ImageView>(R.id.marker_icon)
        val name = customMarkerView.findViewById<TextView>(R.id.marker_name)
        icon.setOnClickListener {
            if (name.isVisible) {
                name.visibility = View.GONE
            } else {
                name.text = place.name
                name.visibility = View.VISIBLE
            }
        }
        return customMarkerView
    }
}