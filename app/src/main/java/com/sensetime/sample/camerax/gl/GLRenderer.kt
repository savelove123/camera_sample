package com.sensetime.sample.camerax.gl

import com.sensetime.sample.camerax.base.UIComponent
import com.sensetime.sample.camerax.ui.GLTextureView
import com.sensetime.sample.camerax.utils.OpenGLUtils
import com.sensetime.sample.camerax.utils.TextureRotationUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class GLRenderer(var mGLTextureView: GLTextureView) :GLTextureView.Renderer,UIComponent{

    protected var glFilter:GLFilter?
    protected var glFilterType:Int

    protected var mTextureId:Int = OpenGLUtils.NO_TEXTURE

    //顶点坐标
    protected val mGLCubeBuffer:FloatBuffer

    //编码使用的纹理坐标
    protected val mEncodeGLTextureBuffer:FloatBuffer

    //预览使用的纹理坐标
    protected val mPreviewGLTextureBuffer:FloatBuffer

    protected var mSurfaceWidth:Int = 0
    protected var mSurfaceHeight:Int = 0

    protected var outputImageWidth:Int = 0
    protected var outputImageHeight:Int = 0

    init{
        glFilter = GLFilterFactory.getFilter( GLFilterFactory.NONE )
        glFilterType =GLFilterFactory.NONE

        mGLCubeBuffer = ByteBuffer.allocateDirect( TextureRotationUtil.CUBE.size*4 )
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        mGLCubeBuffer.put( TextureRotationUtil.CUBE).position(0)

        mEncodeGLTextureBuffer = ByteBuffer.allocateDirect( TextureRotationUtil.TEXTURE_NO_ROTATION.size *4 )
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mEncodeGLTextureBuffer.put( TextureRotationUtil.TEXTURE_NO_ROTATION).position(0)

        mPreviewGLTextureBuffer = ByteBuffer.allocateDirect( TextureRotationUtil.TEXTURE_NO_ROTATION.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        mPreviewGLTextureBuffer.put( TextureRotationUtil.TEXTURE_NO_ROTATION ).position(0)


    }

    protected open fun onFilterChanged(){
        glFilter?:return

        glFilter?.onDisplaySizeChanged( mSurfaceWidth,mSurfaceHeight )
        glFilter?.onOutputSizeChanged( outputImageWidth,outputImageHeight )
    }

    fun setFilter( filterType:Int ){
        mGLTextureView.queueEvent{

            glFilter?:return@queueEvent

            glFilter?.onDestroy()
            glFilter = GLFilterFactory.getFilter( filterType )
            glFilter?.init()
            onFilterChanged()
        }
        mGLTextureView.requestRender()
        this.glFilterType = filterType
    }

    override fun onDrawFrame(gl: GL10?)=Unit

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)=Unit

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) =Unit

    override fun onResume() =Unit

    override fun onPause() =Unit
    override fun onStart() =Unit

    override fun onStop() =Unit

    override fun onDestroy() =Unit

}