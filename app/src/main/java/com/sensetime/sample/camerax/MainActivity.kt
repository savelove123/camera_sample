package com.sensetime.sample.camerax

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.sensetime.sample.camerax.utils.FLAGS_FULLSCREEN
import java.io.File

const val KEY_EVENT_ACTION = "key_event_action"
const val KEY_EVENT_EXTRA = "key_event_extra"
private const val DELAY_TIME = 500L
class MainActivity:AppCompatActivity() {

    private lateinit var container:FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById( R.id.fragment_container )





    }

    override fun onResume() {
        super.onResume()
        container.postDelayed( {
            container.systemUiVisibility = FLAGS_FULLSCREEN
        }, DELAY_TIME)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return when( keyCode ){
            KeyEvent.KEYCODE_VOLUME_DOWN ->{
                //TODO 使用LiveData来通知
                VolumeDownLiveData.getInstance()?.value = System.currentTimeMillis()

                true
            }else-> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        fun getOutputDirectory( context : Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let{
                File( it , "CameraX_Sample").apply { mkdirs() }
            }
            return if( mediaDir != null && mediaDir.exists() ) mediaDir else appContext.filesDir
        }
    }

}