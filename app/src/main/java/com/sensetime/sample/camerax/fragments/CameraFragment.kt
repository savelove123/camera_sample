package com.sensetime.sample.camerax.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sensetime.sample.camerax.MainActivity
import com.sensetime.sample.camerax.R
import com.sensetime.sample.camerax.VolumeDownLiveData
import com.sensetime.sample.camerax.databinding.CameraUiContainerBinding
import com.sensetime.sample.camerax.gl.GLPreviewBuilder
import com.sensetime.sample.camerax.ui.GLTextureView
import com.sensetime.sample.camerax.utils.ANIMATION_FAST_MILLIS
import com.sensetime.sample.camerax.utils.ANIMATION_SLOW_MILLIS
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

//使用CameraX拍摄视频
//对外接口是打开摄像头、关闭摄像头、视频输入、视频输出、视频滤镜处理

typealias LumaListener = (luma:Double) -> Unit

class CameraFragment:Fragment() {

    private lateinit var cameraContainer:ConstraintLayout
    private lateinit var viewFinder: GLTextureView
    private lateinit var outputDirectory:File

    //TODO 使用LiveData
    //
    private var displayId:Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var isRecord = false
    private var imageCapture:ImageCapture ? = null
    private var videoCapture:VideoCapture ? = null
    private var imageAnalyzer:ImageAnalysis ?= null
    private var camera: Camera?=null
    private var binding : CameraUiContainerBinding ?= null
    private val displayManager by lazy {
        requireContext().getSystemService( Context.DISPLAY_SERVICE) as DisplayManager
    }
    private lateinit var cameraExecutor:ExecutorService

    //TODO 使用LiveData接受数据
    //private val volumeDownReceiver = object : xxxx

    private val displayListener = object:DisplayManager.DisplayListener{

        override fun onDisplayAdded(displayId: Int) =Unit

        override fun onDisplayRemoved(displayId: Int)=Unit
        override fun onDisplayChanged(displayId: Int) = view?.let{
            if( displayId == this@CameraFragment.displayId ){
                imageCapture?.targetRotation = it.display.rotation
                imageAnalyzer?.targetRotation = it.display.rotation
            }
        }?:Unit
    }

