package com.github.waterpeak.jsonfit.example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.waterpeak.jsonfit.NetWorker
import com.github.waterpeak.jsonfit.builder

class MainActivity : AppCompatActivity() {

    private val mNetWorker by lazy { NetWorker() }
    private val mLoading by lazy{LoadPromptImpl(this)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoading.showLoading(true)
        API.login("hello","123456").builder(mNetWorker).loadingPromptControl(mLoading).enQueue {
            if(it.success){
                //dosomething
            }else{
                Toast.makeText(this,it.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
