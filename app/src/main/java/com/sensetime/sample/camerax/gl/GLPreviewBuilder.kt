package com.sensetime.sample.camerax.gl

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.camera.core.Preview
import androidx.core.util.Consumer
import com.sensetime.sample.camerax.ui.GLTextureView
import com.sensetime.sample.camerax.utils.OpenGLUtils
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.N)
@SuppressLint("RestrictedApi")
class GLPreviewBuilder private constructor( aspectRatio: Int,rotation: Int,viewFinderRef:WeakReference<GLTextureView> ,executor: Executor) {

    //预览的实例
    val useCase:Preview
    //内部变量，来控制用例输出的旋转方向
    private var bufferRotation:Int = 0
    //内部变量，来跟踪控制试图的方向
    private var viewFinderRotation:Int? = null

    private var bufferDimens: Size = Size( 0,0)
    private var viewFinderDimens:Size = Size(0,0)
    private var viewFinderDisplay :Int = -1

    private lateinit var displayManager:DisplayManager
    lateinit var cameraSurfaceTexture:SurfaceTexture
    private var mCameraTextureId : Int = -1

    private val displayListener = object:DisplayManager.DisplayListener{
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            val viewFinder = viewFinderRef.get() ?:return
            if( displayId != viewFinderDisplay ){
                val display = displayManager.getDisplay( displayId )
                val rotation = getDisplaySurfaceRotation( display )
                updateTransform( viewFinder,rotation,bufferDimens,viewFinderDimens )
            }
        }
    }

    private var mOESTextureId = -1
    private var mFilter :GLFilter = GLFilter()


    init{

        val viewFinder = viewFinderRef.get() ?: throw IllegalArgumentException(
                "Invalid referernce to view finder used " )
        viewFinderDisplay = viewFinder.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation( viewFinder.display ) ?:0

        useCase = Preview.Builder().apply {
            setTargetAspectRatio( aspectRatio)
            setTargetRotation( rotation)
            }.build()

        useCase.setSurfaceProvider {
            if( isShuttingDown( viewFinder ) ){
                it.willNotProvideSurface()
                return@setSurfaceProvider
            }

            var surface = createGLInputSurface( it.resolution, viewFinder)

            it.provideSurface( surface, executor,  Consumer { closeInputSurface(surface)  } )
        }

    }
    @SuppressLint("Recycle")
    private fun createGLInputSurface(size: Size, viewFinder: GLTextureView): Surface {

        val parent = viewFinder.parent as ViewGroup
        parent.removeView( viewFinder)
        parent.addView( viewFinder ,0)
        //绑定外部纹理ID
//        viewFinder.surfaceTexture = SurfaceTexture( OpenGLUtils.getExternalOESTextureID() )
        //相机输出到外部纹理上去
        //然后CameraInputFilter对外部纹理进行处理得到相机的画面
        //然后是滤镜对相机的画面进行二次处理
        mCameraTextureId =  OpenGLUtils.getExternalOESTextureID()
        cameraSurfaceTexture = SurfaceTexture(mCameraTextureId )
        cameraSurfaceTexture.setOnFrameAvailableListener {
            viewFinder.requestRender()
        }

        viewFinder.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?)=Unit

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {

                //TODO 初始化OPENGL把相机输出到SurfaceTexture的图形纹理，显示在GLTextureView上
                val glRenderer:GLRenderer = CameraGLRenderer( cameraSurfaceTexture, mCameraTextureId, viewFinder)
                //TODO 对Surface纹理进行滤镜，比如黑白滤镜
                glRenderer.setFilter( GLFilterFactory.NONE)
                viewFinder.setRenderer( glRenderer )
                viewFinder.renderMode = GLTextureView.RENDERMODE_WHEN_DIRTY
            }

        }

        //当大小调整的时候相应调整
        updateTransform( viewFinder, getDisplaySurfaceRotation( viewFinder.display), size,viewFinderDimens )
        //预览画面会输出到CameraSurfaceTexture纹理当中
        return Surface( cameraSurfaceTexture )
    }

    private fun isShuttingDown( textureView: TextureView ) =  textureView.isAvailable

    private fun closeInputSurface(surface: Surface){
        cameraSurfaceTexture.release()
    }


    private fun updateTransform( textureView:TextureView? , rotation:Int ?,newBufferDimens:Size,newViewFinderDimens:Size){

    }

    companion object{

        fun getDisplaySurfaceRotation( display: Display?) = when( display?.rotation){
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else ->null

        }

        fun build( aspectRatio: Int , rotation: Int,viewFinder:GLTextureView,executor: Executor) =
                GLPreviewBuilder( aspectRatio,rotation, WeakReference( viewFinder ),executor ).useCase
    }

}