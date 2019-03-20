package com.github.waterpeak.jsonfit.example

import android.app.ProgressDialog
import android.content.Context
import com.github.waterpeak.jsonfit.ILoadingPrompt

class LoadPromptImpl(context: Context): ILoadingPrompt{

    private val dialog by lazy{
        val dialog = ProgressDialog(context)
        dialog.setTitle("Loading")
        dialog.setMessage("please wait")
        dialog.isIndeterminate = true
        dialog.setCancelable(true)
        dialog
    }

    override fun showLoading(show: Boolean) {
        if(show){
            if(!dialog.isShowing){
                dialog.show()
            }
        }else{
            if(dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }

}