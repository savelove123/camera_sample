package com.sensetime.sample.camerax.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import com.sensetime.sample.camerax.ui.GLTextureView
import java.lang.Exception
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 使用两个滤镜对相机输出的图像进行处理
 * 第一个是把相机输出的外部纹理转成texture_2d
 * 第二个是把这个texture_2d当成一个texture,那么我们就可以进行处理了
 * 处理完以后，我们可以把处理结果输出(待完成)
 */
class CameraGLRenderer(var mCameraSurfaceTexture: SurfaceTexture, mCameraTextureId:Int, mGLTextureView: GLTextureView,var config:Config) :GLRenderer(mGLTextureView){

    private var mCameraInputFilter:CameraInputFilter = CameraInputFilter()
    private var mGLFilter :GLFilter = GLFilterFactory.getFilter( GLFilterFactory.NONE)
    private var mFilterType = GLFilterFactory.NONE

    private var mUiHandler = Handler(Looper.getMainLooper())

    init{
        mTextureId = mCameraTextureId
        mCameraSurfaceTexture.setOnFrameAvailableListener {
            mGLTextureView.requestRender()
        }

        setupRenderer()


    }

    override fun onDrawFrame(gl: GL10?) {
        Log.e("OpenGL", "renderer draw frame start ")

        GLES20.glClearColor(0.0f,0.0f,0.0f,0.0f)
        GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT and GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            mCameraSurfaceTexture.updateTexImage()
        }catch ( e:Exception ){
            e.printStackTrace()
        }

        var mtx = FloatArray(16)
        mCameraSurfaceTexture.getTransformMatrix( mtx )
        mCameraInputFilter.setTextureTransformMatrix( mtx )

        var textureId = mCameraInputFilter.onDrawFrame( mTextureId )
        Log.e("OpenGL", "renderer draw frame end ")

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

        GLES20.glViewport(0,0,width,height )

        mSurfaceHeight = width
        mSurfaceWidth = height
        onFilterChanged()

    }



    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        GLES20.glDisable( GL10.GL_DITHER )
        GLES20.glClearColor(0.0f,0.0f,0.0f,0.0f)
        GLES20.glEnable(GL10.GL_CULL_FACE)

        mCameraInputFilter.init()
        mGLFilter.init()
        onFilterChanged()
    }

    override fun onResume() {
        setupRenderer()
    }

    override fun onPause() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupRenderer(){
        setupRenderer( this.config )
    }

    private fun setupRenderer(config: Config ){

        outputImageHeight = config.outputSize.height
        outputImageWidth = config.outputSize.width

        if( config.orientation == Config.ORI_MODE_VERTICAL ) {
            mSurfaceWidth = config.previewSize.width
            mSurfaceHeight = config.previewSize.height
        }else{
            mSurfaceWidth = config.previewSize.height
            mSurfaceHeight = config.previewSize.width
        }

        //TODO 当输出的视频和预览的大小不一样的时候，我们需要调整画面显示的内容

    }

    override fun onFilterChanged() {
        super.onFilterChanged()
        mCameraInputFilter.onDisplaySizeChanged( mSurfaceWidth,mSurfaceHeight)
        mCameraInputFilter.initCameraFrameBuffer( outputImageWidth,outputImageHeight )

    }


    class Config(var previewSize: Size, var outputSize:Size ){

        var orientation = ORI_MODE_VERTICAL

        companion object{
            const val ORI_MODE_VERTICAL = 0
            const val ORI_MODE_HORIZON = 1
        }

    }



}