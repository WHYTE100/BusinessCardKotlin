package io.pridetechnologies.businesscard

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import io.pridetechnologies.businesscard.activities.NewBusinessCardActivity
import io.pridetechnologies.businesscard.activities.NewCardActivity

class ScanIndividualActivity : AppCompatActivity() {

    private var cardKey: String? = null
    private var qrScan: IntentIntegrator? = null
    var result: IntentResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_individual)

        qrScan = IntentIntegrator(this@ScanIndividualActivity)
        qrScan!!.setOrientationLocked(true)
        qrScan!!.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        qrScan!!.setPrompt("Scan Individual QR Code")
        qrScan!!.setCameraId(0) // Use a specific camera of the device
        qrScan!!.setBeepEnabled(false)
        qrScan!!.setBarcodeImageEnabled(true)
        qrScan!!.initiateScan()
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        //result2 = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && resultCode == Activity.RESULT_OK && data != null) {
            if (result!!.contents == null) {
                //Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                cardKey = result!!.contents.toString().trim { it <= ' ' }
                val intent = Intent(this@ScanIndividualActivity, NewCardActivity::class.java)
                intent.putExtra("deepLink", cardKey)
                startActivity(intent)
                finish()
                Animatoo.animateFade(this@ScanIndividualActivity)
            }
        } else if (result == null && resultCode == Activity.RESULT_CANCELED){
            finish()
        }else {
            super.onActivityResult(requestCode, resultCode, data)
            finish()
        }
    }
}