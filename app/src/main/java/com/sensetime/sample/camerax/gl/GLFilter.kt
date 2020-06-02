package com.sensetime.sample.camerax.gl

import android.opengl.GLES20
import com.sensetime.sample.camerax.base.UIComponent
import com.sensetime.sample.camerax.utils.OpenGLUtils
import com.sensetime.sample.camerax.utils.TextureRotationUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*

//OpenGL相关操作
 open class GLFilter(private var mVertexShader: String = NO_FILTER_VERTEX_SHADER,private var mFragmentShader:String= NO_FILTER_FRAGMENT_SHADER) :UIComponent{

    protected val mRunOnDraw:LinkedList<Runnable> = LinkedList()
    //program id
    protected var mGLProgramId = UN_INITIALIZE
    //attribute id
    protected var mGLAttributePosition = UN_INITIALIZE
    protected var mGLUniformTexture = UN_INITIALIZE
    protected var mGLAttributeTextureCoordinate = UN_INITIALIZE
//    protected var mGLStrengthLocation = UN_INITIALIZE
    protected var mOutputWidth = UN_INITIALIZE
    protected var mOutputHeight = UN_INITIALIZE
    protected var mIsInitialized = false


    protected var mGLCubeBuffer :FloatBuffer = ByteBuffer.allocateDirect( TextureRotationUtil.CUBE.size *4 )
            .order( ByteOrder.nativeOrder()).asFloatBuffer()
    protected var mDefaultGLTextureBuffer:FloatBuffer = ByteBuffer.allocateDirect( TextureRotationUtil.TEXTURE_NO_ROTATION.size * 4 )
            .order( ByteOrder.nativeOrder()).asFloatBuffer()
    protected var mSurfaceWidth = UN_INITIALIZE
    protected var mSurfaceHeight = UN_INITIALIZE

    init{

        mGLCubeBuffer.put( TextureRotationUtil.CUBE ).position(0)

        mDefaultGLTextureBuffer.put( TextureRotationUtil.TEXTURE_NO_ROTATION).position( 0 )

    }

    open fun init(){
        onInit()
        mIsInitialized = true
        onInitialized()
    }

    protected open fun onInit(){
        mGLProgramId = OpenGLUtils.loadProgram( mVertexShader,mFragmentShader )
        mGLAttributePosition = GLES20.glGetAttribLocation( mGLProgramId, GL_NAME_POSITION )
        mGLAttributeTextureCoordinate = GLES20.glGetAttribLocation(mGLProgramId, GL_NAME_TEXTURE_COORDINATE)
        mGLUniformTexture = GLES20.glGetUniformLocation( mGLProgramId, GL_NAME_IMAGE_TEXTURE )

    }

    protected open fun onInitialized(){

    }

    fun isInitialized() = mIsInitialized

    fun getProgram() = mGLProgramId

    protected fun runOnDraw( runnable: Runnable){
        synchronized(lock = mRunOnDraw){
            mRunOnDraw.addLast( runnable )
        }
    }



    fun ifNeedInit(){
        if( !isInitialized() ){
            init()
        }
    }

    fun onOutputSizeChanged(width:Int , height :Int){
        mOutputWidth = width
        mOutputHeight = height
    }

    fun onDisplaySizeChanged( width: Int , height: Int ){
        mSurfaceWidth = width
        mSurfaceHeight = height

    }

    fun unLoad(){
        synchronized( mIsInitialized ) {
            if(mIsInitialized ) {
                GLES20.glDeleteProgram(mGLProgramId)
                onDestroy()
                mIsInitialized = false
            }
        }
    }

    open fun onDrawFrame(textureId:Int , cubeBuffer:FloatBuffer,textureBuffer:FloatBuffer ):Int {

        GLES20.glUseProgram( mGLProgramId )
        runPendingOnDrawTasks()

        if(!isInitialized() ){
            return OpenGLUtils.NOT_INIT
        }

        cubeBuffer.position( 0 )
        //由于我们顶点坐标只用了x,y 因此，size是2
        GLES20.glVertexAttribPointer( mGLAttributePosition,2,GLES20.GL_FLOAT,false,0,cubeBuffer  )
        GLES20.glEnableVertexAttribArray( mGLAttributePosition )

        //绑定纹理的坐标缓冲区
        textureBuffer.position( 0 )
        GLES20.glVertexAttribPointer( mGLAttributeTextureCoordinate,2,GLES20.GL_FLOAT,false,0,textureBuffer )
        GLES20.glEnableVertexAttribArray( mGLAttributeTextureCoordinate )

        if( textureId != OpenGLUtils.NO_TEXTURE ){
            GLES20.glActiveTexture( GLES20.GL_TEXTURE0 )
            GLES20.glBindTexture( GLES20.GL_TEXTURE_2D,textureId )
            GLES20.glUniform1i( mGLUniformTexture,0 )//对应纹理的第一层
        }

        onDrawArraysPre()
        GLES20.glDrawArrays( GLES20.GL_TRIANGLE_STRIP,0,4)
        GLES20.glDisableVertexAttribArray( mGLAttributePosition )
        GLES20.glDisableVertexAttribArray( mGLAttributeTextureCoordinate )

        GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, 0)


        return OpenGLUtils.ON_DRAWN
    }

    open fun onDrawFrame(textureId: Int ):Int {
        return onDrawFrame( textureId, mGLCubeBuffer, mDefaultGLTextureBuffer )
    }


    protected open fun onDrawArraysPre() = Unit

    protected open fun onDrawArraysAfter() = Unit

    override fun onResume() = Unit

    override fun onPause() = Unit

    override fun onStart() = Unit

    override fun onStop() = Unit

    override fun onDestroy() = Unit


    private fun runPendingOnDrawTasks(){
        while(!mRunOnDraw.isEmpty() ) {
            mRunOnDraw.removeFirst().run()
        }
    }

    protected fun setInteger( location :Int , value :Int ){
        runOnDraw(Runnable {
            ifNeedInit()
            GLES20.glUniform1i(location, value)
        })
    }

    protected fun setFloat( location :Int , value :Float ){
        runOnDraw( Runnable {
            ifNeedInit()
            GLES20.glUniform1f( location, value)
        })
    }


    companion object {

        const val UN_INITIALIZE=-1

        const val GL_NAME_POSITION = "position"
        const val GL_NAME_TEXTURE_COORDINATE = "inputTextureCoordinate"
        const val GL_NAME_IMAGE_TEXTURE = "inputImageTexture"

        const val NO_FILTER_VERTEX_SHADER = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "}"
        const val NO_FILTER_FRAGMENT_SHADER = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                "uniform sampler2D inputImageTexture;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}"
    }


}