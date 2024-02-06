package io.pridetechnologies.businesscard

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import io.pridetechnologies.businesscard.activities.NewBusinessCardActivity
import io.pridetechnologies.businesscard.activities.NewCardActivity

class ScanBusinessActivity : AppCompatActivity() {

    private var cardKey: String? = null
    private var qrScan: IntentIntegrator? = null
    private var result: IntentResult? = null
    private val constants = Constants()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_business)

        qrScan = IntentIntegrator(this@ScanBusinessActivity)
        qrScan!!.setOrientationLocked(true)
        qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        qrScan!!.setPrompt("Scan Business QR Code")
        qrScan!!.setCameraId(0) // Use a specific camera of the device
        qrScan!!.setBeepEnabled(false)
        qrScan!!.setBarcodeImageEnabled(true)
        qrScan!!.initiateScan()
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && resultCode == Activity.RESULT_OK && data != null) {
            if (result!!.contents == null) {
                //Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                cardKey = result!!.contents.toString().trim { it <= ' ' }
                val uri = Uri.parse(cardKey)
                val path = uri?.path
                if (path != null) {
                    // Determine the appropriate activity based on the deep link path
                    when {
                        path.contains("/individuals") -> {
                            val uid = uri.getQueryParameter("key")
                            val intent1 = Intent(this, NewCardActivity::class.java)
                            intent1.putExtra("deepLink", uid.toString())
                            startActivity(intent1)
                            finish()
                            Animatoo.animateFade(this@ScanBusinessActivity)

                        }
                        path.contains("/businesses") -> {
                            val uid = uri.getQueryParameter("key")
                            val intent2 = Intent(this, NewBusinessCardActivity::class.java)
                            intent2.putExtra("business_id", uid.toString())
                            startActivity(intent2)
                            finish()
                            Animatoo.animateFade(this@ScanBusinessActivity)
                        }
                        // Add more cases for different paths if needed
                        else -> {
                            constants.showToast(this, "This code is invalid")
                        }
                    }
                }
            }
        } else if (result == null && resultCode == Activity.RESULT_CANCELED){
            finish()
        }else {
            super.onActivityResult(requestCode, resultCode, data)
            finish()
        }
    }

}