package com.uu_uce

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_profile.*

class Profile : AppCompatActivity() {

    private lateinit var selectedOptionText : TextView
    private lateinit var selectedOptionBar : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        createTopbar(this, "Profile")

        selectedOptionText = badges_button_text
        selectedOptionBar = badges_button_bar

        badges_button.setOnClickListener{
            if(selectedOptionBar != badges_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                badges_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                badges_button_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.HighBlue))

                selectedOptionText = badges_button_text
                selectedOptionBar  = badges_button_bar

                // TODO: switch fragment here
            }
        }

        achievements_button.setOnClickListener{
            if(selectedOptionBar != achievements_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                achievements_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                achievements_button_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.HighBlue))

                selectedOptionText = achievements_button_text
                selectedOptionBar  = achievements_button_bar

                // TODO: switch fragment here
            }
        }

        statistics_button.setOnClickListener{
            if(selectedOptionBar != statistics_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                statistics_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                statistics_button_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.HighBlue))

                selectedOptionText = statistics_button_text
                selectedOptionBar  = statistics_button_bar

                // TODO: switch fragment here
            }
        }
    }

}