    override fun onResume(){
        super.onResume()

        if( !PermissionsFragment.hasPermission( requireContext() ) ){
            Navigation.findNavController( requireActivity(), R.id.fragment_container )
                    .navigate( CameraFragmentDirections.actionCameraToPermissions() )

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //关闭后台线程池
        cameraExecutor.shutdown()

        //TODO 注销LiveData观察
        displayManager.unregisterDisplayListener( displayListener )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_camera,container,false)

    private fun setGalleryThumbnail( uri: Uri){
        val thumbnail = cameraContainer.findViewById<ImageButton>(R.id.photo_view_button)
        thumbnail.post {
            thumbnail.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())
            Glide.with( thumbnail).load( uri )
                    .apply( RequestOptions.circleCropTransform() )
                    .into( thumbnail )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraContainer = view as ConstraintLayout
        viewFinder = cameraContainer.findViewById(R.id.view_finder )

        cameraExecutor = Executors.newSingleThreadExecutor()

        //

        VolumeDownLiveData.getInstance()?.apply {

            var observer = Observer<Long>{
                if( (System.currentTimeMillis()-it)<50L ){
                    view.findViewById<ImageButton>(R.id.camera_capture_button).callOnClick()
                }
            }
            this.observe( viewLifecycleOwner,observer )
        }

        displayManager.registerDisplayListener( displayListener , null )

        outputDirectory = MainActivity.getOutputDirectory( requireContext() )

        viewFinder.post{
            displayId = viewFinder.display.displayId
            updateCameraUi()
            bindCameraUseCases()

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCameraUi()
    }



    @SuppressLint("RestrictedApi")
    fun bindCameraUseCases(){
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics( it )  }

        val screenAspectRatio = aspectRatio( metrics.widthPixels,metrics.heightPixels )

        val rotation = viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing( lensFacing ).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance( requireContext())
        cameraProviderFuture.addListener( Runnable {
            val cameraProvider :ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().setTargetAspectRatio( screenAspectRatio )
                   .setTargetRotation( rotation ).build()
            preview = GLPreviewBuilder.build(screenAspectRatio,rotation,viewFinder,cameraExecutor )

            imageCapture = ImageCapture.Builder()
                    .setCaptureMode( ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY )
                    .setTargetAspectRatio( screenAspectRatio )
                    .setTargetRotation( rotation )
                    .build()
            videoCapture = VideoCaptureConfig.Builder().apply{
                setTargetAspectRatio( screenAspectRatio )
                setTargetRotation( rotation )
                setVideoFrameRate(30)
            }.build()


            imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio( screenAspectRatio )
                    .setTargetAspectRatio( rotation)
                    .build().also {
                        it.setAnalyzer( cameraExecutor,LuminosityAnalyzer{
                            luma ->
                                Log.d(TAG,"Average luminosity:$luma")
                        }  )
                    }

            cameraProvider.unbindAll()
            try {
                camera = if(isRecord ){
                    cameraProvider.bindToLifecycle( this,cameraSelector,preview,imageCapture,videoCapture)
                }else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
                }
                if( isRecord ){
                    val videoFile = createFile( outputDirectory, FILENAME_FORMAT,VIDEO_EXTENSION )
                    val metadata = VideoCapture.Metadata()
                    videoCapture?.startRecording( videoFile,metadata,cameraExecutor,object:VideoCapture.OnVideoSavedCallback{
                        override fun onVideoSaved(file: File) {
                            Log.d(TAG,"Video save success in :${file.absolutePath}" )
                        }

                        override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                            Log.e( TAG,"Video save failed !Message :$message " , cause )
                        }

                    })


                    lifecycleScope.launch {
                        flow {
                            ( 15 downTo 0).forEach {
                                delay(1000)
                                emit( it )
                            }
                        }.flowOn(Dispatchers.Main ).onCompletion {
                            isRecord = false
                            binding?.isRecord= isRecord
                            videoCapture?.stopRecording()
                            cameraContainer.findViewById<ImageButton>(R.id.camera_capture_button).isSelected = false
                            bindCameraUseCases()
                        }.collect {
                            if(isRecord)
                            binding?.progressRate = 15 - it
                        }
                    }
                }
                //TODO
             //   preview?.setSurfaceProvider( viewFinder.createSurfaceProvider( camera?.cameraInfo ) )
            }catch ( e:Exception ){

            }
        },ContextCompat.getMainExecutor( requireContext()) )

    }

    private fun aspectRatio( width :Int , height :Int ):Int{
        val previewRatio = max( width, height ).toDouble() / min( width,height).toDouble()
        if( abs( previewRatio - RATION_4_3_VALUE ) <= abs( previewRatio - RATION_16_9_VALUE ) ){
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
    var recordMode = false
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi(){

        cameraContainer.findViewById<ConstraintLayout>(R.id.camera_ui_container )?.let{
            cameraContainer.removeView( it )
            if( isRecord ){
                isRecord = false
                videoCapture?.stopRecording()
            }
        }
        binding = CameraUiContainerBinding.inflate( layoutInflater,cameraContainer,false )
        this.binding?.progressRate = 0
        val controls = binding?.root
        cameraContainer.addView( controls )

        lifecycleScope.launch( Dispatchers.IO ){
            outputDirectory.listFiles {
                file-> EXTENSION_WHITELIST.contains( file.extension.toUpperCase( Locale.ROOT ) )
            }?.max()?.let{
                setGalleryThumbnail( Uri.fromFile( it ) )
            }
        }

        controls?.findViewById<ImageButton>(R.id.camera_capture_button)?.setOnClickListener {
            //拍照
            if( !isRecord ) {
                imageCapture?.let { imageCapture ->
                    val photoFile = createFile(outputDirectory, FILENAME_FORMAT, PHOTO_EXTENSION)
                    val metadata = ImageCapture.Metadata().apply {
                        isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                    }

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                            .setMetadata(metadata)
                            .build()
                    imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed:${exception.message}", exception)
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val saveUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setGalleryThumbnail(saveUri)
                            }

                            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(saveUri.toFile().extension)
                            MediaScannerConnection.scanFile(context, arrayOf(saveUri.toFile().absolutePath),
                                    arrayOf(mimeType)) {//不需要Path这个变量 就用下划线代替
                                _, uri ->
                                Log.d(TAG, "Image Capture sancaned into media store :$uri")
                            }
                        }
                    })

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraContainer.postDelayed({
                            cameraContainer.foreground = ColorDrawable(Color.WHITE)
                            cameraContainer.postDelayed({
                                camera_container.foreground = null
                            }, ANIMATION_FAST_MILLIS)
                        }, ANIMATION_SLOW_MILLIS)
                    }
                }
            }else{
                videoCapture?.stopRecording()
                it.isSelected = false
                isRecord = false
                bindCameraUseCases()
            }

        }

        controls?.findViewById<ImageButton>(R.id.camera_capture_button)?.setOnLongClickListener {view->
            view.isSelected = true
            isRecord = true
            binding?.isRecord =isRecord
            bindCameraUseCases()


            true
        }

        controls?.findViewById<ImageButton>(R.id.camera_switch_button)?.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing ) {
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }

            bindCameraUseCases()
        }

        controls?.findViewById<ImageButton>(R.id.photo_view_button)?.setOnClickListener {
            //TODO 去GalleryFragment
        }

    }


    private class LuminosityAnalyzer( listener:LumaListener ?= null ):ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>( 5 )
        private val listeners = ArrayList<LumaListener>().apply {  listener?.let { add( it ) } }
        private var lastAnalyzedTimeStamp = 0L
        var framesPerSencond :Double = -1.0
        private set

        fun onFrameAnalyzed( listener :LumaListener ) = listeners.add( listener )

        private fun ByteBuffer.toByteArray():ByteArray{
            rewind()
            val data = ByteArray( remaining() )
            get( data )
            return data
        }

        override fun analyze(image: ImageProxy) {

            if( listeners.isEmpty() ){
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            frameTimestamps.push( currentTime )

            while( frameTimestamps.size >= frameRateWindow ) frameTimestamps.removeLast()
            val timeStampFirst = frameTimestamps.peekFirst() ?:currentTime
            val timeStampLast = frameTimestamps.peekLast() ?:currentTime

            framesPerSencond = 1.0 / ((timeStampFirst - timeStampLast ) /
                    //初始为1帧
                    frameTimestamps.size.coerceAtLeast(1).toDouble()*1000.0)

            lastAnalyzedTimeStamp = frameTimestamps.first
            val buffer = image.planes[0].buffer

            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }

            val luma = pixels.average()

            listeners.forEach{
                 it( luma )
            }
            image.close()

            //TODO 识别人脸


        }

    }

    companion object{

        private const val TAG = "CameraFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"
        private const val RATION_4_3_VALUE = 4.0/3.0
        private const val RATION_16_9_VALUE = 16.0/9.0
        val EXTENSION_WHITELIST = arrayOf("JPG","MP4")


        private fun createFile( baseFolder:File, format:String , extension:String ) =
                File( baseFolder, SimpleDateFormat(format, Locale.CHINA)
                        .format( System.currentTimeMillis()) + extension )

    }

}