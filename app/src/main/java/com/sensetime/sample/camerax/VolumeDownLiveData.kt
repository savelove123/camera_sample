package com.sensetime.sample.camerax

import androidx.lifecycle.MutableLiveData

class VolumeDownLiveData : MutableLiveData<Long>() {

    companion object{

        private var instance : VolumeDownLiveData?=null

        fun getInstance(): VolumeDownLiveData?{
            if( instance == null ){
                instance = VolumeDownLiveData()
            }
            return instance
        }

    }

}