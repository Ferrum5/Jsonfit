# Jsonfit
类似Retrofit，使用proxy代理接口发起网络请求    
由于项目接口都是提交Json数据，少量multipart，觉着给retrofit每个接口加json的header，再拼json字符串的requestbody，太麻烦了    
添加拦截器又要每个请求重新转换请求body，干脆自己写一个    
使用方式和Retrofit差不多，相比更轻量(简陋)    
Response解析主要面对
```json
{
    "code":100,
    "message":"success",
    "content":{
        "name":"张三",
        "age":30,
        "gender":"M"
    }
}
```
这样格式的json    
使用：
```kotlin
commitButton.setOnClickListener{
    loadImpl.showLoading(true)
    commitButton.isClickable = false
    API.login(phone,pwd).builder(mNetWorker).loadingPromptControl(loadImpl).bindView(commitButton).enQueue {
        if(it.success){
            startActivity(Intent(this,IndexActivity::class.java))
        }else{
            alert(it.message)
        }
    }
}

```
传入networker实现，okhttp的callback中会持有弱引用，可以直接在context中联网无需担心泄漏    
最好还是在viewmodel中配合livedata。直接在activity中处理主要是我自己一个人搞整个项目实在没精力搞viewmodel那一套了
