package com.sensetime.sample.camerax.gl

import android.os.Build
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.N)
class GLSurfaceProvider(var executor :Executor , var viewFinder:TextureView) :Preview.SurfaceProvider{

    override fun onSurfaceRequested(request: SurfaceRequest) {

        if( isShuttingDown() ){
            request.willNotProvideSurface()
        }

        var surface = createGLInputSurface( request.resolution)

        request.provideSurface( surface,executor,
        (androidx.core.util.Consumer<SurfaceRequest.Result> {
           closeGLInputSurface( surface )
        }))

    }

    fun createGLInputSurface( size: Size): Surface {
        //当大小调整的时候相应调整

        var surface = Surface( viewFinder.surfaceTexture )

        return surface
    }



    fun closeGLInputSurface( surface:Surface){

    }

    private fun isShuttingDown() = viewFinder.isAvailable

}