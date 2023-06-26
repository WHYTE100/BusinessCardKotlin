package io.pridetechnologies.businesscard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartActivity : AppCompatActivity() {

    private val constants = Constants()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        lifecycleScope.launch {
            delay(3000)
            if ( constants.currentUser == null) {
                val intent = Intent(this@StartActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
                Animatoo.animateFade(this@StartActivity)
            } else {
                val intent = Intent(this@StartActivity, HomeActivity::class.java)
                startActivity(intent)
                finish()
                Animatoo.animateFade(this@StartActivity)
            }
        }
    }
}