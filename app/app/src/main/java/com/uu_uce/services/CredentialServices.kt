package com.uu_uce.services


enum class LoginResult{
    SUCCESS,
    NO_CREDENTIALS,
    NO_USERNAME,
    NO_PASSWORD;
}

fun login(username : String, password : String) : LoginResult{
    if(username.count() < 1 && password.count() < 1) return LoginResult.NO_CREDENTIALS
    else if(username.count() < 1) return LoginResult.NO_USERNAME
    else if(password.count() < 1) return LoginResult.NO_PASSWORD
    return LoginResult.SUCCESS // TODO: actually attempt to log in here
}