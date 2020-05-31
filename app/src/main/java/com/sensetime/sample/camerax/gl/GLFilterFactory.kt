package com.sensetime.sample.camerax.gl

class GLFilterFactory {

    companion object{

        const val NONE=0

        fun getFilter( type:Int ):GLFilter{
            return GLFilter()
        }
    }
}