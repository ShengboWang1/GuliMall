package com.shengbo.gulimall.member.exception;

public class UserNameExistException extends RuntimeException{
    public UserNameExistException() {
        super("手机号存在！");
    }
}
