package com.sensetime.sample.camerax.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.sensetime.sample.camerax.utils.OpenGLUtils

/**
 * 相机输入
 */
class CameraInputFilter() :GLFilter(CAMERA_INPUT_VERTEX_SHADER, CAMERA_INPUT_FRAGMENT_SHADER) {

    private var mTextureTransformMatrix :FloatArray?= null
    private var mTextureTransformMatrixLocation = GLES20.glGetUniformLocation( mGLProgramId,"textureTransform")


    protected var mFrameBuffers:IntArray? = null
    protected var mFrameBufferTextures:IntArray? = null


    override fun onInit() {
        super.onInit()
    }

    fun setTextureTransformMatrix( floatArray: FloatArray ){
        mTextureTransformMatrix = floatArray
    }


    override fun onDrawFrame(textureId: Int) :Int {

        Log.e("OpenGL", "filter draw frame start ")

        mFrameBuffers ?: return OpenGLUtils.NO_TEXTURE
        Log.e("OpenGL", "filter mFrameBuffers start ")

        mFrameBufferTextures ?: return OpenGLUtils.NO_TEXTURE
        Log.e("OpenGL", "filter mFrameBufferTextures start ")

        GLES20.glViewport( 0,0,mOutputWidth,mOutputHeight )
        GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, mFrameBuffers!![0] )
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0)

        GLES20.glUseProgram( mGLProgramId )

        if(!isInitialized() ){
            Log.e("OpenGL", "filter draw frame error is no initlized  ")

            return OpenGLUtils.NOT_INIT
        }

        mGLCubeBuffer.position( 0 )
        //由于我们顶点坐标只用了x,y 因此，size是2
        GLES20.glVertexAttribPointer( mGLAttributePosition,2,GLES20.GL_FLOAT,false,0,mGLCubeBuffer  )
        GLES20.glEnableVertexAttribArray( mGLAttributePosition )

        //绑定纹理的坐标缓冲区
        mDefaultGLTextureBuffer.position( 0 )
        GLES20.glVertexAttribPointer( mGLAttributeTextureCoordinate,2,GLES20.GL_FLOAT,false,0,mDefaultGLTextureBuffer )
        GLES20.glEnableVertexAttribArray( mGLAttributeTextureCoordinate )

        if( textureId != OpenGLUtils.NO_TEXTURE ){
            GLES20.glActiveTexture( GLES20.GL_TEXTURE0 )
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId )
            GLES20.glUniform1i( mGLUniformTexture,0 )//对应纹理的第一层
        }

        onDrawArraysPre()
        GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray( mGLAttributePosition )
        GLES20.glDisableVertexAttribArray( mGLAttributeTextureCoordinate )

        GLES20.glBindTexture( GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)


        GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, 0 )
        GLES20.glViewport( 0,0,mSurfaceWidth,mSurfaceHeight )
        Log.e("OpenGL", "filter draw frame finish ")

        return mFrameBufferTextures!![0]
    }

    fun initCameraFrameBuffer(width:Int,height:Int ) {

        if( mFrameBuffers != null && ( mOutputWidth != width || mOutputHeight != height ) ){
            destroyFrameBuffers()
        }
        mOutputHeight = height
        mOutputWidth = width
        onOutputSizeChanged( mOutputWidth, mOutputHeight )

        mFrameBuffers = IntArray(1)
        mFrameBufferTextures = IntArray(1)
        //生成一个FrameBuffer
        GLES20.glGenFramebuffers( 1,mFrameBuffers,0)
        //生成一个FrameBuffer纹理,控制帧缓冲区
        GLES20.glGenTextures( 1,mFrameBufferTextures,0 )

        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mFrameBufferTextures!![0])
        GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D , 0 , GLES20.GL_RGBA, width,height,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat() )
        GLES20.glTexParameterf( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat() )
        GLES20.glTexParameterf( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat() )

        GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER, mFrameBuffers!![0])
        GLES20.glFramebufferTexture2D( GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D, mFrameBufferTextures!![0],0)

        //清除和之前的绑定
        //OpenGL是状态机，当使用bindTexture绑定一张纹理以后，不绑定新的，那么我们操作都会对应这个纹理
        //因此我们这时候暂时解除绑定
        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D,0)
        // 绑定帧缓冲区对象时，先前的绑定会自动中断
        GLES20.glBindFramebuffer( GLES20.GL_FRAMEBUFFER,0)

    }

    fun destroyFrameBuffers(){
        mFrameBufferTextures ?: return
        mFrameBuffers ?: return

        GLES20.glDeleteBuffers( 1,mFrameBuffers!!,0)
        GLES20.glDeleteTextures(1,mFrameBufferTextures ,0 )

    }


    companion object{
        private const val CAMERA_INPUT_VERTEX_SHADER = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "\n" +
                "uniform mat4 textureTransform;\n" +
                "varying vec2 textureCoordinate;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "	textureCoordinate = (textureTransform * inputTextureCoordinate).xy;\n" +
                "	gl_Position = position;\n" +
                "}"

        private const val CAMERA_INPUT_FRAGMENT_SHADER = "" +
                "#extension GL_OES_EGL_image_external : require\n" +
                "varying highp vec2 textureCoordinate;\n" +
                "\n" +
                "uniform samplerExternalOES inputImageTexture;\n" +
                "\n" +
                "void main()\n" +
                "{\n" +
                "	gl_FragColor = vec4(0.0,1.0,0.0,0.0);\n" +
                "}"
    }

}