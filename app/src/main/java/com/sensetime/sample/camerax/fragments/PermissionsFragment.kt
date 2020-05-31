package com.sensetime.sample.camerax.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.sensetime.sample.camerax.R
import kotlinx.android.synthetic.main.activity_main.view.*

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO )

class PermissionsFragment:Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if( !hasPermission( requireContext() )){
            requestPermissions( PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE )
        }else{
            Navigation.findNavController( requireActivity(),R.id.fragment_container)
                    .navigate(PermissionsFragmentDirections.actionPermissionsToCamera() )

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if( requestCode == PERMISSIONS_REQUEST_CODE ){
            var isGrant = grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }
            if( isGrant ){
                Toast.makeText( context, "Permission request granted ",Toast.LENGTH_SHORT).show()
                Navigation.findNavController( requireActivity(), R.id.fragment_container )
                        .navigate( PermissionsFragmentDirections.actionPermissionsToCamera() )
            }else{
                Toast.makeText( context, "Permission request denied ",Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object{
        fun hasPermission( conext:Context ) = PERMISSIONS_REQUIRED.all{
            ContextCompat.checkSelfPermission( conext , it ) == PackageManager.PERMISSION_GRANTED
        }
    }
}