package com.sensetime.sample.camerax.gl

import android.opengl.GLES20

/**
 * 相机输入
 */
class CameraInputFilter() :GLFilter(CAMERA_INPUT_VERTEX_SHADER, CAMRA_INPUT_FRAGMENT_SHADER) {

    var mTextureTransformMatrix :FloatArray?= null
    private var mTextureTransformMatrixLocation = GLES20.glGetUniformLocation( mGLProgramId,"textureTransform")


    protected var mFrameBuffers:IntArray? = null
    protected var mFrameBufferTextures:IntArray? = null

    private var mFrameWidth = UN_INITIALIZE
    private var mFrameHeight = UN_INITIALIZE

    override fun onInit() {
        super.onInit()

    }

    override fun onDrawFrame(textureId: Int) {
        super.onDrawFrame(textureId)
    }

    fun initCameraFrameBuffer(width:Int,height:Int ) {

        if( mFrameBuffers != null && ( mFrameWidth != width || mFrameHeight != height ) ){
            destroyFrameBuffers()
        }
        mFrameBuffers?:return

        mFrameWidth = width
        mFrameHeight = height
        mFrameBuffers = IntArray(1)
        mFrameBufferTextures = IntArray(1)

        GLES20.glGenFramebuffers( 1,mFrameBuffers,0)
        GLES20.glGenTextures( 1,mFrameBufferTextures,0 )



    }

    fun destroyFrameBuffers(){

    }


    companion object{

        const val CAMERA_INPUT_VERTEX_SHADER = "" +
                "attribute vec4 position;\n"+
                "attribute vec4 inputTextureCoordinate;\n"+
                "\n"+
                "uniform mat4 textureTransform;\n"+
                "varying vec2 textureCoordinate;\n" +
                "\n"+
                "void main()\n"+
                "{\n"+
                "   textureCoordinate = (textureTransform * inputTextureCoordinate).xy;\n" +
                "   gl_Position = position"+
                "}"

        const val CAMRA_INPUT_FRAGMENT_SHADER = ""+
                "#extension GL_OES_EGL_image_external : require\n" +
                "varying highp vec2 textureCoordinate;\n"+
                "\n"+
                "uniform samplerExternalOES inputImageTexture;\n"+
                "\n" +
                "void main() \n"+
                "{\n" +
                "   gl_FragColor = texture2D( inputImageTexture,inputTextureCoordinate)"+
                "}"
    }

}