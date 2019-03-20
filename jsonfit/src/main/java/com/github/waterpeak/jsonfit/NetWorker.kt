package com.github.waterpeak.jsonfit


import android.view.View
import okhttp3.Callback as OkCallback
import java.lang.ref.WeakReference
import java.util.*

interface INetWorker {

    fun addCallbackReference(callback: INetBuilder<*>)

    fun removeCallbackReference(callback: INetBuilder<*>)
}

class NetWorker : INetWorker {

    private val refKeeper = HashSet<Any>()

    override fun addCallbackReference(callback: INetBuilder<*>) {
        refKeeper.add(callback)
    }

    override fun removeCallbackReference(callback: INetBuilder<*>) {
        refKeeper.remove(callback)
    }
}

interface INetBuilder<T : JResponse> {
    fun loadingPromptControl(loadingPrompt: ILoadingPrompt?): INetBuilder<T>
    fun worker(worker: INetWorker): INetBuilder<T>
    fun enQueue(callback: (T) -> Unit)
    fun bindView(view: View): INetBuilder<T>
}

fun <T : JResponse> JCall<T>.builder() = builder(null)
fun <T : JResponse> JCall<T>.builder(worker: INetWorker?): INetBuilder<T> =
    NetBuilder(this).apply { if (worker != null) worker(worker) }

class NetBuilder<T : JResponse>(
    private val call: JCall<T>
) : INetBuilder<T>, IResponseListener<T> {


    private var loadingPrompt: ILoadingPrompt? = null
    private var mWorker: INetWorker? = null
    private var clickableView: WeakReference<View>? = null

    private lateinit var callback: (T) -> Unit

    override fun loadingPromptControl(loadingPrompt: ILoadingPrompt?): INetBuilder<T> {
        this.loadingPrompt = loadingPrompt
        return this
    }

    override fun worker(worker: INetWorker): INetBuilder<T> {
        this.mWorker = worker
        return this
    }

    override fun bindView(view: View): INetBuilder<T> {
        this.clickableView = WeakReference(view)
        return this
    }

    override fun enQueue(callback: (T) -> Unit) {
        this.callback = callback
        mWorker?.addCallbackReference(this)
        call.enqueue(JCallback(call, if (mWorker != null) ResponseWeakWrapperListener(this) else this))
    }

    override fun onResponse(response: T) {
        mWorker?.removeCallbackReference(this)
        loadingPrompt?.showLoading(false)
        clickableView?.get()?.isClickable = true
        callback(response)
    }
}

