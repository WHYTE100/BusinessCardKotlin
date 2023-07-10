package io.pridetechnologies.businesscard

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo
import io.pridetechnologies.businesscard.databinding.ActivitySocialMediaBinding


class SocialMediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySocialMediaBinding
    private val constants = Constants()
    private var currentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySocialMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        currentId = intent.getStringExtra("user_id").toString()

        constants.db.collection("social_media").document(currentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val whatsAppLink = snapshot.get("whatsapp_link").toString()
                    val facebookLink = snapshot.get("facebook_link").toString()
                    val linkedInLink = snapshot.get("linked_in_link").toString()
                    val twitterLink = snapshot.get("twitter_link").toString()
                    val youtubeLink = snapshot.get("youtube_link").toString()
                    val instagramLink = snapshot.get("instagram_link").toString()
                    val weChatLink = snapshot.get("wechat_link").toString()
                    val tiktokLink = snapshot.get("tiktok_link").toString()

                    if (!facebookLink.equals("null")){
                        binding.facebookTextView.text = facebookLink
                    }
                    if (!whatsAppLink.equals("null")){
                        binding.whatsappTextView.text = whatsAppLink
                    }
                    if (!linkedInLink.equals("null")){
                        binding.linkedInTextView.text = linkedInLink
                    }
                    if (!twitterLink.equals("null")){
                        binding.twitterTextView.text = twitterLink
                    }
                    if (!youtubeLink.equals("null")){
                        binding.youtubeTextView.text = youtubeLink
                    }
                    if (!instagramLink.equals("null")){
                        binding.instagramTextView.text = instagramLink
                    }
                    if (!weChatLink.equals("null")){
                        binding.wechatTextView.text = weChatLink
                    }
                    if (!tiktokLink.equals("null")){
                        binding.tikTokTextView.text = tiktokLink
                    }

                } else {
                    Log.d(TAG, "Current data: null")
                }
            }

        binding.addFbButton.setOnClickListener {
            val message = "Type or paste your Facebook link below."
            addLink("facebook_link", message)
        }
        binding.addWhatsappButton.setOnClickListener{
            val message = "Type or paste your WhatsApp number below."
            addLink("whatsapp_link", message)
        }
        binding.addLinkedInButton.setOnClickListener{
            val message = "Type or paste your LinkedIn link below."
            addLink("linked_in_link", message)
        }
        binding.addTwitterButton.setOnClickListener{
            val message = "Type or paste your Twitter link below."
            addLink("twitter_link", message)
        }
        binding.addTiktokButton.setOnClickListener{
            val message = "Type or paste your TikTok link below."
            addLink("tiktok_link", message)
        }
        binding.addWechatButton.setOnClickListener{
            val message = "Type or paste your WeChat link below."
            addLink("wechat_link", message)
        }
        binding.addInstagramButton.setOnClickListener{
            val message = "Type or paste your Instagram link below."
            addLink("instagram_link", message)
        }
        binding.addYoutubeBtn.setOnClickListener{
            val message = "Type or paste your Youtube link below."
            addLink("youtube_link", message)
        }
    }

    private fun addLink(linkValue: String, message: String) {
        val intent = Intent(this, AddSocialMediaActivity::class.java)
        intent.putExtra("link_value", linkValue)
        intent.putExtra("message", message)
        intent.putExtra("user_id", currentId)
        startActivity(intent)
        Animatoo.animateFade(this)
    }
}